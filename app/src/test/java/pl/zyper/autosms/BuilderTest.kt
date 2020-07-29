package pl.zyper.autosms

import org.junit.Assert
import org.junit.Test
import org.osmdroid.util.GeoPoint
import java.util.*


class BuilderTest {
    private fun getContacts(): List<ContactModel> {
        val contact = ContactModel("Jan Nowak", "123123123", null)
        return listOf(contact)
    }

    private fun getCorrectCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1)

        return cal
    }

    @Test
    fun dateTrigger_notNull() {
        val builder = TriggerBuilder()
        builder.setTextMessage("abcd")
        builder.setContacts(getContacts())
        builder.setDate(getCorrectCalendar())

        Assert.assertNotNull(builder.build())
    }

    @Test
    fun locationTrigger_notNull() {
        val builder = TriggerBuilder()
        builder.setTextMessage("abcd")
        builder.setContacts(getContacts())

        builder.setCoordinate(GeoPoint(1.0, 1.0))
        builder.setEntryDirection(TriggerItemModel.LocationEntryDirection.Entering)
        builder.setRadius(300.0)

        Assert.assertNotNull(builder.build())
    }

    @Test(expected = TriggerBuilder.Exception::class)
    fun noCoordinate_exception() {
        val builder = TriggerBuilder()
        builder.setTextMessage("abcd")
        builder.setContacts(getContacts())

        builder.setEntryDirection(TriggerItemModel.LocationEntryDirection.Entering)
        builder.setRadius(300.0)

        builder.build()
    }


    @Test(expected = TriggerBuilder.Exception::class)
    fun noEntryDirection_exception() {
        val builder = TriggerBuilder()
        builder.setTextMessage("abcd")
        builder.setContacts(getContacts())

        builder.setCoordinate(GeoPoint(1.0, 1.0))
        builder.setRadius(300.0)

        builder.build()
    }

    @Test(expected = TriggerBuilder.Exception::class)
    fun noRadius_exception() {
        val builder = TriggerBuilder()
        builder.setTextMessage("abcd")
        builder.setContacts(getContacts())

        builder.setCoordinate(GeoPoint(1.0, 1.0))
        builder.setEntryDirection(TriggerItemModel.LocationEntryDirection.Entering)

        builder.build()
    }

    @Test(expected = TriggerBuilder.Exception::class)
    fun noMessage_exception() {
        val builder = TriggerBuilder()
        builder.setContacts(getContacts())
        builder.setDate(getCorrectCalendar())

        builder.build()
    }

    @Test(expected = TriggerBuilder.Exception::class)
    fun datePast_exception() {
        val builder = TriggerBuilder()
        builder.setContacts(getContacts())
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, -10)
        builder.setDate(cal)
        builder.setTextMessage("abc")

        builder.build()
    }

    @Test(expected = TriggerBuilder.Exception::class)
    fun noDate_exception() {
        val builder = TriggerBuilder()
        builder.setContacts(getContacts())
        builder.setTextMessage("abc")

        builder.build()
    }

    @Test(expected = TriggerBuilder.Exception::class)
    fun noContacts_exception() {
        val builder = TriggerBuilder()
        builder.setDate(getCorrectCalendar())
        builder.setTextMessage("abc")

        builder.build()
    }

    @Test(expected = TriggerBuilder.Exception::class)
    fun emptyContacts_exception() {
        val builder = TriggerBuilder()
        builder.setDate(getCorrectCalendar())
        builder.setTextMessage("abc")
        builder.setContacts(listOf())

        builder.build()
    }
}