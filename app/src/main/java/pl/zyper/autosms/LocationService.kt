package pl.zyper.autosms

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import java.util.*

class LocationService : Service(), TriggerLocationCallback.Caller {
    lateinit var notificationController: NotificationController
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationCallback: TriggerLocationCallback
    internal var otherContext: Context? = null

    private var isForeground = false

    override fun onCreate() {
        Log.i("LocationService", "onCreate")

        super.onCreate()

        otherContext = this

        notificationController = NotificationController(this)
        notificationController.build("Checking location...")
        notificationController.show()

        startForeground(notificationController.notificationID, notificationController.notification)
        isForeground = true

        setLocationCallback()
    }

    private fun setLocationCallback() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = TriggerLocationCallback(this, this)

        val locationRequest = LocationRequest()
        locationRequest.interval = 1000 * 30
        locationRequest.fastestInterval = 1000 * 60
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.maxWaitTime = 1000 * 60 * 5

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("LocationService", "onStartCommand intent=$intent")

        if (intent != null) {
            val bundle = intent.getBundleExtra(INTENT_KEY_PREVIOUS_LOCATION_BUNDLE)

            if (bundle != null) {

                val lat = bundle.getDouble(INTENT_KEY_PREVIOUS_LATITUDE, 0.0)
                val long = bundle.getDouble(INTENT_KEY_PREVIOUS_LONGITUDE, 0.0)

                Log.i("LocationService", "$lat $long")

            }
        }

        Log.i("LocationService", "intent ")

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("LocationService", "onBind")

        return null
    }

    override fun onTriggerCheck(trigger: TriggerItemModel) {

    }

    override fun getPreviousLocation(): Location? {
        val locations = TriggerDbDAO(otherContext!!).getLastLocations()
        if (locations.isEmpty()) return null

        return locations.last().location
    }

    override fun setPreviousLocation(location: Location?) {
        if (location == null) return

        TriggerDbDAO(otherContext!!).addLastLocation(LocationDate(location, Calendar.getInstance()))
    }

    override fun onNearestCheck(info: Triple<Int, Int, Int>) {
        Log.i("onNearestCheck", "near=$info ")

        val previousLocation = getPreviousLocation()

        val near = info.first
        val sent = info.second
        val pending = info.third

        if (near == 0) {
            if (pending > 0) {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)

                setAlarm(
                    otherContext!!,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES / 2,
                    previousLocation
                )
            }

            if (isForeground) {
                try {
                    stopForeground(true)
                    stopSelf()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("LocationService", "exception while stopping")
                }
            }
        }

        if (near > 0) {
            notificationController.hide()
            notificationController.build("You are near $near message location(s)!")
            notificationController.show()
        }
    }

    companion object {
        fun setAlarm(context: Context, timeoutMillis: Long, previousLocation: Location? = null) {
            Log.i("LocationService", "setAlarm $timeoutMillis $previousLocation")

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = getIntent(context, previousLocation)

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //Log.i("LocationService", "PendingIntent.getForegroundService")
                PendingIntent.getForegroundService(context, 0, intent, 0)
            } else {
                Log.i("LocationService", "PendingIntent.getService (not foreground)")
                PendingIntent.getService(context, 0, intent, 0)
            }

            am.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + timeoutMillis,
                pendingIntent
            )
        }

        fun getIntent(context: Context, previousLocation: Location?): Intent {
            val intent = Intent(context, LocationService::class.java)
            intent.data = Uri.parse("autosms://location_check")
            intent.action = "AutoSMSLocationServiceCheck"

            if (previousLocation != null) {
                val bundle = Bundle()
                bundle.putDouble(INTENT_KEY_PREVIOUS_LATITUDE, previousLocation.latitude)
                bundle.putDouble(INTENT_KEY_PREVIOUS_LONGITUDE, previousLocation.longitude)

                intent.putExtra(INTENT_KEY_PREVIOUS_LOCATION_BUNDLE, bundle)
            }
            return intent
        }

        const val INTENT_KEY_PREVIOUS_LATITUDE = "PreviousLatitude"
        const val INTENT_KEY_PREVIOUS_LONGITUDE = "PreviousLongitude"
        const val INTENT_KEY_PREVIOUS_LOCATION = "PreviousLocation"
        const val INTENT_KEY_PREVIOUS_LOCATION_BUNDLE = "PreviousLocationBundle"
    }
}

data class LocationDate(val location: Location, val date: Calendar)