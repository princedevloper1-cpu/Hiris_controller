package com.example.floatingeye.capture

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Crops MediaProjection frames with GLES before they reach MediaRecorder. */
internal class RegionCropRenderer(
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val cropLeft: Int,
    private val cropTop: Int,
    private val cropWidth: Int,
    private val cropHeight: Int,
    private val encoderSurface: Surface
) {
    private val thread = HandlerThread("HirisRegionCrop").apply { start() }
    private val handler = Handler(thread.looper)
    private val initialized = CountDownLatch(1)
    @Volatile private var initializationError: Throwable? = null
    @Volatile private var released = false
    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface = EGL14.EGL_NO_SURFACE
    private var surfaceTexture: SurfaceTexture? = null
    private var input: Surface? = null
    private var textureId = 0
    private var program = 0
    private var positionHandle = -1
    private var texCoordHandle = -1
    private var textureMatrixHandle = -1
    private var cropHandle = -1
    private val textureMatrix = FloatArray(16)
    private val positions = floatBuffer(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
    // SurfaceTexture supplies its own transform matrix. Keep input UVs in the
    // conventional GL order so the producer transform is applied exactly once;
    // the previous reversed V coordinates flipped recorded frames vertically.
    private val texCoords = floatBuffer(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))

    val inputSurface: Surface

    init {
        handler.post {
            try {
                initializeGl()
            } catch (error: Throwable) {
                initializationError = error
                releaseGl()
            } finally {
                initialized.countDown()
            }
        }
        check(initialized.await(5, TimeUnit.SECONDS)) { "Timed out creating crop renderer" }
        initializationError?.let { throw IllegalStateException("Could not initialize video crop", it) }
        inputSurface = checkNotNull(input)
    }

    private fun initializeGl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "No EGL display" }
        val versions = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, versions, 0, versions, 1)) { "Could not initialize EGL" }

        val configAttributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(eglDisplay, configAttributes, 0, configs, 0, 1, count, 0) && count[0] > 0) {
            "No recordable EGL config"
        }
        val config = checkNotNull(configs[0])
        eglContext = EGL14.eglCreateContext(
            eglDisplay, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
        )
        check(eglContext != EGL14.EGL_NO_CONTEXT) { "Could not create EGL context" }
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, encoderSurface, intArrayOf(EGL14.EGL_NONE), 0)
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "Could not connect EGL to video encoder" }
        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) { "Could not activate EGL surface" }

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureMatrixHandle = GLES20.glGetUniformLocation(program, "uTextureMatrix")
        cropHandle = GLES20.glGetUniformLocation(program, "uCrop")
        surfaceTexture = SurfaceTexture(textureId).apply {
            setDefaultBufferSize(sourceWidth, sourceHeight)
            setOnFrameAvailableListener({ texture -> drawFrame(texture) }, handler)
        }
        input = Surface(surfaceTexture)
    }

    private fun drawFrame(texture: SurfaceTexture) {
        if (released || eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return
        try {
            texture.updateTexImage()
            texture.getTransformMatrix(textureMatrix)
            GLES20.glViewport(0, 0, cropWidth, cropHeight)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)

            positions.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, positions)
            texCoords.position(0)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoords)
            GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, textureMatrix, 0)
            GLES20.glUniform4f(
                cropHandle,
                cropLeft.toFloat() / sourceWidth,
                cropTop.toFloat() / sourceHeight,
                cropWidth.toFloat() / sourceWidth,
                cropHeight.toFloat() / sourceHeight
            )
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, texture.timestamp)
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        } catch (_: RuntimeException) {
            // A late frame can arrive while the projection is being stopped.
        }
    }

    fun release() {
        if (released) return
        released = true
        val finished = CountDownLatch(1)
        handler.post {
            releaseGl()
            finished.countDown()
        }
        finished.await(2, TimeUnit.SECONDS)
        thread.quitSafely()
    }

    private fun releaseGl() {
        surfaceTexture?.setOnFrameAvailableListener(null)
        input?.release()
        input = null
        surfaceTexture?.release()
        surfaceTexture = null
        if (textureId != 0) GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        textureId = 0
        if (program != 0) GLES20.glDeleteProgram(program)
        program = 0
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglSurface = EGL14.EGL_NO_SURFACE
        eglContext = EGL14.EGL_NO_CONTEXT
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val result = GLES20.glCreateProgram()
        GLES20.glAttachShader(result, vertex)
        GLES20.glAttachShader(result, fragment)
        GLES20.glLinkProgram(result)
        val status = IntArray(1)
        GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, status, 0)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        check(status[0] == GLES20.GL_TRUE) { GLES20.glGetProgramInfoLog(result) }
        return result
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { GLES20.glGetShaderInfoLog(shader) }
        return shader
    }

    private fun floatBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values).position(0) }

    companion object {
        private const val EGL_RECORDABLE_ANDROID = 0x3142
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uTextureMatrix;
            uniform vec4 uCrop;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vec2 cropped = uCrop.xy + aTexCoord * uCrop.zw;
                vTexCoord = (uTextureMatrix * vec4(cropped, 0.0, 1.0)).xy;
            }
        """
        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES sTexture;
            varying vec2 vTexCoord;
            void main() { gl_FragColor = texture2D(sTexture, vTexCoord); }
        """
    }
}
