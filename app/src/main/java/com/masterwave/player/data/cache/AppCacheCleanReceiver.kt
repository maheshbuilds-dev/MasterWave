package com.masterwave.player.data.cache

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppCacheCleanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == CacheCleanManager.ACTION_HOURLY_CACHE_CLEAN ||
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d("AppCacheCleanReceiver", "Hourly cache clean broadcast received: ${intent.action}")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CacheCleanManager.getInstance(context).clearAppAndOfflineCache()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
