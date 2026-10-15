package com.example.floatingeye.audio

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class MicOutputRoute(val label: String) {
    AUTO("Auto"),
    BLUETOOTH("Bluetooth"),
    AUX("Wired AUX")
}

/**
 * Controller managing live Microphone to Bluetooth speaker / audio pass-through (Megaphone).
 * Streams low-latency PCM audio from device mic to connected Bluetooth speaker or headphones.
 * Real-time RMS volume level metering drives live cybernetic visualizer effects.
 */
class BluetoothMicController private constructor(private val appContext: Context) {

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private var streamingJob: Job? = null
    private val routePrefs = appContext.getSharedPreferences("mic_audio_route", Context.MODE_PRIVATE)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    // Reactive StateFlows
    private val _isMicPlaying = MutableStateFlow(false)
    val isMicPlaying: StateFlow<Boolean> = _isMicPlaying.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()
    private val _isAuxConnected = MutableStateFlow(false)
    val isAuxConnected: StateFlow<Boolean> = _isAuxConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String>("Phone Speaker")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    private val _inputDeviceName = MutableStateFlow("Internal microphone")
    val inputDeviceName: StateFlow<String> = _inputDeviceName.asStateFlow()

    private val _availableOutputRoutes = MutableStateFlow(MicOutputRoute.entries)
    val availableOutputRoutes: StateFlow<List<MicOutputRoute>> = _availableOutputRoutes.asStateFlow()

    private val _selectedOutputRoute = MutableStateFlow(
        runCatching { MicOutputRoute.valueOf(routePrefs.getString(KEY_OUTPUT_ROUTE, null) ?: "AUTO") }
            .getOrDefault(MicOutputRoute.AUTO)
    )
    val selectedOutputRoute: StateFlow<MicOutputRoute> = _selectedOutputRoute.asStateFlow()

