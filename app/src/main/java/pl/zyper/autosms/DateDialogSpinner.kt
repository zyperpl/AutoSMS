package pl.zyper.autosms

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.util.AttributeSet
import android.widget.Spinner
import java.text.DateFormat
import java.util.*


class DateDialogSpinner(val mContext: Context, val attrs: AttributeSet) : Spinner(mContext, attrs) {
    private val adapter: SingleItemAdapter<String>

    var value: DateValue = DateValue(0, 0, 0)
        set(v) {
            field = v
            adapter.setAdapterItem(calendarToString(v.year, v.month, v.dayOfMonth))
            adapter.notifyDataSetChanged()
        }


    init {
        val calendar = Calendar.getInstance()
        adapter = SingleItemAdapter(mContext, calendarToString(calendar))

        value = DateValue(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        super.setAdapter(adapter)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        val listener = DatePickerDialog.OnDateSetListener { datePicker, year, month, dayOfMonth ->

            value = DateValue(year, month, dayOfMonth)
            adapter.setAdapterItem(calendarToString(year, month, dayOfMonth))
        }

        val dialog = DatePickerDialog(
            mContext, listener,
            value.year, value.month, value.dayOfMonth
        )

        dialog.show()

        return dialog.isShowing
    }

    private fun calendarToString(calendar: Calendar): String {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        return calendarToString(y, m, d)
    }

    private fun calendarToString(year: Int, month: Int, dayOfMonth: Int): String {
        val cal = GregorianCalendar(0, 0, 0)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        return DateFormat.getDateInstance().format(cal.time)
    }

    fun setFromCalendar(date: Calendar) {
        val y = date.get(Calendar.YEAR)
        val m = date.get(Calendar.MONTH)
        val d = date.get(Calendar.DAY_OF_MONTH)

        this.value = DateValue(y, m, d)
    }

    data class DateValue(val year: Int, val month: Int, val dayOfMonth: Int)
}