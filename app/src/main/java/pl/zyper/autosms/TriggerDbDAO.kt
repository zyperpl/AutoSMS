package pl.zyper.autosms

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.provider.BaseColumns
import android.util.Log
import androidx.core.database.getLongOrNull
import org.osmdroid.util.GeoPoint
import java.util.*
import kotlin.collections.ArrayList

class TriggerDbDAO(val context: Context) {

    fun deleteTrigger(triggerId: Long): Boolean {
        if (triggerId < 0) return false

        var ret: Boolean

        val db = TriggerDbHelper(context).writableDatabase
        db.beginTransaction()

        try {
            if (!deleteTriggerInfo(db, triggerId)) {
                throw Exception("cannot delete trigger info")
            }
            if (!deleteTriggerData(db, triggerId)) {
                throw Exception("cannot delete trigger data")
            }
            if (!deleteTriggerContacts(db, triggerId)) {
                throw Exception("cannot delete trigger contacts")
            }

            db.setTransactionSuccessful()
            ret = true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("deleteTrigger", "delete trigger exception $e")
            ret = false
        }

        db.endTransaction()
        db.close()

        return ret
    }

    private fun deleteTriggerInfo(db: SQLiteDatabase, triggerId: Long): Boolean {
        val table = TriggersContract.TriggerEntry.TABLE_NAME
        val whereClause = "${BaseColumns._ID}=?"
        val whereArgs = arrayOf(triggerId.toString())
        return db.delete(table, whereClause, whereArgs) > 0
    }

    private fun deleteTriggerData(db: SQLiteDatabase, triggerId: Long): Boolean {
        var deleted = deleteLocationTrigger(db, triggerId)
        if (deleted <= 0) {
            deleted = deleteTimeTrigger(db, triggerId)
        }

        return deleted > 0
    }

    private fun deleteTriggerContacts(db: SQLiteDatabase, triggerId: Long): Boolean {
        val table = TriggersContract.ContactEntry.TABLE_NAME
        val whereClause = "${TriggersContract.ContactEntry.COLUMN_TRIGGER_ID}=?"
        val whereArgs = arrayOf(triggerId.toString())
        return db.delete(table, whereClause, whereArgs) > 0
    }

    private fun deleteTimeTrigger(db: SQLiteDatabase, triggerId: Long): Int {
        val table = TriggersContract.TimeTriggerEntry.TABLE_NAME
        val whereClause = "${TriggersContract.TimeTriggerEntry.COLUMN_TRIGGER_ID}=?"
        val whereArgs = arrayOf(triggerId.toString())
        return db.delete(table, whereClause, whereArgs)
    }

    private fun deleteLocationTrigger(db: SQLiteDatabase, triggerId: Long): Int {
        val table = TriggersContract.LocationTriggerEntry.TABLE_NAME
        val whereClause = "${TriggersContract.LocationTriggerEntry.COLUMN_TRIGGER_ID}=?"
        val whereArgs = arrayOf(triggerId.toString())
        return db.delete(table, whereClause, whereArgs)
    }

    fun getLastLocations(): List<LocationDate> {
        val list = mutableListOf<LocationDate>()

        val db = TriggerDbHelper(context).readableDatabase
        val query = TriggersContract.SQL_SELECT_COORDINATES_TABLE
        val cursor = db.rawQuery(query, null)

        with(cursor) {
            while (moveToNext()) {
                val epochTime =
                    getLong(getColumnIndexOrThrow(TriggersContract.CoordinateEntry.COLUMN_DATE))

                val lat = getDouble(getColumnIndex(TriggersContract.CoordinateEntry.COLUMN_LAT))
                val lon = getDouble(getColumnIndex(TriggersContract.CoordinateEntry.COLUMN_LONG))

                list.add(
                    LocationDate(
                        Location("dummy").fromCoordinate(lat, lon),
                        Calendar.getInstance().fromLong(epochTime)!!
                    )
                )
            }
        }

        cursor.close()
        db.close()

        return list
    }

