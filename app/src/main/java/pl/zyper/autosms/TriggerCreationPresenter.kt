package pl.zyper.autosms

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.location.Address
import android.os.Handler
import android.os.StrictMode
import android.preference.PreferenceManager
import android.provider.ContactsContract
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*
import kotlin.math.max

abstract class TriggerCreationPresenter(val context: Context, triggerId: Long? = null) {
    protected var trigger: TriggerItemModel? = null

    val contactsQueryListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                contactsAdapter.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                contactsAdapter.filter.filter(newText)
                return true
            }
        }

    val triggerBuilder: TriggerBuilder = TriggerBuilder(context)
    internal lateinit var contactsAdapter: ContactsAdapter

    init {
        fillContactsAdapter()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val ctx = context.applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().tileDownloadThreads = 8
        Configuration.getInstance().tileFileSystemThreads = 16

        if (triggerId != null && triggerId >= 0) {
            try {
                trigger = TriggerDbDAO(context).getTrigger(triggerId)
                Log.i("TriggerCreationPresentr", "Loaded trigger $trigger for presenter $this")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("TriggerCreationPresentr", e.toString())
            }
        }
    }

    fun fillContactsAdapter() {
        val contacts = fetchContacts()
        this.contactsAdapter = ContactsAdapter(
            context,
            contacts,
            ContextCompat.getDrawable(context, R.drawable.default_contact_image)!!
        )

    }

    private fun fetchContacts(): MutableMap<Int, ContactModel> {
        val map: MutableMap<Int, ContactModel> = mutableMapOf()

        val granted =
            PermissionManager(context as Activity).request(
                Manifest.permission.READ_CONTACTS,
                context.resources.getString(R.string.permission_dialog_contacts)
            )

        if (!granted) {
            return mutableMapOf()
        }

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        while (cursor!!.moveToNext()) {

            val contactId =
                cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
            val name =
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phoneNumber =
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))


            val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI,
                    contactId.toLong()
                )
            )

            var bitmapPhoto: Any? = null
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)

                val rndBitmap = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
                rndBitmap.cornerRadius = max(bitmap.width, bitmap.height) / 2.0f

                bitmapPhoto = rndBitmap
            }

            //Log.d("ContactsContract >>", "$contactId: $name  $phoneNumber")

            if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) == 1) {
                map[contactId] = ContactModel(name, phoneNumber, bitmapPhoto)
            }

        }
        cursor.close()

        return map
    }

    fun createTrigger(): Long {
        var newTrigger: TriggerItemModel? = null
        try {
            newTrigger = triggerBuilder.build()
            print(newTrigger)
        } catch (e: TriggerBuilder.Exception) {
            Log.w("createTrigger", "catched TriggerBuilder.Exception $e")
            Toast.makeText(
                context,
                "Cannot set message (${e.type})!",
                Toast.LENGTH_LONG
            ).show()

            showInfoDialog(
                context.getString(R.string.error_cannot_save_message),
                TriggerBuilder.getExceptionInfo(context, e)
            )
        }

        if (newTrigger == null) {
            Log.w("createTrigger", "trigger is null")
            return -1
        }

        if (!PermissionManager(context as Activity).request(
                Manifest.permission.SEND_SMS,
                context.resources.getString(R.string.permission_dialog_sms)
            )
        ) {
            Log.w("createTrigger", "no SEND_SMS permission")
            return -1
        }

        if (!PermissionManager(context as Activity).request(
                Manifest.permission.ACCESS_FINE_LOCATION,
                context.resources.getString(R.string.permission_dialog_location)
            )
        ) {
            Log.w("createTrigger", "no ACCESS_FINE_LOCATION permission")
            return -1
        }

        if (trigger == null) {
            Log.d("createTrigger", "adding new trigger $newTrigger")
            // we are not editing trigger
            return TriggerDbDAO(context).addTrigger(newTrigger)
        }

        Log.d("createTrigger", "updating trigger ${trigger!!.id} with $newTrigger")
        return TriggerDbDAO(context).updateTrigger(trigger!!.id, newTrigger)
    }

    private fun showInfoDialog(title: String, info: String) {
        val builder: AlertDialog.Builder = context.let { AlertDialog.Builder(it) }

        builder.setMessage(info)?.setTitle(title)

        val dialog: AlertDialog? = builder.create()
        dialog?.show()

        Log.d("TrCrPrs shwInfDlg", "permission=$title info=$info")
    }


    abstract fun onCreate(triggerCreationActivity: TriggerCreationActivity)
    abstract fun onResume()
    abstract fun onPause()

    class TimePresenter(context: Context, triggerId: Long? = null) :
        TriggerCreationPresenter(context, triggerId) {
        override fun onCreate(triggerCreationActivity: TriggerCreationActivity) {
            if (trigger != null) {
                triggerCreationActivity.fillTriggerData(trigger!!)
            }
        }

        override fun onResume() {

        }

        override fun onPause() {

        }

        fun getDate(
            dateValue: DateDialogSpinner.DateValue,
            timeValue: TimeDialogSpinner.TimeValue
        ): Calendar {

            val date = Calendar.getInstance()
            date.set(
                dateValue.year,
                dateValue.month,
                dateValue.dayOfMonth,
                timeValue.hour,
                timeValue.minute
            )
            return date
        }
    }

    class LocationPresenter(context: Context, triggerId: Long? = null) :
        TriggerCreationPresenter(context, triggerId) {
        internal lateinit var map: MapView
        internal lateinit var mapPinOverlay: MapPinOverlay
        internal lateinit var locationOverlay: MyLocationNewOverlay

        val lastSavedLocation: LocationDate? = TriggerDbDAO(context).getLastLocations().lastOrNull()

        init {
            Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        }

        override fun onCreate(triggerCreationActivity: TriggerCreationActivity) {
            if (trigger != null) {
                triggerCreationActivity.fillTriggerData(trigger!!)

                val td = trigger!!.data

                if (td is TriggerItemModel.LocationData) {
                    locationOverlay.disableFollowLocation()
                    Handler().postDelayed({
                        map.controller.zoomTo(16 - 0.0006 * td.radius)
                        goToLocation(td.coordinate)
                    }, 200)
                }
            } else {
                map.controller.setCenter(GeoPoint(51.11, 17.05))
                Handler().postDelayed({
                    map.controller.zoomTo(5.0)
                    goToMyLocation()
                }, 200)
            }

            PermissionManager(context as Activity).request(
                Manifest.permission.ACCESS_FINE_LOCATION, context.getString(
                    R.string.permission_dialog_location
                )
            )

            PermissionManager(context as Activity).request(
                Manifest.permission.READ_EXTERNAL_STORAGE, context.getString(
                    R.string.permission_dialog_map_storage
                )
            )

        }

        override fun onResume() {
            map.onResume()
        }

        override fun onPause() {
            map.onPause()
        }

        fun makeMap(map: MapView) {
            this.map = map
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            map.setBuiltInZoomControls(false)
            map.maxZoomLevel = 20.0
            map.minZoomLevel = 4.0

            run {
                locationOverlay = MyLocationNewOverlay(
                    org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider(context), map
                )
                locationOverlay.isOptionsMenuEnabled = true
                locationOverlay.enableMyLocation()

                if (trigger == null) {
                    locationOverlay.enableFollowLocation()

                    locationOverlay.runOnFirstFix {
                        (context as Activity).runOnUiThread {
                            map.controller.animateTo(locationOverlay.myLocation)
                            map.controller.zoomTo(16.0)
                            map.controller.setZoom(16.0)
                        }
                    }
                }

                map.overlays.add(locationOverlay)

                mapPinOverlay = MapPinOverlay(MapPinOverlay.MapPinEventsReceiver(map))
                map.overlays.add(mapPinOverlay)
            }
        }

        internal fun mapViewGoToAddressName(location: String, map: MapView?) {
            val addresses = findAddress(location)
            if (addresses.isEmpty()) return
            val address = addresses[0] ?: return
            if (map == null) return

            map.controller.setCenter(GeoPoint(address.latitude, address.longitude))
            map.controller.setZoom(14.0)
        }


        private fun findAddress(location: String, maxResults: Int = 1): List<Address?> {
            try {

                val geocoder = GeocoderNominatim("pl.zyper.autosms / 0.002 test")
                return geocoder.getFromLocationName(location, maxResults)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return listOf()
        }

        fun goToMyLocation() {
            PermissionManager(context as Activity).request(
                Manifest.permission.ACCESS_FINE_LOCATION, context.getString(
                    R.string.permission_dialog_location
                )
            )

            if (locationOverlay.myLocation != null) {
                goToLocation(locationOverlay.myLocation)
            } else if (lastSavedLocation != null) {
                goToLocation(lastSavedLocation.location.toGeoPoint()!!)
            }
            locationOverlay.enableFollowLocation()

            map.controller.setZoom(17.0)
        }

        fun goToLocation(loc: GeoPoint) {
            Log.d("LocationPresenter", "goToLocation $loc")

            map.controller.animateTo(loc)
        }

    }
}
