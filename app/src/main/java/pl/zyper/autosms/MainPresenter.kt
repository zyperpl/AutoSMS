package pl.zyper.autosms

import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import android.widget.Toast
import java.util.*

class MainPresenter(val activity: Activity) : TriggerListAdapter.EventReceiver {
    var triggerListAdapter: TriggerListAdapter = TriggerListAdapter(activity, this)
    var addTriggerFab: AddTriggerFab = AddTriggerFab(activity)

    fun startTriggerCreationActivity(layoutId: Int, editTriggerId: Long? = null) {
        val triggerId: Long = editTriggerId ?: -1

        val intent = TriggerCreationActivity.newIntent(activity, layoutId, triggerId)
        activity.startActivityForResult(intent, TriggerCreationActivity.CREATE_TRIGGER_REQUEST)
    }

    fun loadTriggers() {
        val dao = TriggerDbDAO(activity)
        val triggers = dao.getAllTriggers()
        val triggerInitializer = TriggerInitializer(activity)

        triggerListAdapter.list.clear()
        triggerListAdapter.filteredList.clear()

        for (trigger in triggers) {
            triggerListAdapter.list.add(trigger)
            triggerListAdapter.filteredList = triggerListAdapter.list

            if (trigger.sentDate == null) {
                triggerInitializer.setTrigger(trigger)
                Log.i("TRIGGER LOAD", "Trigger ${trigger.message} loaded!")
            }
        }

        triggerListAdapter.notifyDataSetChanged()
    }

    private fun openTriggerForEditing(trigger: TriggerItemModel) {
        activity.startActivityForResult(
            TriggerCreationActivity.newIntent(
                activity,
                trigger.data.toLayoutId(),
                trigger.id
            ), TriggerCreationActivity.EDIT_TRIGGER_REQUEST
        )
    }

    override fun onItemOpen(trigger: TriggerItemModel?) {
        if (trigger == null) return

        openTriggerForEditing(trigger)
    }

    override fun onItemEdit(trigger: TriggerItemModel?) {
        if (trigger == null) return

        openTriggerForEditing(trigger)
    }

    override fun onItemRestart(trigger: TriggerItemModel?) {
        if (trigger == null) return

        trigger.sentDate = null
        if (trigger.data is TriggerItemModel.TimeData) {
            val today = Calendar.getInstance()

            if (trigger.data.date < today) {
                trigger.data.date.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
                trigger.data.date.set(Calendar.MONTH, today.get(Calendar.MONTH))
                trigger.data.date.set(Calendar.YEAR, today.get(Calendar.YEAR))

                if (trigger.data.date <= today) {
                    trigger.data.date.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
        val ret = TriggerDbDAO(activity).updateTrigger(trigger.id, trigger) >= 0
        if (!ret) {
            Toast.makeText(activity, "Cannot restart trigger!", Toast.LENGTH_LONG).show()
            Log.e("trEventReceiver", "cannot restart trigger $trigger")
        }

        TriggerInitializer(activity).setTrigger(trigger)
        triggerListAdapter.notifyDataSetChanged()
    }

    override fun onItemRemove(trigger: TriggerItemModel?) {
        if (trigger == null) return

        val ret = TriggerDbDAO(activity).deleteTrigger(trigger.id)
        if (ret) {
            Toast.makeText(activity, "Trigger removed!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "Trigger cannot be removed!", Toast.LENGTH_LONG).show()
            Log.e("trEventReceiver", "cannot remove trigger id ${trigger.id}!")
        }

        triggerListAdapter.removeItem(trigger)
        triggerListAdapter.notifyDataSetChanged()
    }

    fun onFlushDatabase(): Boolean {
        val builder: AlertDialog.Builder = activity.let { AlertDialog.Builder(it) }

        builder.setMessage(activity.getString(R.string.dialog_this_will_delete_all_messages))
        builder.setTitle(activity.getString(R.string.dialog_are_you_sure))
        builder.setCancelable(true)
        builder.setPositiveButton(R.string.dialog_confirm) { _, _ -> flushDatabase() }
        builder.setNegativeButton(R.string.dialog_no) { _, _ -> null }

        val dialog: AlertDialog? = builder.create()
        dialog?.show()

        return true
    }

    private fun flushDatabase() {
        TriggerDbHelper(activity).removeAllTables()
        TriggerDbHelper(activity).createAllTables()
        loadTriggers()
    }
}