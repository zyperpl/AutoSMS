package pl.zyper.autosms

import android.content.Context
import android.util.Log
import org.osmdroid.util.GeoPoint
import java.util.*

class TriggerBuilder(val context: Context? = null) {

    private var message: CharSequence? = null
    private var selectedContacts: List<ContactModel>? = null
    private var entryDirection: TriggerItemModel.LocationEntryDirection? = null
    private var radius: Double? = null
    private var coordinate: GeoPoint? = null
    private var date: Calendar? = null

    fun setDate(date: Calendar) {
        this.date = date
    }

    fun setCoordinate(geoPoint: GeoPoint) {
        this.coordinate = geoPoint
    }

    fun setRadius(radius: Double) {
        this.radius = radius
    }

    fun setEntryDirection(entryDirection: TriggerItemModel.LocationEntryDirection) {
        this.entryDirection = entryDirection
    }

    fun setContacts(selectedContacts: List<ContactModel>) {
        this.selectedContacts = selectedContacts
    }

    fun setTextMessage(message: CharSequence?) {
        this.message = message
    }

    fun build(): TriggerItemModel {
        try {
            checkValidity()
        } catch (e: Exception) {
            //Log.w("TriggerBuilder", "${e.message}")
            throw e
        }

        var triggerData: TriggerItemModel.Data? = null
        if (date == null) {
            if (coordinate != null && radius != null && entryDirection != null) {
                triggerData =
                    TriggerItemModel.LocationData(coordinate!!, radius!!, entryDirection!!)
            }
        } else if (date != null) {
            triggerData = TriggerItemModel.TimeData(date!!)
        }

        if (triggerData != null) {
            return TriggerItemModel(message.toString(), triggerData, selectedContacts!!)
        }

        // not reached
        throw Exception(Exception.Type.Unknown)
    }

    private fun checkValidity() {
        if (message.isNullOrEmpty()) {
            throw Exception(Exception.Type.NoMessage)
        }
        if (selectedContacts.isNullOrEmpty()) {
            throw Exception(Exception.Type.NoContacts)
        }

        if (date == null) {
            when {
                entryDirection == null -> throw Exception(Exception.Type.NoEntryDirection)
                radius == null -> throw Exception(Exception.Type.NoRadius)
                coordinate == null -> throw Exception(Exception.Type.NoCoordinate)
            }
        } else {
            if (date!! < Calendar.getInstance()) {
                throw Exception(Exception.Type.DatePast)
            }
        }

        if (date == null && entryDirection == null && radius == null && coordinate == null) {
            throw Exception(Exception.Type.NoDate)
        }
    }

    class Exception(val type: Type) : Throwable() {
        enum class Type {
            NoMessage, NoContacts, NoEntryDirection, NoRadius, NoCoordinate, NoDate, DatePast, Unknown
        }
    }

    companion object {
        fun getExceptionInfo(context: Context, exception: Exception): String {
            return context.resources.getString(
                when (exception.type) {
                    Exception.Type.NoMessage -> R.string.trigger_builder_exception_nomessage
                    Exception.Type.NoContacts -> R.string.trigger_builder_exception_nocontacts
                    Exception.Type.NoEntryDirection -> R.string.trigger_builder_exception_noentrydirection
                    Exception.Type.NoRadius -> R.string.trigger_builder_exception_noradius
                    Exception.Type.NoCoordinate -> R.string.trigger_builder_exception_nocoordinate
                    Exception.Type.NoDate -> R.string.trigger_builder_exception_nodate
                    Exception.Type.DatePast -> R.string.trigger_builder_exception_datepast
                    Exception.Type.Unknown -> R.string.trigger_builder_exception_unknown
                }
            )
        }
    }
}
