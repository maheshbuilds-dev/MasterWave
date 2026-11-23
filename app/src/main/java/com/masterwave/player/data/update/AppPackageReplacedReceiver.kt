package com.masterwave.player.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.masterwave.player.BuildConfig
import java.io.File

/**
 * Triggered automatically by Android OS when MasterWave finishes updating:
 * 1. Cleans up and deletes any downloaded update APKs from internal cache/storage.
 * 2. Cancels lingering update notifications.
 * 3. Records updated version code so no update notification is shown until next update.
 * 4. Automatically relaunches MasterWave to the latest version smoothly.
 */
class AppPackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "MasterWave update completed! Cleaning old APKs and relaunching latest build...")
            com.masterwave.player.data.icon.AppIconManager.ensureValidLauncherAlias(context)

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
                val nm = NotificationManagerCompat.from(context)
                nm.cancel(20_001)
                nm.cancelAll()
            } catch (e: Exception) {
                Log.w(TAG, "Could not dismiss update notification", e)
            }

            // 3. Suppress update notifications until a newer remote update exists
            try {
                val prefs = context.getSharedPreferences("masterwave_updates", Context.MODE_PRIVATE)
                val currentCode = BuildConfig.VERSION_CODE
                prefs.edit()
                    .putInt("last_notified_version", currentCode)
                    .putInt("dismissed_version_code", currentCode)
                    .apply()
            } catch (e: Exception) {
                Log.w(TAG, "Could not record post-update version preferences", e)
            }

            // 4. Automatically relaunch MasterWave to latest version smoothly
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct auto-relaunch failed, falling back to notification", e)
            }

            try {
                val nm = NotificationManagerCompat.from(context)
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val notification = androidx.core.app.NotificationCompat.Builder(context, "update_channel")
                    .setSmallIcon(com.masterwave.player.R.mipmap.ic_launcher)
                    .setContentTitle("MasterWave Updated")
                    .setContentText("MasterWave v${BuildConfig.VERSION_NAME} is ready to play.")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .build()

                nm.notify(30_001, notification)
            } catch (e: Exception) {
                Log.w(TAG, "Could not post post-update notification", e)
            }
        }
    }

    companion object {
        private const val TAG = "AppPackageReplaced"
    }
}
