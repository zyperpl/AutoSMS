package pl.zyper.autosms

import android.content.Context
import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.ServiceTestRule
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint


@RunWith(AndroidJUnit4::class)
@MediumTest
class LocationServiceTest {
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    @get:Rule
    val serviceRule: ServiceTestRule = ServiceTestRule()

    @get:Rule
    val activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @Test
    fun createLocationTrigger() {
        val tr = TriggerItemModel(
            TriggerOperationTest.MESSAGE_TEXT,
            TriggerItemModel.LocationData(
                GeoPoint(51.654, 32.123),
                200.0,
                TriggerItemModel.LocationEntryDirection.Entering
            ),
            listOf(
                ContactModel(
                    TriggerOperationTest.PHONE_NUMBER, TriggerOperationTest.PHONE_NUMBER, null
                )
            )
        )

        Assert.assertTrue(TriggerDbDAO(TriggerOperationTest.context).addTrigger(tr) >= 0)
    }

    @Test
    fun startLocationService() {
        val intent = LocationService.getIntent(context, null)
        val binder = serviceRule.bindService(intent)
    }

    @Test
    fun setNotification() {
        val notificationController =
            NotificationController(activityRule.activity.applicationContext)
        notificationController.build("_ESPRESSO_TEST")
        notificationController.show()
        notificationController.hide()
    }

    @Test
    fun setLocationCallback() {
        createLocationTrigger()
        createLocationTrigger()

        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(activityRule.activity.applicationContext)
        val locationService = LocationService()
        locationService.otherContext = activityRule.activity.applicationContext
        val locationCallback =
            TriggerLocationCallback(activityRule.activity.applicationContext, locationService)

        locationService.locationCallback = locationCallback
        locationService.fusedLocationProviderClient = fusedLocationProviderClient

        locationCallback.onLocationResult(
            LocationResult.create(
                mutableListOf(
                    Location("dummy").fromCoordinate(
                        0.0,
                        0.0
                    )
                )
            )
        )

        val locationRequest = LocationRequest()
        locationRequest.interval = 1000 * 30
        locationRequest.fastestInterval = 1000 * 60
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.maxWaitTime = 1000 * 60 * 5

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            context.mainLooper
        )
    }
}