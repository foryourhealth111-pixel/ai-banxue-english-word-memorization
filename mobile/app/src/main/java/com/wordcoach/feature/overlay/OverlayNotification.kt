package com.wordcoach.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wordcoach.R

object OverlayNotification {
  const val CHANNEL_ID = "word_coach_overlay"
  const val NOTIFICATION_ID = 1101

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Word Coach Overlay",
      NotificationManager.IMPORTANCE_LOW
    )
    manager.createNotificationChannel(channel)
  }

  fun build(context: Context): Notification {
    return NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle(context.getString(R.string.overlay_notification_title))
      .setContentText(context.getString(R.string.overlay_notification_text))
      .setOngoing(true)
      .build()
  }
}
