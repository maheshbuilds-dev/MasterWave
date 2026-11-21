package com.masterwave.player.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

/**
 * Handles package installer session callbacks for clean, trusted in-app updates.
 * When the package installer requires user confirmation, it launches the official
 * system "Update MasterWave? [Cancel] [Update]" prompt directly.
 */
class AppUpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        Log.d(TAG, "Install status received: $status")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "MasterWave update installed successfully! Auto-launching latest version...")
                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("post_update_relaunch", true)
                    }
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not auto-launch after update", e)
                }
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w(TAG, "Install callback: status=$status, message=$message")
            }
        }
    }

    companion object {
        private const val TAG = "AppUpdateInstallReceiver"
        const val ACTION_INSTALL_STATUS = "com.masterwave.player.ACTION_INSTALL_STATUS"
    }
}
