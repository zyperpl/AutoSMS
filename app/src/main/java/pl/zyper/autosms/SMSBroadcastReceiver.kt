package pl.zyper.autosms

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import java.util.*

class SMSBroadcastReceiver : BroadcastReceiver() {
    private lateinit var smsManager: SmsManager

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("SMSBroadcastReceiver", "intent $intent received")

        if (intent.action == null) return

        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            Log.i("SMSBroadcastReceiver", "BOOT COMPLETED")
            initializeTriggers(context)
        }

        if (intent.action!!.startsWith(ACTION_PREFIX)) {
            Log.i("SMSBroadcastReceiver", "Sending SMS")
            sendSMS(context, intent)
        }
    }

    private fun initializeTriggers(context: Context) {
        val dao = TriggerDbDAO(context)
        val triggers = dao.getAllTriggers()
        val triggerInitializer = TriggerInitializer(context)


        for (trigger in triggers) {
            triggerInitializer.setTrigger(trigger)
            Log.i("TRIGGER LOAD", "Trigger ${trigger.message} loaded!")
        }
    }

    private fun sendSMS(context: Context, intent: Intent) {
        smsManager = SmsManager.getDefault()
        val contactNumbers = getContactNumbers(intent)
        val triggerId = intent.getLongExtra(INTENT_EXTRA_TRIGGER_ID, -1)
        assert(triggerId >= 0)

        val trigger = TriggerDbDAO(context).getTriggerInfo(triggerId)
        if (trigger == null || trigger.sentDate != null) {
            Log.w("SMS SEND", "trigger is null or sentDate is not null (trigger=$trigger)")
            return
        }


        var message = intent.getStringExtra(INTENT_EXTRA_MESSAGE)

        if (message == null || message != trigger.message) {
            Log.i("SMSBroadcastRecvSendSMS", "message=$message; trigger.message=${trigger.message}")
            message = trigger.message
        }

        if (message == null) {
            Toast.makeText(context, "Message text not specified!", Toast.LENGTH_LONG).show()
            return
        }

        Log.i("SMSBroadcastRecvSendSMS", "$triggerId: $message $contactNumbers")

        var success = true
        for (contactNumber in contactNumbers) {
            if (!sendMessage(contactNumber, message)) {
                success = false
            }
        }
        if (success) {
            Log.i("SMSBroadcastRecvSendSMS", "SMS Message sent!")
            Toast.makeText(context, "SMS $message sent!", Toast.LENGTH_LONG).show()

            val updated =
                TriggerDbDAO(context).updateTriggerInfo(triggerId, message, Calendar.getInstance())
            if (!updated) {
                Toast.makeText(context, "SMS entry cannot be updated!", Toast.LENGTH_LONG).show()
                Log.e("SMSBroadcastRecSendSMS", "Trigger $triggerId not updated!")
            }
        }
    }

    private fun sendMessage(number: String, message: String): Boolean {
        if (message.startsWith("_ANDROIDTEST") || number.startsWith("0000000")) {
            Log.w("snedMessage", "DETECTED ANDROID TEST MESSAGE! num=$number msg=$message")
            return true
        }

        return try {
            smsManager.sendTextMessage(number, null, message, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getContactNumbers(intent: Intent): List<String> {
        val contactString = intent.getStringExtra(INTENT_EXTRA_CONTACTS)!!
        return contactString.split(';')
    }

    companion object {
        const val INTENT_EXTRA_TRIGGER_ID = "triggerID"
        const val INTENT_EXTRA_MESSAGE = "messageString"
        const val INTENT_EXTRA_CONTACTS = "contactsString"
        const val TAG = "SMSBroadcastReceiver"
        const val ACTION_PREFIX = "pl.zyper.autosms.sendsms="

        fun start(context: Context, trigger: TriggerItemModel) {
            val alarmIntent = Intent(context, SMSBroadcastReceiver::class.java).let { intent ->
                intent.data =
                    Uri.parse("autosms://" + trigger.contacts.joinToString() + trigger.message)
                intent.action = ACTION_PREFIX +
                        trigger.id.toString() + trigger.contacts.joinToString() + trigger.message.take(
                    5
                )

                val contactsString: String =
                    trigger.contacts.fold(String()) { acc: String, element -> acc + element.number + ";" }
                        .trim(';')

                intent.putExtra(INTENT_EXTRA_TRIGGER_ID, trigger.id)
                intent.putExtra(INTENT_EXTRA_MESSAGE, trigger.message)
                intent.putExtra(INTENT_EXTRA_CONTACTS, contactsString)

                PendingIntent.getBroadcast(context, 0, intent, 0)
            }
            alarmIntent.send()
        }
    }
}