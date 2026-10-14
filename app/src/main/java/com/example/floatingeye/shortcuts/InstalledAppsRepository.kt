package com.example.floatingeye.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovers and queries applications installed on the user's Android device.
 * Never hardcodes apps; uses real Android PackageManager APIs.
 */
class InstalledAppsRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        val appList = mutableListOf<InstalledAppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            // Skip this application itself and duplicates
            if (pkg == context.packageName || seenPackages.contains(pkg)) {
                continue
            }
            seenPackages.add(pkg)

            val name = resolveInfo.loadLabel(packageManager)?.toString() ?: pkg
            val icon = try {
                resolveInfo.loadIcon(packageManager)
            } catch (_: Exception) {
                packageManager.defaultActivityIcon
            }

            appList.add(
                InstalledAppInfo(
                    packageName = pkg,
                    displayName = name,
                    icon = icon
                )
            )
        }

        // Sort alphabetically by app name
        appList.sortedBy { it.displayName.lowercase() }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            try {
                packageManager.defaultActivityIcon
            } catch (_: Exception) {
                null
            }
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
