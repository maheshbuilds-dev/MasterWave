package com.masterwave.player.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

/**
 * Receives the result of the PackageInstaller session used for in-app self-updates.
 *
 * MasterWave uses `SessionParams.USER_ACTION_NOT_REQUIRED` together with the
 * `UPDATE_PACKAGES_WITHOUT_USER_ACTION` permission, so on Android 12+ the update is applied
 * silently and this receiver normally only ever sees SUCCESS / FAILURE. The
 * STATUS_PENDING_USER_ACTION branch is kept as a safe fallback for devices or modes where the
 * system still wants confirmation, so the update can never get stuck.
 */
class AppUpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.d(TAG, "Install status received: $status (${message ?: "no message"})")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Fallback only: the silent path was not honoured, so show the system prompt.
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not show system install confirmation", e)
                        AppUpdateManager.peekInstance()?.onInstallFailed("Could not open the system installer.")
                    }
                } else {
                    AppUpdateManager.peekInstance()?.onInstallFailed("The system installer did not respond.")
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                // The process is normally killed during a self-update; MY_PACKAGE_REPLACED
                // handles relaunch. Nothing else is required here.
                Log.i(TAG, "MasterWave update installed successfully")
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Log.i(TAG, "Update cancelled")
                AppUpdateManager.peekInstance()?.onInstallAborted()
            }

            else -> {
                Log.w(TAG, "Update failed: status=$status message=$message")
                AppUpdateManager.peekInstance()?.onInstallFailed(
                    message?.takeIf { it.isNotBlank() } ?: "The update could not be installed."
                )
            }
        }
    }

    companion object {
        private const val TAG = "AppUpdateInstallReceiver"
        const val ACTION_INSTALL_STATUS = "com.masterwave.player.ACTION_INSTALL_STATUS"
    }
}
