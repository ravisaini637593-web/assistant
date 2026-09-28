package com.example.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

data class NotificationSummary(
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class AssistantNotificationListener : NotificationListenerService() {

    companion object {
        var isConnected: Boolean = false
            private set

        private val recentNotifications = mutableListOf<NotificationSummary>()

        fun getRecent(filter: String? = null): List<NotificationSummary> {
            synchronized(recentNotifications) {
                return if (filter.isNullOrBlank()) {
                    recentNotifications.takeLast(10).reversed()
                } else {
                    recentNotifications.filter {
                        it.packageName.contains(filter, ignoreCase = true) ||
                                it.title.contains(filter, ignoreCase = true) ||
                                it.text.contains(filter, ignoreCase = true)
                    }.takeLast(10).reversed()
                }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isNotBlank() || text.isNotBlank()) {
            synchronized(recentNotifications) {
                recentNotifications.add(
                    NotificationSummary(
                        packageName = sbn.packageName,
                        title = title.take(50),
                        text = text.take(100),
                        postTime = sbn.postTime
                    )
                )
                if (recentNotifications.size > 50) {
                    recentNotifications.removeAt(0)
                }
            }
        }
    }
}
