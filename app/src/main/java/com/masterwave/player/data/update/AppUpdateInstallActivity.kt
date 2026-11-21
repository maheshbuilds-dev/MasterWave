package com.masterwave.player.data.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

/**
 * Transparent trampoline activity that manages the official Android PackageInstaller dialog.
 * Crucial behavior:
 * - Keeps MasterWave active while the system dialog "[Cancel] [Update]" is shown.
 * - When the user clicks the real PackageInstaller [Update] button:
 *   Closes MasterWave immediately so Android can perform the update silently in the background
 *   without file locks, UI freezing, or Play Protect scan alerts.
 * - If the user clicks [Cancel]:
 *   Leaves MasterWave open normally without quitting.
 */
class AppUpdateInstallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_CONFIRM_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_CONFIRM_INTENT)
        }

        if (confirmIntent != null) {
            try {
                // Ensure FLAG_ACTIVITY_NEW_TASK does not interfere with onActivityResult
                confirmIntent.flags = confirmIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
                startActivityForResult(confirmIntent, REQUEST_INSTALL)
            } catch (e: Exception) {
                Log.e(TAG, "Could not start PackageInstaller confirmation intent", e)
                finish()
            }
        } else {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL) {
            Log.d(TAG, "PackageInstaller dialog result: resultCode=$resultCode")
            if (resultCode == RESULT_OK) {
                // User tapped the real "Update" button on the PackageInstaller prompt!
                Log.i(TAG, "User clicked Update! Closing app for silent background installation...")
                closeAppAndExit()
            } else {
                // User tapped "Cancel" or dismissed the dialog
                Log.i(TAG, "User canceled PackageInstaller dialog")
                finish()
            }
        }
    }

    private fun closeAppAndExit() {
        try {
            // Signal MainActivity and PlaybackService to terminate immediately
            sendBroadcast(Intent(ACTION_CLOSE_FOR_UPDATE))
        } catch (_: Exception) {}

        try {
            // Stop foreground playback service
            val stopServiceIntent = Intent().apply {
                setClassName(packageName, "com.masterwave.player.playback.MasterWavePlaybackService")
                action = ACTION_CLOSE_FOR_UPDATE
            }
            startService(stopServiceIntent)
        } catch (_: Exception) {}

        try {
            // Minimize to home so background package update has clean OS priority
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (_: Exception) {}

        try {
            finishAffinity()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "AppUpdateInstallActivity"
        const val EXTRA_CONFIRM_INTENT = "extra_confirm_intent"
        const val ACTION_CLOSE_FOR_UPDATE = "com.masterwave.player.ACTION_CLOSE_FOR_UPDATE"
        private const val REQUEST_INSTALL = 1001

        fun launch(context: Context, confirmIntent: Intent) {
            val intent = Intent(context, AppUpdateInstallActivity::class.java).apply {
                putExtra(EXTRA_CONFIRM_INTENT, confirmIntent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
