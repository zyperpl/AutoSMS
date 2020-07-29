package pl.zyper.autosms

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList


class TriggerListAdapter(
    private val mContext: Context,
    private val eventsReceiver: EventReceiver
) : RecyclerView.Adapter<TriggerListAdapter.ViewHolder>(), Filterable {

    val list: ArrayList<TriggerItemModel> = arrayListOf()
    var filteredList: java.util.ArrayList<TriggerItemModel> = list
    var category: CharSequence = ""

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val listFromCategory = when (category) {
                    PENDING -> list.filter { it.sentDate == null }
                    COMPLETED -> list.filter { it.sentDate != null }
                    else -> list
                }

                val constraintLc = constraint.toString().toLowerCase(Locale.getDefault())
                filterResults.values = listFromCategory.filter { (message, _, contacts) ->
                    message.toLowerCase(Locale.getDefault()).contains(constraintLc)
                            || contacts.any { (name, number) ->
                        number.contains(constraintLc) || name.contains(constraintLc)
                    }
                }

                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                val values = results.values

                if (values is ArrayList<*>) {
                    filteredList = values as ArrayList<TriggerItemModel>

                }
                notifyDataSetChanged()
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(mContext).inflate(R.layout.trigger_item, parent, false)
        return ViewHolder(v, eventsReceiver)
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun getItemId(position: Int): Long {
        return filteredList[position].id
    }

    fun removeItem(trigger: TriggerItemModel?) {
        if (trigger == null) return

        val pos = filteredList.indexOfFirst { it.id == trigger.id }

        filteredList.remove(trigger)
        list.remove(trigger)
        notifyItemRemoved(pos)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trigger = filteredList[position]
        holder.updateTrigger(mContext, trigger)

        //Log.i("TRIGGER LIST ADAPTER", "holder.itemView=${holder.itemView}")
    }

    companion object {
        const val PENDING: String = "pending"
        const val COMPLETED: String = "completed"
        const val ALL: String = ""
    }

    class ViewHolder(private val view: View, eventReceiver: EventReceiver) :
        RecyclerView.ViewHolder(view) {
        private val triggerName = view.findViewById(R.id.trigger_name) as TextView
        private val triggerInfo = view.findViewById(R.id.trigger_info) as TextView
        private val triggerIcon = view.findViewById(R.id.trigger_icon) as ImageView
        private val triggerDescription = view.findViewById(R.id.trigger_description) as TextView

        private var trigger: TriggerItemModel? = null

        init {
            view.setOnClickListener {
                Log.d("ViewHolder", "setOnClickListener")
                eventReceiver.onItemOpen(trigger)
            }

            view.setOnLongClickListener {
                Log.w("ViewHolder", "setOnLongClickListener")

                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.menu_trigger, popup.menu)

                if (trigger?.sentDate == null) {
                    // nothing to restart
                    popup.menu.findItem(R.id.action_restart).isEnabled = false
                    popup.menu.findItem(R.id.action_restart).isVisible = false
                }

                popup.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            eventReceiver.onItemEdit(trigger)
                            true
                        }
                        R.id.action_restart -> {
                            eventReceiver.onItemRestart(trigger)
                            true
                        }
                        R.id.action_remove -> {
                            eventReceiver.onItemRemove(trigger)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()

                true
            }
        }

        fun updateTrigger(context: Context, trigger: TriggerItemModel) {
            this.trigger = trigger

            triggerName.text = trigger.getTitle()

            if (trigger.sentDate != null) {
                /*triggerName.text =
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(trigger.sentDate!!.time)*/
                itemView.setBackgroundColor(context.resources.getColor(R.color.colorDisabled))
            } else {
                itemView.setBackgroundColor(context.resources.getColor(R.color.contactBackground))
            }

            triggerInfo.text = trigger.getInfoString(context)

            if (trigger.data is TriggerItemModel.TimeData) {
                triggerIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_timer_black))
            } else {
                triggerIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_location_black))
            }

            triggerDescription.text = trigger.getContactsString(context)
        }
    }

    interface EventReceiver {
        fun onItemOpen(trigger: TriggerItemModel?)
        fun onItemEdit(trigger: TriggerItemModel?)
        fun onItemRestart(trigger: TriggerItemModel?)
        fun onItemRemove(trigger: TriggerItemModel?)
    }
}