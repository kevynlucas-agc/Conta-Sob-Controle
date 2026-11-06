package br.com.contasobcontrole.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import br.com.contasobcontrole.MainActivity
import br.com.contasobcontrole.R
import java.time.LocalDate
import java.time.ZoneId

class DueDateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Conta a vencer"
        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Lembretes de contas", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(if (date.isBlank()) "Confira suas contas no Conta Sob Controle." else "Vencimento previsto para $date.")
            .setContentIntent(open).setAutoCancel(true).build()
        manager.notify(intent.getLongExtra(EXTRA_ID, System.currentTimeMillis()).toInt(), notification)
    }

    companion object {
        const val CHANNEL = "due_dates"
        const val EXTRA_ID = "expense_id"
        const val EXTRA_TITLE = "expense_title"
        const val EXTRA_DATE = "expense_date"

        fun schedule(context: Context, id: Long, title: String, date: String) {
            val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
            val reminderDate = parsed.minusDays(1)
            var due = reminderDate.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (due <= System.currentTimeMillis()) {
                if (parsed != LocalDate.now()) return
                due = System.currentTimeMillis() + 60_000L
            }
            val intent = Intent(context, DueDateReceiver::class.java).apply {
                putExtra(EXTRA_ID, id); putExtra(EXTRA_TITLE, title); putExtra(EXTRA_DATE, date)
            }
            val pending = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending)
        }

        fun cancel(context: Context, id: Long) {
            val pending = PendingIntent.getBroadcast(context, id.toInt(), Intent(context, DueDateReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pending != null) (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
        }
    }
}
