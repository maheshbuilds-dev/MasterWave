package com.masterwave.player.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.io.File

/**
 * Triggered automatically by Android OS when MasterWave finishes updating:
 * 1. Cleans up and deletes any downloaded update APKs from internal cache/storage.
 * 2. Cancels lingering update notifications.
 * 3. Automatically relaunches MasterWave to the latest version smoothly.
 */
class AppPackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "MasterWave update completed! Cleaning old APKs and relaunching latest build...")

            // 1. Delete all old APK files from cache to free user storage
            try {
                val updateDir = File(context.cacheDir, "updates")
                if (updateDir.exists()) {
                    updateDir.deleteRecursively()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete old APK cache", e)
            }

            // 2. Clear any active update notification
            try {
                NotificationManagerCompat.from(context).cancel(20_001)
            } catch (e: Exception) {
                Log.w(TAG, "Could not dismiss update notification", e)
            }

            // 3. Automatically relaunch the app into the new version
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("post_update_relaunch", true)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not auto-relaunch MasterWave after update", e)
            }
        }
    }

    companion object {
        private const val TAG = "AppPackageReplaced"
    }
}