    private val _micGain = MutableStateFlow(1.0f)
    val micGain: StateFlow<Float> = _micGain.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?> (null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var legacyScoRoutingActive = false
    private var communicationDeviceSelected = false

    init {
        checkBluetoothStatus()
        registerDeviceCallbacks()
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBluetoothConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    fun checkBluetoothStatus() {
        val devices = try {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        } catch (_: SecurityException) {
            _isBluetoothConnected.value = false
            _isAuxConnected.value = false
            _connectedDeviceName.value = "Allow Bluetooth access to list devices"
            _inputDeviceName.value = "Internal microphone"
            return
        }
        val bluetooth = devices.firstOrNull { it.isBluetoothAudioOutput() }
        val wired = devices.firstOrNull { it.isWiredAudioOutput() }
        _availableOutputRoutes.value = MicOutputRoute.entries
        _isBluetoothConnected.value = bluetooth != null
        _isAuxConnected.value = wired != null
        val activeOutput = when (_selectedOutputRoute.value) {
            MicOutputRoute.BLUETOOTH -> bluetooth
            MicOutputRoute.AUX -> wired
            MicOutputRoute.AUTO -> null
        }
        _connectedDeviceName.value = activeOutput?.displayName() ?: when (_selectedOutputRoute.value) {
            MicOutputRoute.AUTO -> "System default"
            MicOutputRoute.BLUETOOTH -> "Connect Bluetooth device"
            MicOutputRoute.AUX -> "Connect wired AUX headset"
        }

        val inputDevices = try {
            audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        } catch (_: SecurityException) {
            emptyArray()
        }
        val preferredInput = when (_selectedOutputRoute.value) {
            MicOutputRoute.BLUETOOTH -> inputDevices.firstOrNull { it.isBluetoothAudioInput() }
            MicOutputRoute.AUX -> inputDevices.firstOrNull { it.isWiredAudioInput() }
            MicOutputRoute.AUTO -> null
        }
        _inputDeviceName.value = preferredInput?.displayName() ?: "Internal microphone"
    }

    @Synchronized
    fun setOutputRoute(route: MicOutputRoute) {
        if (_selectedOutputRoute.value == route) return
        val restart = _isMicPlaying.value
        if (restart) stopStreaming()
        _selectedOutputRoute.value = route
        routePrefs.edit().putString(KEY_OUTPUT_ROUTE, route.name).apply()
        checkBluetoothStatus()
        val isConnected = when (route) {
            MicOutputRoute.AUTO -> true
            MicOutputRoute.BLUETOOTH -> _isBluetoothConnected.value
            MicOutputRoute.AUX -> _isAuxConnected.value
        }
        _errorMessage.value = if (isConnected) null else when (route) {
            MicOutputRoute.BLUETOOTH -> "Connect a Bluetooth audio device first"
            MicOutputRoute.AUX -> "Connect wired headphones or an AUX headset first"
            MicOutputRoute.AUTO -> null
        }
        if (restart && isConnected) startStreaming()
    }

    private fun registerDeviceCallbacks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    checkBluetoothStatus()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    checkBluetoothStatus()
                }
            }
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        }

        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                checkBluetoothStatus()
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }
        try {
            appContext.registerReceiver(bluetoothReceiver, filter)
        } catch (_: Exception) {}
    }

    fun setGain(gain: Float) {
        _micGain.value = gain.coerceIn(MIN_MIC_GAIN, MAX_MIC_GAIN)
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleStreaming(): Boolean {
        return if (_isMicPlaying.value) {
            stopStreaming()
            false
        } else {
            startStreaming()
        }
    }

    @Synchronized
    fun startStreaming(): Boolean {
        if (_isMicPlaying.value) return true
        if (!hasRecordPermission()) {
            _errorMessage.value = "Microphone permission is required to enable live streaming"
            return false
        }
        if (_selectedOutputRoute.value == MicOutputRoute.BLUETOOTH && !hasBluetoothConnectPermission()) {
            _errorMessage.value = "Allow Nearby devices permission to use Bluetooth audio"
            return false
        }

        _errorMessage.value = null
        checkBluetoothStatus()

        if (_selectedOutputRoute.value != MicOutputRoute.AUTO && preferredOutputForSelection() == null) {
            _errorMessage.value = when (_selectedOutputRoute.value) {
                MicOutputRoute.BLUETOOTH -> "Connect a Bluetooth audio device, then start the mic again"
                MicOutputRoute.AUX -> "Connect wired headphones or an AUX headset, then start the mic again"
                MicOutputRoute.AUTO -> null
            }
            return false
        }

        // Only enable Bluetooth communication routing when the user selected it.
        enableBluetoothAudioRouting(_selectedOutputRoute.value)

        val sampleRate = 44100
        val channelIn = AudioFormat.CHANNEL_IN_MONO
        val channelOut = AudioFormat.CHANNEL_OUT_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT

        val minRecBuffer = AudioRecord.getMinBufferSize(sampleRate, channelIn, format)
        val minTrackBuffer = AudioTrack.getMinBufferSize(sampleRate, channelOut, format)

        if (minRecBuffer <= 0 || minTrackBuffer <= 0) {
            disableBluetoothAudioRouting()
            _errorMessage.value = "Audio hardware configuration not supported"
            return false
        }

        val recBufferSize = (minRecBuffer * 2).coerceAtLeast(2048)
        val trackBufferSize = (minTrackBuffer * 2).coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelIn,
                format,
                recBufferSize
            )
            preferredInputForSelection()?.let { input ->
                audioRecord?.setPreferredDevice(input)
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(format)
                .setChannelMask(channelOut)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(trackBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val preferredOutput = preferredOutputForSelection()
            if (_selectedOutputRoute.value != MicOutputRoute.AUTO && preferredOutput == null) {
                cleanupAudio()
                disableBluetoothAudioRouting()
                _errorMessage.value = "The selected audio output is no longer connected"
                return false
            }
            if (preferredOutput != null && audioTrack?.setPreferredDevice(preferredOutput) != true) {
                cleanupAudio()
                disableBluetoothAudioRouting()
                _errorMessage.value = "Android could not route audio to ${preferredOutput.displayName()}"
                return false
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED ||
                audioTrack?.state != AudioTrack.STATE_INITIALIZED
            ) {
                cleanupAudio()
                disableBluetoothAudioRouting()
                _errorMessage.value = "Failed to initialize microphone or speaker"
                return false
            }

            audioRecord?.startRecording()
            audioTrack?.play()
            audioRecord?.routedDevice?.let { _inputDeviceName.value = it.displayName() }
            (audioTrack?.routedDevice ?: preferredOutput)?.let { _connectedDeviceName.value = it.displayName() }
            _isMicPlaying.value = true

            // Real-time low latency stream loop
            streamingJob = scope.launch {
                val chunkSize = 512 // ~11ms chunks at 44.1kHz for super fast live audio
                val buffer = ShortArray(chunkSize)

                while (isActive && _isMicPlaying.value) {
                    val read = audioRecord?.read(buffer, 0, chunkSize) ?: -1
                    if (read > 0) {
                        var sumSq = 0.0
                        val currentGain = _micGain.value
                        val muted = _isMuted.value

                        for (i in 0 until read) {
                            if (muted) {
                                buffer[i] = 0
                            } else {
                                buffer[i] = limitSample(buffer[i], currentGain)
                            }
                            val outputSample = buffer[i].toDouble()
                            sumSq += outputSample * outputSample
                        }

                        // RMS Amplitude calculation for UI visualizer
                        val rms = sqrt(sumSq / read) / 32768.0
                        _audioLevel.value = (rms.toFloat() * 2.8f).coerceIn(0f, 1f)

                        // Write to output speaker
                        audioTrack?.write(buffer, 0, read)
                    }
                }
            }
            return true
        } catch (e: SecurityException) {
            cleanupAudio()
            disableBluetoothAudioRouting()
            _errorMessage.value = if (_selectedOutputRoute.value == MicOutputRoute.BLUETOOTH) {
                "Allow Bluetooth and microphone permissions to use this route"
            } else "Microphone permission not granted"
            return false
        } catch (e: Exception) {
            cleanupAudio()
            disableBluetoothAudioRouting()
            _errorMessage.value = "Audio connection error: ${e.message}"
            return false
        }
    }

    /** Compresses peaks smoothly so high gain does not hard-clip the PCM stream. */
    private fun limitSample(input: Short, gain: Float): Short {
        val driven = input.toDouble() * gain / Short.MAX_VALUE
        val magnitude = abs(driven)
        val threshold = LIMITER_THRESHOLD
        val limitedMagnitude = if (magnitude <= threshold) {
            magnitude
        } else {
            val excess = magnitude - threshold
            threshold + (1.0 - threshold) * (excess / (excess + (1.0 - threshold)))
        }
        val limited = if (driven < 0.0) -limitedMagnitude else limitedMagnitude
        return (limited * Short.MAX_VALUE).roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }

    @Synchronized
    fun stopStreaming() {
        _isMicPlaying.value = false
        _audioLevel.value = 0f
        streamingJob?.cancel()
        streamingJob = null

        cleanupAudio()
        disableBluetoothAudioRouting()
    }

    private fun cleanupAudio() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
        } catch (_: Exception) {}
        try {
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun preferredInputForSelection(): AudioDeviceInfo? {
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return when (_selectedOutputRoute.value) {
            MicOutputRoute.BLUETOOTH -> inputs.firstOrNull { it.isBluetoothAudioInput() }
            MicOutputRoute.AUX -> inputs.firstOrNull { it.isWiredAudioInput() }
            MicOutputRoute.AUTO -> null
        }
    }

    private fun preferredOutputForSelection(): AudioDeviceInfo? {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return when (_selectedOutputRoute.value) {
            MicOutputRoute.BLUETOOTH -> outputs.firstOrNull { it.isBluetoothAudioOutput() }
            MicOutputRoute.AUX -> outputs.firstOrNull { it.isWiredAudioOutput() }
            MicOutputRoute.AUTO -> null
        }
    }

    private fun enableBluetoothAudioRouting(route: MicOutputRoute) {
        if (route != MicOutputRoute.BLUETOOTH) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val commDevices = audioManager.availableCommunicationDevices
                val btDevice = commDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }
                if (btDevice != null) {
                    communicationDeviceSelected = audioManager.setCommunicationDevice(btDevice)
                }
            } else {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                legacyScoRoutingActive = true
            }
        } catch (_: Exception) {}
    }

    private fun disableBluetoothAudioRouting() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && communicationDeviceSelected) {
                audioManager.clearCommunicationDevice()
                communicationDeviceSelected = false
            } else if (legacyScoRoutingActive) {
                audioManager.isBluetoothScoOn = false
                audioManager.stopBluetoothSco()
                legacyScoRoutingActive = false
            }
        } catch (_: Exception) {}
    }

    fun release() {
        stopStreaming()
        audioDeviceCallback?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.unregisterAudioDeviceCallback(it)
            }
        }
        bluetoothReceiver?.let {
            try {
                appContext.unregisterReceiver(it)
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val KEY_OUTPUT_ROUTE = "selected_output_route"
        private const val MIN_MIC_GAIN = 0.5f
        private const val MAX_MIC_GAIN = 2.0f
        private const val LIMITER_THRESHOLD = 0.82
        @Volatile
        private var instance: BluetoothMicController? = null

        fun getInstance(context: Context): BluetoothMicController {
            return instance ?: synchronized(this) {
                instance ?: BluetoothMicController(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private fun AudioDeviceInfo.displayName(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { productName?.toString()?.takeIf { it.isNotBlank() } }.getOrNull() ?: "Audio device"
        } else "Audio device"

    private fun AudioDeviceInfo.isBluetoothAudioOutput(): Boolean = type in setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_HEADSET
    )

    private fun AudioDeviceInfo.isWiredAudioOutput(): Boolean = type in setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET
    )

    private fun AudioDeviceInfo.isBluetoothAudioInput(): Boolean = type in setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET
    )

    private fun AudioDeviceInfo.isWiredAudioInput(): Boolean = type in setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET
    )
}
