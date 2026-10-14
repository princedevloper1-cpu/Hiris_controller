package com.example.floatingeye.shortcuts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

/**
 * Handles launching real installed applications by package name.
 * Robust against uninstalled apps; never crashes and provides graceful feedback.
 */
class AppLaunchManager(
    private val context: Context,
    private val shortcutRepository: AppShortcutRepository? = null
) {
    fun launchApp(packageName: String, displayName: String = ""): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        if (launchIntent == null) {
            handleMissingApp(packageName, displayName)
            return false
        }

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(launchIntent)
            true
        } catch (e: ActivityNotFoundException) {
            handleMissingApp(packageName, displayName)
            false
        } catch (e: SecurityException) {
            Toast.makeText(context, "Cannot open $displayName: Security restriction", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            handleMissingApp(packageName, displayName)
            false
        }
    }

    /** Opens a shortcut beside the current task when Android and the target app allow it. */
    fun launchAppAdjacent(packageName: String, displayName: String = ""): Boolean {
        val label = displayName.ifEmpty { packageName }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            handleMissingApp(packageName, displayName)
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(context, "Split screen is unavailable for $label. Opening normally.", Toast.LENGTH_LONG).show()
            return launchNormally(launchIntent, packageName, displayName)
        }

        return try {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            context.startActivity(launchIntent)
            true
        } catch (_: ActivityNotFoundException) {
            handleMissingApp(packageName, displayName)
            false
        } catch (_: Exception) {
            Toast.makeText(context, "Split screen could not start. Opening $label normally.", Toast.LENGTH_LONG).show()
            launchNormally(launchIntent, packageName, displayName)
        }
    }

    private fun launchNormally(intent: Intent, packageName: String, displayName: String): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        handleMissingApp(packageName, displayName)
        false
    } catch (_: SecurityException) {
        Toast.makeText(context, "Cannot open ${displayName.ifEmpty { packageName }}: Security restriction", Toast.LENGTH_SHORT).show()
        false
    } catch (_: Exception) {
        handleMissingApp(packageName, displayName)
        false
    }

    private fun handleMissingApp(packageName: String, displayName: String) {
        val label = displayName.ifEmpty { packageName }
        Toast.makeText(
            context,
            "App \"$label\" is no longer installed on this device",
            Toast.LENGTH_LONG
        ).show()

        // Clean up or remove from shortcuts
        shortcutRepository?.removeShortcut(packageName)
    }
}
