package com.example.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val activeTimers = db.timerRecordDao().getActiveTimers().firstOrNull() ?: emptyList()
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    val now = System.currentTimeMillis()

                    for (timer in activeTimers) {
                        if (timer.endTimeMillis > now && alarmManager != null) {
                            val alertIntent = Intent(context, AlarmReceiver::class.java).apply {
                                action = AlarmReceiver.ACTION_TIMER_ALERT
                                putExtra(AlarmReceiver.EXTRA_TITLE, "Timer finished: ${timer.label}")
                                putExtra(AlarmReceiver.EXTRA_ID, timer.id)
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                timer.id.toInt(),
                                alertIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    timer.endTimeMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setExact(
                                    AlarmManager.RTC_WAKEUP,
                                    timer.endTimeMillis,
                                    pendingIntent
                                )
                            }
                        } else {
                            db.timerRecordDao().stopTimer(timer.id)
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
