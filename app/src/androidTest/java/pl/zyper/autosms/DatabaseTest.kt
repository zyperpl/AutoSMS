package pl.zyper.autosms

import android.content.Context
import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class DatabaseTest {
    companion object {
        const val MESSAGE_TEXT: String = "_ANDROIDTEST_ESPRESSO"
        const val PHONE_NUMBER: String = "0000000"
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        @BeforeClass
        @JvmStatic
        fun init() {
            System.err.println("TriggerCreateTest: init")
            deleteAllFromDb(MESSAGE_TEXT)
        }

        @AfterClass
        @JvmStatic
        fun end() {
            System.err.println("TriggerCreateTest: end")
            deleteAllFromDb(MESSAGE_TEXT)
        }

        private fun deleteAllFromDb(messageText: String) {
            val triggers =
                TriggerDbDAO(context).getAllTriggers().filter { it.message == messageText }
            triggers.forEach {
                TriggerDbDAO(context).deleteTrigger(it.id)
            }
        }

    }

    @Test
    fun removeCreateAllTables() {
        TriggerDbHelper(context).removeAllTables()
        TriggerDbHelper(context).createAllTables()
    }

    @Test
    fun addTimeTrigger() {
        val tr = TriggerItemModel(
            TriggerOperationTest.MESSAGE_TEXT,
            TriggerItemModel.TimeData(Calendar.getInstance()),
            listOf(
                ContactModel(
                    TriggerOperationTest.PHONE_NUMBER, TriggerOperationTest.PHONE_NUMBER, null
                )
            )
        )
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        yesterday.add(Calendar.MINUTE, 5)
        tr.sentDate = yesterday

        Assert.assertTrue(TriggerDbDAO(context).addTrigger(tr) >= 0)
    }

    @Test
    fun addLocationTrigger() {
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
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        yesterday.add(Calendar.MINUTE, 5)
        tr.sentDate = yesterday

        Assert.assertTrue(TriggerDbDAO(context).addTrigger(tr) >= 0)
    }

    @Test
    fun getAllTriggers() {
        addTimeTrigger()
        Assert.assertTrue(TriggerDbDAO(context).getAllTriggers().isNotEmpty())
        deleteAllFromDb(MESSAGE_TEXT)
    }

    @Test
    fun getAllTimeTriggers() {
        addTimeTrigger()
        Assert.assertTrue(TriggerDbDAO(context).getTimeTriggers().isNotEmpty())
        Assert.assertTrue(TriggerDbDAO(context).getAllTriggers().isNotEmpty())
        deleteAllFromDb(MESSAGE_TEXT)
    }

    @Test
    fun getAllLocationTriggers() {
        addLocationTrigger()
        Assert.assertTrue(TriggerDbDAO(context).getTimeTriggers().isEmpty())
        Assert.assertTrue(TriggerDbDAO(context).getLocationTriggers().isNotEmpty())
        Assert.assertTrue(TriggerDbDAO(context).getAllTriggers().isNotEmpty())
        deleteAllFromDb(MESSAGE_TEXT)
    }

    @Test
    fun editTrigger() {
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
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        yesterday.add(Calendar.MINUTE, 5)
        tr.sentDate = yesterday

        val trId = TriggerDbDAO(context).addTrigger(tr)
        Assert.assertTrue(trId >= 0)

        Assert.assertTrue(TriggerDbDAO(context).updateTrigger(trId, tr) >= 0)
        Assert.assertTrue(TriggerDbDAO(context).updateTriggerInfo(trId, tr))
    }

    @Test
    fun removeLocationTrigger() {
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
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        yesterday.add(Calendar.MINUTE, 5)
        tr.sentDate = yesterday

        val trId = TriggerDbDAO(context).addTrigger(tr)
        Assert.assertTrue(trId >= 0)

        Assert.assertTrue(TriggerDbDAO(context).deleteTrigger(trId))
    }

    @Test
    fun removeTimeTrigger() {
        val tr = TriggerItemModel(
            TriggerOperationTest.MESSAGE_TEXT,
            TriggerItemModel.TimeData(Calendar.getInstance()),
            listOf(
                ContactModel(
                    TriggerOperationTest.PHONE_NUMBER, TriggerOperationTest.PHONE_NUMBER, null
                )
            )
        )
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        yesterday.add(Calendar.MINUTE, 5)
        tr.sentDate = yesterday

        val trId = TriggerDbDAO(context).addTrigger(tr)
        Assert.assertTrue(trId >= 0)

        Assert.assertTrue(TriggerDbDAO(context).deleteTrigger(trId))
    }

    @Test
    fun addLastLocation() {
        val lId = TriggerDbDAO(context).addLastLocation(
            LocationDate(
                Location("dummy").fromCoordinate(
                    0.0,
                    0.0
                ), Calendar.getInstance()
            )
        )

        Assert.assertTrue(lId >= 0)
    }

    @Test
    fun getLastLocations() {
        val lId = TriggerDbDAO(context).addLastLocation(
            LocationDate(
                Location("dummy").fromCoordinate(
                    0.0,
                    0.0
                ), Calendar.getInstance()
            )
        )

        Assert.assertTrue(lId >= 0)

        val list = TriggerDbDAO(context).getLastLocations()

        Assert.assertTrue(list.isNotEmpty())
        Assert.assertTrue(list[0].location.latitude < 0.1)
    }
}