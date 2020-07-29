package pl.zyper.autosms

import android.content.Context
import android.location.Location
import org.osmdroid.util.GeoPoint
import java.text.DateFormat
import java.util.*

data class TriggerItemModel(
    val message: String,
    val data: Data,
    val contacts: List<ContactModel>,
    var sentDate: Calendar? = null,
    var id: Long = -1
) {
    private val titleMaxLength = 20

    fun getTitle(): String {
        var msg = message.replace("\\s".toRegex(), " ")
        return msg
    }

    fun getContactsString(context: Context): String {

        var d = "${contacts.size} contacts"

        if (contacts.size == 1) {
            d = contacts.first().number
        } else if (contacts.size == 2) {
            d = contacts.first().number + ", "
            d += contacts.last().number
        }

        return d
    }

    fun getInfoString(context: Context): String {
        var d = ""
        if (data is LocationData) {
            val long = Location.convert(data.coordinate.longitude, Location.FORMAT_DEGREES)
            val lat = Location.convert(data.coordinate.latitude, Location.FORMAT_DEGREES)

            if (data.direction == LocationEntryDirection.Leaving) {
                d += context.resources.getString(R.string.trigger_direction_on_leave)
            } else if (data.direction == LocationEntryDirection.Entering) {
                d += context.resources.getString(R.string.trigger_direction_on_enter)
            }

            d += "  ${data.radius}m - $lat;$long"
        } else if (data is TimeData) {
            d += DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(data.date.time)
        }
        return d
    }

    enum class LocationEntryDirection {
        None, Leaving, Entering
    }

    open class Data

    data class LocationData(
        val coordinate: GeoPoint,
        val radius: Double,
        val direction: LocationEntryDirection = LocationEntryDirection.None
    ) : Data()

    data class TimeData(val date: Calendar) : Data()
}
