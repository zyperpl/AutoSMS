package pl.zyper.autosms

import android.content.Context
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.RootMatchers
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
class TriggerEditTest {

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
    fun openEditTimeTrigger() {
        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
        createTimeTrigger()
        onView(withId(R.id.trigger_list)).perform(swipeDown())

        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(MESSAGE_TEXT)),
                longClick()
            )
        )
        onView(withText(R.string.menu_edit)).perform(click())

        onView(withId(R.id.button_next)).check(matches(isDisplayed()))
    }

    @Test
    fun openEditLocationTrigger() {
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

        onView(withId(R.id.trigger_message)).check(matches(isDisplayed()))
    }

    @Test
    fun tryEditTimeTriggerNoMessage() {
        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
        createTimeTrigger()
        onView(withId(R.id.trigger_list)).perform(swipeDown())

        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(MESSAGE_TEXT)),
                longClick()
            )
        )
        onView(withText(R.string.menu_edit)).perform(click())

        onView(withId(R.id.button_next)).check(matches(isDisplayed()))

        onView(withId(R.id.trigger_message)).perform(ViewActions.replaceText(""))
        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun tryEditTimeTriggerDatePast() {
        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
        createTimeTrigger()
        onView(withId(R.id.trigger_list)).perform(swipeDown())


        onView(withText(MESSAGE_TEXT)).perform(click())

        onView(withId(R.id.button_next)).check(matches(isDisplayed()))

        setDatePicker(1990, 26, 6)
        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun editTimeTrigger() {
        deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
        createTimeTrigger()
        onView(withId(R.id.trigger_list)).perform(swipeDown())

        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(MESSAGE_TEXT)),
                longClick()
            )
        )
        onView(withText(R.string.menu_edit)).perform(click())

        onView(withId(R.id.button_next)).check(matches(isDisplayed()))

        setDatePicker(2120, 1, 19)
        setTimePicker(13, 37)

        onView(withId(R.id.button_next)).perform(click())
        onView(withText(TriggerCreateTest.MESSAGE_TEXT)).check(matches(isDisplayed()))

        TriggerEditTest.deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
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

        TriggerEditTest.deleteAllFromDb(TriggerCreateTest.MESSAGE_TEXT)
    }


}