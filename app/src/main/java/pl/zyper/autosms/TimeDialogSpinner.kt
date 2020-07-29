package pl.zyper.autosms

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.widget.Spinner
import java.util.*


class TimeDialogSpinner(private val mContext: Context, private val attrs: AttributeSet) :
    Spinner(mContext, attrs) {
    private val adapter: SingleItemAdapter<String>

    var value: TimeValue = TimeValue(0, 0)
        set(v) {
            field = v
            adapter.setAdapterItem(timeToString(v.hour, v.minute))
            adapter.notifyDataSetChanged()
        }


    init {
        val calendar = Calendar.getInstance()
        adapter = SingleItemAdapter(mContext, timeToString(calendar))
        value = TimeValue(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

        super.setAdapter(adapter)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        val listener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            value = TimeValue(hour, minute)
            adapter.setAdapterItem(timeToString(hour, minute))
        }

        val use24HourClock = DateFormat.is24HourFormat(mContext)

        val dialog = TimePickerDialog(
            mContext, listener,
            value.hour, value.minute,
            use24HourClock
        )

        dialog.show()

        return dialog.isShowing
    }

    private fun timeToString(calendar: Calendar): String {
        val h = calendar.get(Calendar.HOUR_OF_DAY)
        val m = calendar.get(Calendar.MINUTE)
        return timeToString(h, m)
    }

    private fun timeToString(hour: Int, minute: Int): String {
        val cal = GregorianCalendar(0, 0, 0)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)

        return DateFormat.getTimeFormat(mContext).format(cal.time)
    }

    fun setFromCalendar(date: Calendar) {
        val h = date.get(Calendar.HOUR_OF_DAY)
        val m = date.get(Calendar.MINUTE)

        this.value = TimeValue(h, m)
    }

    data class TimeValue(val hour: Int, val minute: Int)
}