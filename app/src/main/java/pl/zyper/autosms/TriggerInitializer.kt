package pl.zyper.autosms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.util.*

class TriggerInitializer(val context: Context) {

    var alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val locationServiceInitialDelay: Long = 1000 * 20 // seconds

    fun setTrigger(trigger: TriggerItemModel) {

        if (trigger.data is TriggerItemModel.TimeData) {

            if (trigger.data.date < Calendar.getInstance()) {
                Log.w("TriggerInitializer", "trigger $trigger date < Calendar.getInstance!")
                return
            }

            val alarmIntent = Intent(context, SMSBroadcastReceiver::class.java).let { intent ->
                intent.data = Uri.parse("autosms://" + trigger.id.toString())
                intent.action = SMSBroadcastReceiver.ACTION_PREFIX + trigger.id.toString()

                val contactsString =
                    trigger.contacts.fold(String()) { acc: String, element -> acc + element.number + ";" }
                        .trim(';')

                intent.putExtra(SMSBroadcastReceiver.INTENT_EXTRA_TRIGGER_ID, trigger.id)
                intent.putExtra(SMSBroadcastReceiver.INTENT_EXTRA_MESSAGE, trigger.message)
                intent.putExtra(SMSBroadcastReceiver.INTENT_EXTRA_CONTACTS, contactsString)

                PendingIntent.getBroadcast(context, 0, intent, 0)
            }

            alarmManager.tryExactAlarm(
                AlarmManager.RTC_WAKEUP,
                trigger.data.date.timeInMillis,
                alarmIntent
            )

            Log.i("TriggerInitializer", "Set tr for date ${trigger.data.date}!")


        } else if (trigger.data is TriggerItemModel.LocationData) {
            LocationService.setAlarm(context, locationServiceInitialDelay)
            Log.i("TriggerInitializer", "Set tr for loc ${trigger.data.coordinate}!")
        }
    }
}