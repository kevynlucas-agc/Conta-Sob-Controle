package br.com.contasobcontrole.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import br.com.contasobcontrole.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "conta-sob-controle.db").build()
                db.dao().observeAll().first().filter { !it.isPaid }.forEach {
                    DueDateReceiver.schedule(context, it.id, it.name, it.dueDate)
                }
                db.close()
            } finally {
                pending.finish()
            }
        }
    }
}
