package com.example.floatingeye.firewall.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.example.floatingeye.firewall.model.ActiveNetworkType
import com.example.floatingeye.firewall.model.FirewallAppRule
import com.example.floatingeye.firewall.service.FirewallVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class FirewallRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _rules = MutableStateFlow<Map<String, FirewallAppRule>>(loadRules())
    val rules: StateFlow<Map<String, FirewallAppRule>> = _rules.asStateFlow()

    private val _isFirewallActive = MutableStateFlow(prefs.getBoolean(KEY_ACTIVE, false))
    val isFirewallActive: StateFlow<Boolean> = _isFirewallActive.asStateFlow()

    private val _activeNetworkType = MutableStateFlow(determineCurrentNetworkType())
    val activeNetworkType: StateFlow<ActiveNetworkType> = _activeNetworkType.asStateFlow()

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        updateNetworkState()
                    }

                    override fun onLost(network: Network) {
                        updateNetworkState()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        updateNetworkState()
                    }
                }
            )
        } catch (_: Exception) {}
    }

    private fun updateNetworkState() {
        _activeNetworkType.value = determineCurrentNetworkType()
        if (_isFirewallActive.value) {
            notifyServiceRulesChanged()
        }
    }

    private fun determineCurrentNetworkType(): ActiveNetworkType {
        val cm = connectivityManager ?: return ActiveNetworkType.NONE
        return try {
            val activeNetwork = cm.activeNetwork ?: return ActiveNetworkType.NONE
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return ActiveNetworkType.NONE
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ActiveNetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ActiveNetworkType.MOBILE
                else -> ActiveNetworkType.NONE
            }
        } catch (_: Exception) {
            ActiveNetworkType.NONE
        }
    }

    private fun loadRules(): Map<String, FirewallAppRule> {
        val json = prefs.getString(KEY_RULES, null) ?: return emptyMap()
        val map = mutableMapOf<String, FirewallAppRule>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pkg = obj.getString("package")
                val name = obj.optString("name", pkg)
                val mobile = obj.optBoolean("mobile", true)
                val wifi = obj.optBoolean("wifi", true)
                map[pkg] = FirewallAppRule(
                    packageName = pkg,
                    appName = name,
                    blockMobileData = mobile,
                    blockWifi = wifi
                )
            }
        } catch (_: Exception) {}
        return map
    }

    private fun persistRules(rulesMap: Map<String, FirewallAppRule>) {
        try {
            val array = JSONArray()
            rulesMap.values.forEach { rule ->
                val obj = JSONObject().apply {
                    put("package", rule.packageName)
                    put("name", rule.appName)
                    put("mobile", rule.blockMobileData)
                    put("wifi", rule.blockWifi)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_RULES, array.toString()).apply()
            _rules.value = rulesMap
        } catch (_: Exception) {}
    }

    fun setFirewallActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_ACTIVE, active).apply()
        _isFirewallActive.value = active

        if (active) {
            FirewallVpnService.startService(appContext)
        } else {
            FirewallVpnService.stopService(appContext)
        }
    }

    fun setAppRule(
        packageName: String,
        appName: String,
        blockMobile: Boolean,
        blockWifi: Boolean
    ) {
        val current = _rules.value.toMutableMap()
        if (!blockMobile && !blockWifi) {
            current.remove(packageName)
        } else {
            current[packageName] = FirewallAppRule(
                packageName = packageName,
                appName = appName,
                blockMobileData = blockMobile,
                blockWifi = blockWifi
            )
        }
        persistRules(current)
        notifyServiceRulesChanged()
    }

    fun toggleBlockMobile(packageName: String, appName: String) {
        val currentRule = _rules.value[packageName]
        val newMobile = !(currentRule?.blockMobileData ?: false)
        val currentWifi = currentRule?.blockWifi ?: false
        setAppRule(packageName, appName, newMobile, currentWifi)
    }

    fun toggleBlockWifi(packageName: String, appName: String) {
        val currentRule = _rules.value[packageName]
        val currentMobile = currentRule?.blockMobileData ?: false
        val newWifi = !(currentRule?.blockWifi ?: false)
        setAppRule(packageName, appName, currentMobile, newWifi)
    }

    fun toggleBlockAll(packageName: String, appName: String) {
        val currentRule = _rules.value[packageName]
        val shouldBlock = !(currentRule?.isAllBlocked ?: false)
        setAppRule(packageName, appName, shouldBlock, shouldBlock)
    }

    fun removeRule(packageName: String) {
        val current = _rules.value.toMutableMap()
        if (current.remove(packageName) != null) {
            persistRules(current)
            notifyServiceRulesChanged()
        }
    }

    fun isAppBlockedOnCurrentNetwork(packageName: String): Boolean {
        if (!_isFirewallActive.value) return false
        val rule = _rules.value[packageName] ?: return false
        return when (_activeNetworkType.value) {
            ActiveNetworkType.MOBILE -> rule.blockMobileData
            ActiveNetworkType.WIFI -> rule.blockWifi
            ActiveNetworkType.NONE -> rule.isAnyBlocked
        }
    }

    fun getCurrentlyBlockedPackages(): Set<String> {
        val active = _isFirewallActive.value
        if (!active) return emptySet()

        val netType = _activeNetworkType.value
        return _rules.value.filter { (_, rule) ->
            when (netType) {
                ActiveNetworkType.MOBILE -> rule.blockMobileData
                ActiveNetworkType.WIFI -> rule.blockWifi
                ActiveNetworkType.NONE -> rule.isAnyBlocked
            }
        }.keys
    }

    private fun notifyServiceRulesChanged() {
        if (_isFirewallActive.value) {
            val intent = Intent(appContext, FirewallVpnService::class.java).apply {
                action = FirewallVpnService.ACTION_RELOAD_RULES
            }
            try {
                appContext.startService(intent)
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val PREFS_NAME = "hiris_firewall_prefs"
        private const val KEY_ACTIVE = "firewall_is_active"
        private const val KEY_RULES = "firewall_rules_json"

        @Volatile
        private var instance: FirewallRepository? = null

        fun getInstance(context: Context): FirewallRepository {
            return instance ?: synchronized(this) {
                instance ?: FirewallRepository(context).also { instance = it }
            }
        }

        // Popular predefined apps for instant zero-effort blocking
        val POPULAR_APPS = listOf(
            FirewallAppRule("com.whatsapp", "WhatsApp", true, true),
            FirewallAppRule("com.facebook.katana", "Facebook", true, true),
            FirewallAppRule("com.instagram.android", "Instagram", true, true),
            FirewallAppRule("com.facebook.orca", "Messenger", true, true),
            FirewallAppRule("com.zhiliaoapp.musically", "TikTok", true, true),
            FirewallAppRule("org.telegram.messenger", "Telegram", true, true),
            FirewallAppRule("com.twitter.android", "X (Twitter)", true, true),
            FirewallAppRule("com.google.android.youtube", "YouTube", true, true)
        )
    }
}
