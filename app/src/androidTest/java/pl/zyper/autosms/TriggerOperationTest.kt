package pl.zyper.autosms

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.containsString
import org.junit.*
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class TriggerOperationTest {

    companion object {
        const val MESSAGE_TEXT: String = "_ANDROIDTEST_ESPRESSO"
        const val PHONE_NUMBER: String = "0000000"
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext


        @BeforeClass
        @JvmStatic
        fun init() {
            System.err.println("TriggerCreateTest: init")
            deleteAllFromDb(MESSAGE_TEXT)
            deleteTriggerFromDb(MESSAGE_TEXT, PHONE_NUMBER)
        }

        @AfterClass
        @JvmStatic
        fun end() {
            System.err.println("TriggerCreateTest: end")
            deleteAllFromDb(MESSAGE_TEXT)
            deleteTriggerFromDb(MESSAGE_TEXT, PHONE_NUMBER)
        }

        private fun deleteAllFromDb(messageText: String) {
            val triggers =
                TriggerDbDAO(context).getAllTriggers().filter { it.message == messageText }
            triggers.forEach {
                TriggerDbDAO(context).deleteTrigger(it.id)
            }
        }

        private fun deleteTriggerFromDb(messageText: String, phoneNumber: String) {
            val trigger = TriggerDbDAO(context).getAllTriggers()
                .find { t -> t.message == messageText && t.contacts.any { c -> c.number == phoneNumber } }

            if (trigger != null) {
                TriggerDbDAO(context).deleteTrigger(trigger.id)
            }
        }
    }

    @get:Rule
    val activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    fun deleteTrigger(title: String) {
        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(title)),
                longClick()
            )
        )
        onView(withText(R.string.menu_remove)).perform(click())
    }

    private fun setDatePicker(y: Int, m: Int, d: Int) {
        onView(withId(R.id.date_spinner)).perform(click())
        onView(withClassName(containsString("DatePicker"))).perform(PickerActions.setDate(y, m, d))
        onView(withText(android.R.string.ok)).perform(click())
    }

    private fun setTimePicker(h: Int, m: Int) {
        onView(withId(R.id.time_spinner)).perform(click())
        onView(withResourceName(containsString("timePicker"))).perform(PickerActions.setTime(h, m))
        onView(withText(android.R.string.ok)).perform(click())
    }

    fun createLocationTrigger(): TriggerItemModel {
        val tr = TriggerItemModel(
            MESSAGE_TEXT,
            TriggerItemModel.LocationData(
                GeoPoint(51.654, 32.123),
                200.0,
                TriggerItemModel.LocationEntryDirection.Entering
            ),
            listOf(
                ContactModel(
                    PHONE_NUMBER, PHONE_NUMBER, null
                )
            )
        )

        assert(TriggerDbDAO(context).addTrigger(tr) >= 0)
        return tr
    }

    fun getFutureCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, 30)
        return cal
    }

    fun createTimeTrigger(): TriggerItemModel {
        val tr = TriggerItemModel(
            MESSAGE_TEXT,
            TriggerItemModel.TimeData(getFutureCalendar()),
            listOf(
                ContactModel(
                    PHONE_NUMBER, PHONE_NUMBER, null
                )
            )
        )

        assert(TriggerDbDAO(context).addTrigger(tr) >= 0)
        return tr
    }

    @Test
    fun deleteLocationTrigger() {
        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
        createLocationTrigger()
        onView(withId(R.id.trigger_list)).perform(swipeDown())

        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(MESSAGE_TEXT)),
                longClick()
            )
        )
        onView(withText(R.string.menu_remove)).perform(click())
    }

    @Test
    fun deleteTimeTrigger() {
        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
        createTimeTrigger()
        onView(withId(R.id.trigger_list)).perform(swipeDown())

        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(MESSAGE_TEXT)),
                longClick()
            )
        )
        onView(withText(R.string.menu_remove)).perform(click())
    }

    @Test
    fun addTriggerLocation() {
        onView(withId(R.id.button_next)).check(ViewAssertions.doesNotExist())

        onView(withId(R.id.add_fab)).perform(click())
        onView(withId(R.id.gps_fab)).perform(click())

        onView(withId(R.id.button_next)).check(matches(isDisplayed()))
        onView(withId(R.id.trigger_message)).perform(typeText(TriggerCreateTest.MESSAGE_TEXT))
        onView(withId(R.id.my_location_button)).perform(click())

        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.trigger_radius)).perform(swipeRight())

        onView(withId(R.id.contacts_list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<ContactsAdapter.ViewHolder>(0, click())
        )

        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).check(ViewAssertions.doesNotExist())
    }

    @Test
    fun addTenLocationTriggers() {
        repeat(10) {
            addTriggerLocation()
        }
        val triggerAdapter =
            activityRule.activity.findViewById<RecyclerView>(R.id.trigger_list).adapter as TriggerListAdapter

        Assert.assertEquals(triggerAdapter.list.size, 10)

        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
    }

    @Test
    fun editLocationTrigger() {
        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
        createLocationTrigger()
        onView(withId(R.id.trigger_list)).perform(swipeDown())

        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(MESSAGE_TEXT)),
                longClick()
            )
        )
        onView(withText(R.string.menu_edit)).perform(click())

        onView(withId(R.id.button_next)).check(matches(isDisplayed()))

        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.my_location_button)).perform(click())

        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.trigger_radius)).perform(ViewActions.swipeRight())

        onView(withId(R.id.contacts_list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<ContactsAdapter.ViewHolder>(0, click())
        )

        onView(withId(R.id.button_next)).perform(click())
        onView(withText(TriggerCreateTest.MESSAGE_TEXT)).check(matches(isDisplayed()))

        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
    }

    fun createTimeTriggerSent(): Long {
        val tr = TriggerItemModel(
            MESSAGE_TEXT,
            TriggerItemModel.TimeData(getFutureCalendar()),
            listOf(
                ContactModel(
                    PHONE_NUMBER, PHONE_NUMBER, null
                )
            )
        )
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        yesterday.add(Calendar.MINUTE, 5)
        tr.sentDate = yesterday

        val tId = TriggerDbDAO(context).addTrigger(tr)
        assert(tId >= 0)
        return tId
    }

    @Test
    fun restartTimeTrigger() {
        val tId = createTimeTriggerSent()

        onView(withId(R.id.trigger_list)).perform(swipeDown())
        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(MESSAGE_TEXT)),
                longClick()
            )
        )
        onView(withText(R.string.menu_restart)).perform(click())

        val restartedTrigger = TriggerDbDAO(context).getTrigger(tId)!!
        Assert.assertNull(restartedTrigger.sentDate)
        Assert.assertTrue((restartedTrigger.data as TriggerItemModel.TimeData).date > Calendar.getInstance())

        println(restartedTrigger.toString())

        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
    }


}