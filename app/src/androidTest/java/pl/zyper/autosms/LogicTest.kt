package pl.zyper.autosms

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class LogicTest {
    @get:Rule
    val activity: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    fun tryCatchBuild(builder: TriggerBuilder): TriggerItemModel? =
        try {
            builder.build()
        } catch (e: TriggerBuilder.Exception) {
            null
        }

    @Test
    fun testTimeBuilder() {
        val timeBuilder = TriggerBuilder(context)
        Assert.assertNull(tryCatchBuild(timeBuilder))

        timeBuilder.setContacts(listOf(ContactModel("a", "1", null)))
        Assert.assertNull(tryCatchBuild(timeBuilder))

        timeBuilder.setTextMessage("test")
        Assert.assertNull(tryCatchBuild(timeBuilder))

        timeBuilder.setDate(Calendar.getInstance().fromLong(1579468624)!!)
        Assert.assertNull(tryCatchBuild(timeBuilder))

        val future = Calendar.getInstance()
        future.add(Calendar.YEAR, 30)
        timeBuilder.setDate(future)

        Assert.assertNotNull(tryCatchBuild(timeBuilder))
    }

    @Test
    fun testLocationBuilder() {
        val locBuilder = TriggerBuilder(context)
        Assert.assertNull(tryCatchBuild(locBuilder))

        locBuilder.setContacts(listOf(ContactModel("a", "1", null)))
        Assert.assertNull(tryCatchBuild(locBuilder))

        locBuilder.setTextMessage("test")
        Assert.assertNull(tryCatchBuild(locBuilder))

        locBuilder.setRadius(1000.0)
        Assert.assertNull(tryCatchBuild(locBuilder))

        locBuilder.setEntryDirection(TriggerItemModel.LocationEntryDirection.Leaving)
        Assert.assertNull(tryCatchBuild(locBuilder))

        locBuilder.setCoordinate(GeoPoint(51.0, 32.0))

        Assert.assertNotNull(tryCatchBuild(locBuilder))
    }
}