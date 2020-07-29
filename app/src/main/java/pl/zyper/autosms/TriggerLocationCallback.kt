package pl.zyper.autosms

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlin.math.absoluteValue

class TriggerLocationCallback(
    val context: Context,
    private val caller: Caller
) :
    LocationCallback() {
    private var tries: Long = 0

    override fun onLocationResult(result: LocationResult?) {
        super.onLocationResult(result)

        Log.i("LocationCallback", result.toString())

        if (result == null) return
        if (result.lastLocation == null) return

        val location = result.lastLocation

        Log.d("LocationCallback", "accuracy: " + result.lastLocation.accuracy.toString())

        if (result.lastLocation.accuracy > 100.0 && tries < MAX_TRIES) {
            tries++
            return
        }

        val geoPoint = location.toGeoPoint() ?: return

        val previousLocation = caller.getPreviousLocation()


        var info = Triple(0, 0, 0)

        Log.d("LocationCallback", geoPoint.toDoubleString())

        if (previousLocation != null) {
            info = checkLocationTriggers(location, previousLocation)
        } else {
            Log.w("LocationCallback", "previous location is null!")
        }

        caller.onNearestCheck(info)
        if (location.accuracy < 70.0) {
            caller.setPreviousLocation(location)
        }
    }

    private fun checkLocationTriggers(
        current: Location,
        previous: Location
    ): Triple<Int, Int, Int> {
        Log.i("checkLocationTriggers", "current=$current \t previous=$previous")
        Log.d(
            "checkLocationTriggers",
            "distance moved=${current.distanceTo(previous)}m"
        )

        var near = 0
        var sent = 0
        var pending = 0

        val dao = TriggerDbDAO(context)
        val triggers = dao.getLocationTriggers()
        for (t in triggers) {
            Log.d("checkLocationTriggers", "trigger: $t")

            caller.onTriggerCheck(t)

            val data = t.data as TriggerItemModel.LocationData
            val dist = current.toGeoPoint()!!.distanceToAsDouble(data.coordinate) - data.radius
            Log.d("checkLocationTriggers", "dist=$dist")

            val currentInside = current.toGeoPoint()!!.isInRadius(data.coordinate, data.radius)
            val previousInside =
                previous.toGeoPoint()!!.isInRadius(data.coordinate, data.radius)

            val direction: TriggerItemModel.LocationEntryDirection =
                if (!previousInside && currentInside) {
                    TriggerItemModel.LocationEntryDirection.Entering
                } else if (previousInside && !currentInside) {
                    TriggerItemModel.LocationEntryDirection.Leaving
                } else {
                    TriggerItemModel.LocationEntryDirection.None
                }

            if (data.direction == TriggerItemModel.LocationEntryDirection.Leaving && dist < 0) {
                if (dist.absoluteValue < 100) {
                    near++
                }
            }
            if (data.direction == TriggerItemModel.LocationEntryDirection.Entering && dist > 0) {
                if (dist.absoluteValue < 100) {
                    near++
                }
            }

            Log.i(
                "checkLocationTriggers",
                "entry direction (previousInside=$previousInside; currentInside=$currentInside) = $direction"
            )

            if (t.sentDate == null) {
                pending++
            }

            if (direction == data.direction && t.sentDate == null) {
                // send sms
                SMSBroadcastReceiver.start(context, t)
                sent++
                pending--
            }
        }

        return Triple(near, sent, pending)
    }

    interface Caller {
        fun onTriggerCheck(trigger: TriggerItemModel)
        fun getPreviousLocation(): Location?
        fun setPreviousLocation(location: Location?)
        fun onNearestCheck(info: Triple<Int, Int, Int>)
    }

    companion object {
        const val MAX_TRIES: Int = 30
    }
}