    fun addLastLocation(ld: LocationDate): Long {
        val db = TriggerDbHelper(context).writableDatabase
        val values = ContentValues().apply {
            put(TriggersContract.CoordinateEntry.COLUMN_LAT, ld.location.latitude)
            put(TriggersContract.CoordinateEntry.COLUMN_LONG, ld.location.longitude)
            put(TriggersContract.CoordinateEntry.COLUMN_DATE, Long.fromCalendar(ld.date))
        }
        val id = db.insert(TriggersContract.CoordinateEntry.TABLE_NAME, null, values)

        val query = TriggersContract.SQL_DELETE_OLD_COORDINATES
        db.execSQL(query)

        db.close()
        return id
    }

    fun getTriggerInfo(triggerId: Long): TriggerItemModel? {
        val db = TriggerDbHelper(context).readableDatabase
        val query = TriggersContract.SQL_SELECT_ONE_TRIGGER
        val cursor = db.rawQuery(query, arrayOf(triggerId.toString()))

        var trigger: TriggerItemModel? = null

        try {
            with(cursor) {
                moveToFirst()

                val sentDate =
                    getLongOrNull(getColumnIndexOrThrow(TriggersContract.TriggerEntry.COLUMN_SENT_DATE))
                val message =
                    getString(getColumnIndexOrThrow(TriggersContract.TriggerEntry.COLUMN_MESSAGE))

                val contacts = getContacts(db, triggerId)
                trigger = TriggerItemModel(
                    message,
                    TriggerItemModel.TimeData(Calendar.getInstance().fromLong(0)!!),
                    contacts,
                    Calendar.getInstance().fromLong(sentDate)
                )

                trigger!!.id = triggerId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w("TriggerDbDAO", "Got exception: $e")
        }

        cursor.close()
        db.close()

        return trigger
    }

    fun getTrigger(triggerId: Long): TriggerItemModel? {
        try {
            val info = getTriggerInfo(triggerId) ?: return null

            val trigger = TriggerItemModel(
                info.message,
                getTriggerData(triggerId),
                info.contacts,
                info.sentDate
            )
            trigger.id = triggerId

            return trigger
        } catch (e: Exception) {
            return null
        }
    }

    private fun getTriggerData(triggerId: Long): TriggerItemModel.Data {
        val locationData = getTriggerLocationData(triggerId)

        if (locationData != null) return locationData

        return getTriggerTimeData(triggerId)!!
    }

    private fun getTriggerTimeData(triggerId: Long): TriggerItemModel.Data? {
        val query = TriggersContract.SQL_SELECT_ONE_TIME_DATA
        val db = TriggerDbHelper(context).readableDatabase
        val cursor = db.rawQuery(query, arrayOf(triggerId.toString()))

        var data: TriggerItemModel.TimeData? = null

        with(cursor) {
            while (moveToNext()) {
                val dateInt =
                    getLong(getColumnIndexOrThrow(TriggersContract.TimeTriggerEntry.COLUMN_DATE))

                data = TriggerItemModel.TimeData(Calendar.getInstance().fromLong(dateInt)!!)
            }
        }

        cursor.close()
        db.close()

        return data
    }

    private fun getTriggerLocationData(triggerId: Long): TriggerItemModel.Data? {
        val query = TriggersContract.SQL_SELECT_ONE_LOCATION_DATA
        val db = TriggerDbHelper(context).readableDatabase
        val cursor = db.rawQuery(query, arrayOf(triggerId.toString()))

        var data: TriggerItemModel.LocationData? = null

        with(cursor) {
            while (moveToNext()) {
                val lat =
                    getDouble(getColumnIndexOrThrow((TriggersContract.LocationTriggerEntry.COLUMN_LAT)))
                val long =
                    getDouble(getColumnIndexOrThrow((TriggersContract.LocationTriggerEntry.COLUMN_LONG)))
                val radius =
                    getDouble(getColumnIndexOrThrow((TriggersContract.LocationTriggerEntry.COLUMN_RADIUS)))

                val direction =
                    getInt(getColumnIndexOrThrow(((TriggersContract.LocationTriggerEntry.COLUMN_DIRECTION))))
                val triggerDirection = if (direction == 0) {
                    TriggerItemModel.LocationEntryDirection.Leaving
                } else {
                    TriggerItemModel.LocationEntryDirection.Entering
                }

                data = TriggerItemModel.LocationData(GeoPoint(lat, long), radius, triggerDirection)
            }
        }

        cursor.close()
        db.close()

        return data
    }

    fun addTrigger(trigger: TriggerItemModel): Long {
        val db = TriggerDbHelper(context).writableDatabase

        var triggerRowId: Long
        db.beginTransaction()

        try {
            triggerRowId = insertTrigger(db, trigger)
            if (triggerRowId < 0) {
                throw Exception("cannot insert trigger")
            }

            var dataRowId: Long = -1
            if (trigger.data is TriggerItemModel.TimeData) {
                dataRowId =
                    insertTimeTrigger(db, trigger.data, triggerRowId)
            } else if (trigger.data is TriggerItemModel.LocationData) {
                dataRowId =
                    insertLocationTrigger(db, trigger.data, triggerRowId)
            }

            if (dataRowId < 0) {
                throw Exception("cannot insert trigger.data ${trigger.data}")
            }

            for (contact in trigger.contacts) {
                val contactId = insertContact(db, contact.number, triggerRowId)
                if (contactId < 0) {
                    throw Exception("cannot insert trigger contact $contact")
                }
            }

            db.setTransactionSuccessful()
            Log.i("DB", "DB triggerRowID=$triggerRowId \t dataRowId=$dataRowId\n")
        } catch (e: Exception) {
            Log.e("addTrigger", e.toString())
            Log.i("addTrigger", "cannot add $trigger to db $db")
            triggerRowId = -1
        }
        db.endTransaction()
        db.close()

        return triggerRowId
    }

    fun updateTriggerInfo(id: Long, trigger: TriggerItemModel): Boolean {
        return updateTriggerInfo(id, trigger.message, trigger.sentDate)
    }

    fun updateTriggerInfo(id: Long, message: String, sentDate: Calendar?): Boolean {
        val db = TriggerDbHelper(context).writableDatabase

        val values = ContentValues()
        values.put(TriggersContract.TriggerEntry.COLUMN_MESSAGE, message)
        values.put(
            TriggersContract.TriggerEntry.COLUMN_SENT_DATE,
            Long.fromCalendar(sentDate)
        )

        Log.i("DB", "update trigger $id")

        val where = BaseColumns._ID + "=" + id.toString()
        val ret = db.update(TriggersContract.TriggerEntry.TABLE_NAME, values, where, null) > 0

        db.close()

        return ret
    }

    private fun insertContact(db: SQLiteDatabase, number: String, triggerRowId: Long): Long {
        val values = ContentValues().apply {
            put(TriggersContract.ContactEntry.COLUMN_NUMBER, number)
            put(TriggersContract.ContactEntry.COLUMN_TRIGGER_ID, triggerRowId)
        }
        return db.insert(TriggersContract.ContactEntry.TABLE_NAME, null, values)
    }

    private fun insertLocationTrigger(
        db: SQLiteDatabase,
        data: TriggerItemModel.LocationData,
        triggerRowId: Long
    ): Long {
        val lat: Double = data.coordinate.latitude
        val long: Double = data.coordinate.longitude
        val radius: Double = data.radius

        val values = ContentValues().apply {
            put(TriggersContract.LocationTriggerEntry.COLUMN_RADIUS, radius)
            put(TriggersContract.LocationTriggerEntry.COLUMN_LAT, lat)
            put(TriggersContract.LocationTriggerEntry.COLUMN_LONG, long)
            put(
                TriggersContract.LocationTriggerEntry.COLUMN_DIRECTION,
                (data.direction == TriggerItemModel.LocationEntryDirection.Entering)
            )
            put(TriggersContract.LocationTriggerEntry.COLUMN_TRIGGER_ID, triggerRowId)
        }
        return db.insert(TriggersContract.LocationTriggerEntry.TABLE_NAME, null, values)
    }

    private fun insertTimeTrigger(
        db: SQLiteDatabase,
        timeData: TriggerItemModel.TimeData,
        triggerRowId: Long
    ): Long {
        val timeFromEpoch: Long = timeData.date.timeInMillis / 1000

        val values = ContentValues().apply {
            put(TriggersContract.TimeTriggerEntry.COLUMN_DATE, timeFromEpoch)
            put(TriggersContract.TimeTriggerEntry.COLUMN_TRIGGER_ID, triggerRowId)
        }
        return db.insert(TriggersContract.TimeTriggerEntry.TABLE_NAME, null, values)
    }

    private fun insertTrigger(db: SQLiteDatabase, trigger: TriggerItemModel): Long {
        val values = ContentValues().apply {
            put(TriggersContract.TriggerEntry.COLUMN_MESSAGE, trigger.message)
            put(TriggersContract.TriggerEntry.COLUMN_SENT_DATE, Long.fromCalendar(trigger.sentDate))
        }
        return db.insert(TriggersContract.TriggerEntry.TABLE_NAME, null, values)
    }

    fun getAllTriggers(): ArrayList<TriggerItemModel> {
        val triggers: ArrayList<TriggerItemModel> = getTimeTriggers()
        triggers += getLocationTriggers()

        return triggers
    }

    fun getTimeTriggers(): ArrayList<TriggerItemModel> {
        val triggers = ArrayList<TriggerItemModel>()
        val db = TriggerDbHelper(context).readableDatabase
        val query = TriggersContract.SQL_SELECT_TIMETR_TABLES
        val cursor = db.rawQuery(query, null)

        with(cursor) {
            while (moveToNext()) {
                val rowId =
                    getLong(getColumnIndexOrThrow(TriggersContract.TimeTriggerEntry.COLUMN_TRIGGER_ID))

                val sentDate =
                    getLongOrNull(getColumnIndexOrThrow(TriggersContract.TriggerEntry.COLUMN_SENT_DATE))
                val message =
                    getString(getColumnIndexOrThrow(TriggersContract.TriggerEntry.COLUMN_MESSAGE))

                val epochTime =
                    getLong(getColumnIndexOrThrow((TriggersContract.TimeTriggerEntry.COLUMN_DATE)))

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = epochTime * 1000

                val data = TriggerItemModel.TimeData(calendar)

                val contacts = getContacts(db, rowId)
                val tr = TriggerItemModel(
                    message,
                    data,
                    contacts,
                    Calendar.getInstance().fromLong(sentDate)
                )

                tr.id = rowId

                triggers.add(tr)
            }
        }

        cursor.close()
        db.close()

        return triggers
    }

    private fun getContacts(db: SQLiteDatabase, rowId: Long): List<ContactModel> {
        val contacts = mutableListOf<ContactModel>()
        val query = TriggersContract.SQL_SELECT_ONE_CONTACTS_TABLES
        val cursor = db.rawQuery(query, arrayOf(rowId.toString()))

        with(cursor) {
            while (moveToNext()) {
                val number =
                    getString(getColumnIndexOrThrow(TriggersContract.ContactEntry.COLUMN_NUMBER))
                val contact = ContactModel("", number, null)

                contacts.add(contact)
            }
        }

        cursor.close()

        return contacts
    }

    fun getLocationTriggers(): ArrayList<TriggerItemModel> {
        val triggers = ArrayList<TriggerItemModel>()
        val db = TriggerDbHelper(context).readableDatabase
        val query = TriggersContract.SQL_SELECT_LOCATIONTR_TABLES
        val cursor = db.rawQuery(query, null)

        with(cursor) {
            while (moveToNext()) {
                val rowId =
                    getLong(getColumnIndexOrThrow(TriggersContract.LocationTriggerEntry.COLUMN_TRIGGER_ID))

                val sentDate =
                    getLongOrNull(getColumnIndexOrThrow(TriggersContract.TriggerEntry.COLUMN_SENT_DATE))
                val message =
                    getString(getColumnIndexOrThrow(TriggersContract.TriggerEntry.COLUMN_MESSAGE))

                val lat =
                    getDouble(getColumnIndexOrThrow((TriggersContract.LocationTriggerEntry.COLUMN_LAT)))
                val long =
                    getDouble(getColumnIndexOrThrow((TriggersContract.LocationTriggerEntry.COLUMN_LONG)))
                val radius =
                    getDouble(getColumnIndexOrThrow((TriggersContract.LocationTriggerEntry.COLUMN_RADIUS)))

                val direction =
                    getInt(getColumnIndexOrThrow(((TriggersContract.LocationTriggerEntry.COLUMN_DIRECTION))))
                val triggerDirection = if (direction == 0) {
                    TriggerItemModel.LocationEntryDirection.Leaving
                } else {
                    TriggerItemModel.LocationEntryDirection.Entering
                }

                val data =
                    TriggerItemModel.LocationData(GeoPoint(lat, long), radius, triggerDirection)

                val contacts = getContacts(db, rowId)
                val tr = TriggerItemModel(
                    message,
                    data,
                    contacts,
                    Calendar.getInstance().fromLong(sentDate)
                )

                tr.id = rowId

                triggers.add(tr)
            }
        }

        cursor.close()
        db.close()

        return triggers
    }

    fun updateTrigger(id: Long, trigger: TriggerItemModel): Long {
        var returnId = id

        val db = TriggerDbHelper(context).writableDatabase
        db.beginTransaction()
        try {
            val infoValues = ContentValues()
            infoValues.put(TriggersContract.TriggerEntry.COLUMN_MESSAGE, trigger.message)
            infoValues.put(
                TriggersContract.TriggerEntry.COLUMN_SENT_DATE,
                Long.fromCalendar(trigger.sentDate)
            )

            val d = trigger.data
            val dataValues = ContentValues()
            if (d is TriggerItemModel.TimeData) {
                dataValues.apply {
                    put(TriggersContract.TimeTriggerEntry.COLUMN_DATE, Long.fromCalendar(d.date))
                }
            } else
                if (d is TriggerItemModel.LocationData) {
                    dataValues.apply {
                        put(TriggersContract.LocationTriggerEntry.COLUMN_RADIUS, d.radius)
                        put(TriggersContract.LocationTriggerEntry.COLUMN_LAT, d.coordinate.latitude)
                        put(
                            TriggersContract.LocationTriggerEntry.COLUMN_LONG,
                            d.coordinate.longitude
                        )
                        put(
                            TriggersContract.LocationTriggerEntry.COLUMN_DIRECTION,
                            (d.direction == TriggerItemModel.LocationEntryDirection.Entering)
                        )
                    }
                }


            Log.i("DB", "update trigger $id")

            var ret = db.update(
                TriggersContract.TriggerEntry.TABLE_NAME,
                infoValues,
                BaseColumns._ID + "=" + id.toString(),
                null
            ) > 0
            if (!ret) {
                throw Exception("cannot update trigger $id")
            }

            if (trigger.data is TriggerItemModel.TimeData) {
                ret = db.update(
                    TriggersContract.TimeTriggerEntry.TABLE_NAME,
                    dataValues,
                    TriggersContract.TimeTriggerEntry.COLUMN_TRIGGER_ID + "=" + id.toString(),
                    null
                ) > 0 && ret
            } else if (trigger.data is TriggerItemModel.LocationData) {
                ret = db.update(
                    TriggersContract.LocationTriggerEntry.TABLE_NAME,
                    dataValues,
                    TriggersContract.LocationTriggerEntry.COLUMN_TRIGGER_ID + "=" + id.toString(),
                    null
                ) > 0 && ret
            }
            if (!ret) {
                throw Exception("cannot update data ${trigger.data}")
            }

            db.setTransactionSuccessful()
            Log.i("DB", "update data trigger $id")
        } catch (e: Exception) {
            Log.e("updateTrigger", e.toString())
            Log.w("updateTrigger", "cannot update trigger $id to $trigger db=$db")
            returnId = -1
        }

        db.endTransaction()
        db.close()

        return returnId
    }
}