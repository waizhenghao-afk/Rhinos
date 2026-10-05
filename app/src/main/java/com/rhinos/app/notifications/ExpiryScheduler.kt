package com.rhinos.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rhinos.app.data.model.Trade
import java.time.LocalDate
import java.time.ZoneId

/**
 * One reminder per trade, delivered the day BEFORE the expiry date at 09:00 local
 * time. Recomputing the full schedule on every data change and reboot keeps the
 * alarm set consistent with the database.
 *
 * Robustness: the alarm request code is the trade id, and the receiver re-checks
 * the database before notifying, so alarms for edited/deleted trades are either
 * replaced (FLAG_UPDATE_CURRENT) or self-suppress silently.
 */
object ExpiryScheduler {

    const val CHANNEL_ID = "expiry_tips"
    const val EXTRA_TRADE_ID = "extra_trade_id"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Expiry Tips",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Gentle one-time reminders before a trade's margin period expires."
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Replaces the whole alarm schedule. Call after any trade change and on boot. */
    fun rescheduleAll(context: Context, trades: List<Trade>, tipsEnabled: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val today = LocalDate.now()
        for (trade in trades) {
            cancelOne(context, am, trade.id)
            if (!tipsEnabled) continue
            val remindOn = trade.expiryDate?.minusDays(1) ?: continue
            if (remindOn.isBefore(today)) continue
            scheduleOne(context, am, trade, remindOn)
        }
    }

    /** Alarm time: 09:00 on the day before expiry. */
    fun triggerAt(remindOn: LocalDate): Long =
        remindOn.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun pendingIntent(context: Context, trade: Trade): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            trade.id.toInt(), // request code = trade id: one alarm per trade
            Intent(context, ExpiryAlarmReceiver::class.java).apply {
                action = ExpiryAlarmReceiver.ACTION_EXPIRY_TIP
                putExtra(EXTRA_TRADE_ID, trade.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun scheduleOne(context: Context, am: AlarmManager, trade: Trade, remindOn: LocalDate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt(remindOn),
            pendingIntent(context, trade)
        )
    }

    private fun cancelOne(context: Context, am: AlarmManager, tradeId: Long) {
        val intent = Intent(context, ExpiryAlarmReceiver::class.java).apply {
            action = ExpiryAlarmReceiver.ACTION_EXPIRY_TIP
        }
        val pending = PendingIntent.getBroadcast(
            context,
            tradeId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) am.cancel(pending)
    }
}
