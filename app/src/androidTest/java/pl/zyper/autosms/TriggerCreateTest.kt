package pl.zyper.autosms

import android.content.Context
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.containsString
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class TriggerCreateTest {

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


    @Test
    fun isAddFabVisible() {
        onView(withId(R.id.add_fab)).check(matches(isDisplayed()))
    }

    @Test
    fun isTimeFabVisible() {
        onView(withId(R.id.add_fab)).perform(click())
        onView(withId(R.id.time_fab)).check(matches(isDisplayed()))
    }

    @Test
    fun isFabClosed() {
        onView(withId(R.id.add_fab)).perform(click())
        onView(withId(R.id.add_fab)).perform(click())
    }

    @Test
    fun isLocationFabVisible() {
        onView(withId(R.id.add_fab)).perform(click())
        onView(withId(R.id.gps_fab)).check(matches(isDisplayed()))
    }

    @Test
    fun openTriggerLocationCreationActivity() {
        onView(withId(R.id.button_next)).check(doesNotExist())

        onView(withId(R.id.add_fab)).perform(click())
        onView(withId(R.id.gps_fab)).perform(click())

        onView(withId(R.id.button_next)).check(matches(isDisplayed()))
    }

    @Test
    fun openTriggerTimeCreationActivity() {
        onView(withId(R.id.button_next)).check(doesNotExist())

        onView(withId(R.id.add_fab)).perform(click())
        onView(withId(R.id.time_fab)).perform(click())

        onView(withId(R.id.button_next)).check(matches(isDisplayed()))
    }

    @Test
    fun tryTriggerLocationNoMessage() {
        openTriggerLocationCreationActivity()
        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun tryTriggerLocationNoCoordinate() {
        openTriggerLocationCreationActivity()
        onView(withId(R.id.trigger_message)).perform(typeText(MESSAGE_TEXT))
        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun tryTriggerLocationNoContacts() {
        openTriggerLocationCreationActivity()
        onView(withId(R.id.trigger_message)).perform(typeText(MESSAGE_TEXT))
        onView(withId(R.id.my_location_button)).perform(click())

        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.trigger_radius)).perform(swipeRight())

        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun addTriggerLocation() {
        openTriggerLocationCreationActivity()
        onView(withId(R.id.trigger_message)).perform(typeText(MESSAGE_TEXT))
        onView(withId(R.id.my_location_button)).perform(click())

        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.trigger_radius)).perform(swipeRight())

        onView(withId(R.id.contacts_list)).perform(
            actionOnItemAtPosition<ContactsAdapter.ViewHolder>(0, click())
        )

        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).check(doesNotExist())

        onView(withText(MESSAGE_TEXT)).check(matches(isDisplayed()))

        deleteAllFromDb(MESSAGE_TEXT)
    }

    @Test
    fun addTriggerLocationCustomPhone() {
        openTriggerLocationCreationActivity()
        onView(withId(R.id.trigger_message)).perform(typeText(MESSAGE_TEXT))
        onView(withId(R.id.my_location_button)).perform(click())

        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.trigger_radius)).perform(swipeRight())

        onView(withId(R.id.contacts_search)).perform(typeText(PHONE_NUMBER))

        onView(withId(R.id.contacts_list)).perform(
            actionOnItemAtPosition<ContactsAdapter.ViewHolder>(0, click())
        )

        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).check(doesNotExist())

        onView(withText(MESSAGE_TEXT)).check(matches(isDisplayed()))

        deleteAllFromDb(MESSAGE_TEXT)
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

    @Test
    fun tryTriggerDatePast() {
        openTriggerTimeCreationActivity()
        onView(withId(R.id.trigger_message)).perform(typeText(MESSAGE_TEXT))

        onView(withId(R.id.contacts_list)).perform(
            actionOnItemAtPosition<ContactsAdapter.ViewHolder>(0, click())
        )

        setDatePicker(1990, 26, 6)

        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).inRoot(isDialog())
            .check(matches(isDisplayed()))
    }


    @Test
    fun addTriggerDate() {
        openTriggerTimeCreationActivity()
        onView(withId(R.id.trigger_message)).perform(typeText(MESSAGE_TEXT))

        onView(withId(R.id.contacts_list)).perform(
            actionOnItemAtPosition<ContactsAdapter.ViewHolder>(0, click())
        )

        // just to be sure it doesn't trigger
        setDatePicker(2120, 26, 6)
        setTimePicker(21, 36)

        onView(withId(R.id.button_next)).perform(click())

        onView(withText(R.string.error_cannot_save_message)).check(doesNotExist())

        onView(withText(MESSAGE_TEXT)).check(matches(isDisplayed()))

        deleteAllFromDb(MESSAGE_TEXT)
    }

    @Test
    fun addTriggerDateCustomPhone() {
        openTriggerTimeCreationActivity()
        onView(withId(R.id.trigger_message)).perform(typeText(MESSAGE_TEXT))

        onView(withId(R.id.contacts_search)).perform(typeText(PHONE_NUMBER))

        onView(withId(R.id.contacts_list)).perform(
            actionOnItemAtPosition<ContactsAdapter.ViewHolder>(0, click())
        )

        setDatePicker(2120, 26, 6)
        setTimePicker(21, 36)

        onView(withId(R.id.button_next)).perform(click())

        onView(withText(MESSAGE_TEXT)).check(matches(isDisplayed()))

        deleteAllFromDb(MESSAGE_TEXT)
    }


    @Test
    fun deleteTimeTrigger() {
        openTriggerTimeCreationActivity()
        onView(withId(R.id.trigger_message)).perform(typeText(MESSAGE_TEXT))

        onView(withId(R.id.contacts_search)).perform(typeText(PHONE_NUMBER))

        onView(withId(R.id.contacts_list)).perform(
            actionOnItemAtPosition<ContactsAdapter.ViewHolder>(0, click())
        )

        setDatePicker(2120, 26, 6)
        setTimePicker(21, 36)

        onView(withId(R.id.button_next)).perform(click())

        onView(withText(MESSAGE_TEXT)).check(matches(isDisplayed()))

        onView(withId(R.id.trigger_list)).perform(
            actionOnItem<TriggerListAdapter.ViewHolder>(
                hasDescendant(withText(MESSAGE_TEXT)),
                longClick()
            )
        )
        onView(withText(R.string.menu_remove)).perform(click())
    }
}