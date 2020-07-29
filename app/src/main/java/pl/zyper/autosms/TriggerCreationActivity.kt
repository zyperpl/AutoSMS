package pl.zyper.autosms

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView


class TriggerCreationActivity : Activity() {
    private lateinit var presenter: TriggerCreationPresenter
    internal lateinit var contactsSearch: SearchView
    internal lateinit var contactsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        var layoutId = (intent.extras ?: return).getInt(INTENT_EXTRA_LAYOUT)
        var triggerId: Long? = intent.extras?.getLong(INTENT_EXTRA_TRIGGER)

        Log.i("TriggerCreationActivity", "layoutId=$layoutId; triggerId=$triggerId")

        setContentView(layoutId)

        when (layoutId) {
            R.layout.trigger_creation_location -> {
                presenter = TriggerCreationPresenter.LocationPresenter(this, triggerId)
                prepareTriggerLocationCreation()
                presenter.onCreate(this)
            }
            R.layout.trigger_creation_time -> {
                presenter = TriggerCreationPresenter.TimePresenter(this, triggerId)
                presenter.onCreate(this)
            }
        }

        contactsSearch = findViewById(R.id.contacts_search)
        contactsList = findViewById(R.id.contacts_list)

        Handler().postDelayed({
            populateContactList()
        }, 10)

        val buttonNext = findViewById<Button>(R.id.button_next)

        buttonNext?.setOnClickListener {
            buttonNext.isEnabled = false
            addTriggerToDatabase()
            Thread.sleep(100)
            buttonNext.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) {
            return
        }

        when (requestCode) {
            Manifest.permission.READ_CONTACTS.hashCode() -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.fillContactsAdapter()
                    populateContactList()
                }
            }

            else -> {

            }
        }
    }

    fun fillTriggerData(trigger: TriggerItemModel) {
        if (presenter is TriggerCreationPresenter.TimePresenter && trigger.data is TriggerItemModel.TimeData) {
            findViewById<DateDialogSpinner>(R.id.date_spinner).setFromCalendar(trigger.data.date)
            findViewById<TimeDialogSpinner>(R.id.time_spinner).setFromCalendar(trigger.data.date)
        } else if (presenter is TriggerCreationPresenter.LocationPresenter && trigger.data is TriggerItemModel.LocationData) {
            findViewById<SeekBar>(R.id.trigger_radius).progress =
                (presenter as TriggerCreationPresenter.LocationPresenter).mapPinOverlay.radiusToProgress(
                    trigger.data.radius
                )

            (presenter as TriggerCreationPresenter.LocationPresenter).mapPinOverlay.pinEventsReceiver.point =
                trigger.data.coordinate

            findViewById<ToggleButton>(R.id.trigger_direction).isChecked =
                trigger.data.direction == TriggerItemModel.LocationEntryDirection.Entering
        }

        presenter.contactsAdapter.selectItems(trigger.contacts)
        findViewById<EditText>(R.id.trigger_message).setText(trigger.message)
    }

    private fun populateContactList() {
        val viewManager = LinearLayoutManager(this)

        contactsList.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = presenter.contactsAdapter
        }

        prepareContactsSearchBar()
    }

    private fun prepareContactsSearchBar() {
        contactsSearch.queryHint = resources.getString(R.string.hint_contact_phone)
        contactsSearch.setOnQueryTextListener(presenter.contactsQueryListener)
        contactsSearch.setOnClickListener { contactsSearch.isIconified = false }
        contactsSearch.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
    }

    private fun addTriggerToDatabase() {
        val dateDialogSpinner = findViewById<DateDialogSpinner>(R.id.date_spinner)
        val timeDialogSpinner = findViewById<TimeDialogSpinner>(R.id.time_spinner)

        if (presenter is TriggerCreationPresenter.TimePresenter) {
            // date trigger
            val dateValue = dateDialogSpinner.value
            val timeValue = timeDialogSpinner.value

            val date =
                (presenter as TriggerCreationPresenter.TimePresenter).getDate(dateValue, timeValue)
            Log.i("addTriggerToDb", "date=$date")

            presenter.triggerBuilder.setDate(date)

        } else if (presenter is TriggerCreationPresenter.LocationPresenter) {
            val locationPresenter = presenter as TriggerCreationPresenter.LocationPresenter
            val radius = locationPresenter.mapPinOverlay.getRadius()
            val geoPoint = locationPresenter.mapPinOverlay.getPosition()

            val triggerDirection = findViewById<ToggleButton>(R.id.trigger_direction).isChecked

            val triggerDataDirection = if (triggerDirection) {
                TriggerItemModel.LocationEntryDirection.Entering
            } else {
                TriggerItemModel.LocationEntryDirection.Leaving
            }

            if (geoPoint != null) {
                presenter.triggerBuilder.setCoordinate(geoPoint)
            }
            presenter.triggerBuilder.setRadius(radius)
            presenter.triggerBuilder.setEntryDirection(triggerDataDirection)

        }

        // get selected contacts
        val selectedContacts = presenter.contactsAdapter.getSelectedItems()
        Log.d(
            "addTriggerToDatabase",
            "selected : ${presenter.contactsAdapter.selectedPositions.joinToString()}"
        )
        presenter.triggerBuilder.setContacts(selectedContacts)

        // get text message
        val triggerMessage = findViewById<TextView>(R.id.trigger_message).text
        if (triggerMessage != null) {
            presenter.triggerBuilder.setTextMessage(triggerMessage)
        }

        val triggerId = presenter.createTrigger()
        if (triggerId >= 0) {
            val intent = Intent()
            intent.putExtra("TriggerID", triggerId)
            setResult(RESULT_OK, intent)
            finish()
            return
        }
    }

    private fun prepareTriggerLocationCreation() {
        val locationPresenter = presenter as TriggerCreationPresenter.LocationPresenter

        val map = findViewById<MapView>(R.id.map)
        locationPresenter.makeMap(map)

        val locationButton = findViewById<ImageButton>(R.id.my_location_button)
        locationButton.setOnClickListener {
            locationPresenter.goToMyLocation()
        }

        val radiusBar = findViewById<SeekBar>(R.id.trigger_radius)
        val radiusInfo = findViewById<TextView>(R.id.radius_info)

        locationPresenter.mapPinOverlay.radiusInfo = radiusInfo
        radiusBar.setOnSeekBarChangeListener(locationPresenter.mapPinOverlay)
    }


    override fun onPause() {
        super.onPause()

        presenter.onPause()
    }

    override fun onResume() {
        super.onResume()

        presenter.onResume()
    }

    companion object {
        const val CREATE_TRIGGER_REQUEST: Int = 1
        const val EDIT_TRIGGER_REQUEST: Int = 3
        const val INTENT_EXTRA_LAYOUT = "Layout"
        const val INTENT_EXTRA_TRIGGER = "Trigger"

        fun newIntent(context: Context, layoutId: Int, editTriggerId: Long): Intent {
            val intent = Intent(context, TriggerCreationActivity::class.java)
            intent.putExtra(INTENT_EXTRA_LAYOUT, layoutId)
            intent.putExtra(INTENT_EXTRA_TRIGGER, editTriggerId)
            return intent
        }
    }
}