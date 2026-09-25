package com.morningrefresh.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.morningrefresh.app.MorningRefreshApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as MorningRefreshApplication
        CoroutineScope(Dispatchers.IO).launch {
            app.repository.enabledAlarms().forEach(app.alarmScheduler::schedule)
            pendingResult.finish()
        }
    }
}
