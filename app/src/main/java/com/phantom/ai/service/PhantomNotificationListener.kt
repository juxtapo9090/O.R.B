package com.phantom.ai.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification

class PhantomNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        PhantomContext.lastNotification = if (title.isNotBlank()) "$title: $text" else text
        PhantomContext.lastNotificationApp = sbn.packageName
    }
}
