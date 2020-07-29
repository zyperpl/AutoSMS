package pl.zyper.autosms

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.location.Address
import android.location.Location
import android.os.Build
import org.osmdroid.util.GeoPoint
import java.util.*


internal fun Location?.toGeoPoint(): GeoPoint? {
    return if (this == null) {
        null
    } else {
        GeoPoint(this.latitude, this.longitude, this.altitude)
    }
}

internal fun GeoPoint.isInRadius(point: GeoPoint, radius: Double): Boolean {
    return this.distanceToAsDouble(point) <= radius
}


@SuppressLint("ObsoleteSdkInt")
internal fun AlarmManager.tryExactAlarm(
    type: Int,
    timeInMillis: Long,
    alarmIntent: PendingIntent
) {
    if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT
        && Build.VERSION.SDK_INT < Build.VERSION_CODES.M
    ) {
        setExact(type, timeInMillis, alarmIntent)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        setExactAndAllowWhileIdle(type, timeInMillis, alarmIntent)
    } else {
        set(type, timeInMillis, alarmIntent)
    }
}

internal fun Address.getName(): String {
    val SEPERATOR = " "

    val sb = StringBuilder()
    for (i in 0 until maxAddressLineIndex) {
        sb.append(getAddressLine(i)).append(SEPERATOR)
    }
    sb.append(locality).append(SEPERATOR)
    sb.append(postalCode).append(SEPERATOR)
    sb.append(countryName)
    return sb.toString()
}

internal fun Location.fromCoordinate(lat: Double, long: Double): Location {
    val l = Location("DummyProvider")
    l.latitude = lat
    l.longitude = long
    return l
}

internal fun Calendar.fromLong(n: Long?): Calendar? {
    if (n == null) {
        return null
    }

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = n * 1000
    return calendar
}

internal fun Long.Companion.fromCalendar(cal: Calendar?): Long? {
    if (cal == null) {
        return null
    }

    return cal.timeInMillis / 1000
}

internal fun String.toPhoneNumber(): String {
    return this.replace(" ", "").replace("-", "").trim()
}

internal fun TriggerItemModel.Data.toLayoutId(): Int {
    when (this) {
        is TriggerItemModel.TimeData -> return R.layout.trigger_creation_time
        is TriggerItemModel.LocationData -> return R.layout.trigger_creation_location
    }

    // fallback
    return R.layout.trigger_creation_location
}