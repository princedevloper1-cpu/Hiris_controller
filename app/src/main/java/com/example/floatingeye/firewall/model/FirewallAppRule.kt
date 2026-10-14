package com.example.floatingeye.firewall.model

/**
 * Network blocking rules for a specific application.
 * Allows blocking Mobile Data, Wi-Fi, or both.
 */
data class FirewallAppRule(
    val packageName: String,
    val appName: String,
    val blockMobileData: Boolean = true,
    val blockWifi: Boolean = true
) {
    val isAnyBlocked: Boolean
        get() = blockMobileData || blockWifi

    val isAllBlocked: Boolean
        get() = blockMobileData && blockWifi
}

enum class ActiveNetworkType {
    WIFI,
    MOBILE,
    NONE
}
