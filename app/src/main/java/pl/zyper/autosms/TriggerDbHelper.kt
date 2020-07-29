package pl.zyper.autosms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns


class TriggerDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        createAllTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db != null) {
            removeAllTables(db)
            onCreate(db)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun createAllTables() {
        createAllTables(writableDatabase)
    }

    private fun createAllTables(db: SQLiteDatabase) {
        db.execSQL(TriggersContract.SQL_CREATE_COORDINATE_TABLE)
        db.execSQL(TriggersContract.SQL_CREATE_TRIGGER_TABLE)
        db.execSQL(TriggersContract.SQL_CREATE_CONTACT_TABLE)
        db.execSQL(TriggersContract.SQL_CREATE_LOCATIONTR_ENTRY)
        db.execSQL(TriggersContract.SQL_CREATE_TIMETR_ENTRY)
    }

    fun removeAllTables() {
        removeAllTables(writableDatabase)
    }

    private fun removeAllTables(db: SQLiteDatabase) {
        db.execSQL(TriggersContract.SQL_DELETE_COORDINATE_TABLE)
        db.execSQL(TriggersContract.SQL_DELETE_TIMETR_TABLE)
        db.execSQL(TriggersContract.SQL_DELETE_LOCATIONTR_TABLE)
        db.execSQL(TriggersContract.SQL_DELETE_CONTACT_TABLE)
        db.execSQL(TriggersContract.SQL_DELETE_TRIGGER_TABLE)
    }

    companion object {
        const val DATABASE_NAME: String = "triggers_db"
        const val DATABASE_VERSION: Int = 5
    }
}

object TriggersContract {
    object TriggerEntry : BaseColumns {
        const val TABLE_NAME = "Trigger"
        const val COLUMN_SENT_DATE = "sent_date"
        const val COLUMN_MESSAGE = "message"
    }

    object ContactEntry : BaseColumns {
        const val TABLE_NAME = "Contact"
        const val COLUMN_NUMBER = "number"
        const val COLUMN_TRIGGER_ID = "trigger_id"
    }

    object LocationTriggerEntry : BaseColumns {
        const val TABLE_NAME = "LocationTrigger"
        const val COLUMN_LAT = "lat"
        const val COLUMN_LONG = "long"
        const val COLUMN_RADIUS = "radius"
        const val COLUMN_DIRECTION = "entry_direction"
        const val COLUMN_TRIGGER_ID = "trigger_id"
    }

    object TimeTriggerEntry : BaseColumns {
        const val TABLE_NAME = "TimeTrigger"
        const val COLUMN_DATE = "date"
        const val COLUMN_TRIGGER_ID = "trigger_id"
    }

    object CoordinateEntry : BaseColumns {
        const val TABLE_NAME = "Coordinate"
        const val COLUMN_DATE = "date"
        const val COLUMN_LAT = "lat"
        const val COLUMN_LONG = "long"
    }

    const val SQL_CREATE_COORDINATE_TABLE =
        "CREATE TABLE ${CoordinateEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY, " +
                "${CoordinateEntry.COLUMN_DATE} INTEGER, " +
                "${CoordinateEntry.COLUMN_LAT} REAL NOT NULL, " +
                "${CoordinateEntry.COLUMN_LONG} REAL NOT NULL " +
                ")"

    const val SQL_DELETE_COORDINATE_TABLE = "DROP TABLE IF EXISTS ${CoordinateEntry.TABLE_NAME}"

    const val SQL_CREATE_TRIGGER_TABLE =
        "CREATE TABLE ${TriggerEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY, " +
                "${TriggerEntry.COLUMN_SENT_DATE} INTEGER, " +
                "${TriggerEntry.COLUMN_MESSAGE} TEXT NOT NULL)"

    const val SQL_DELETE_TRIGGER_TABLE = "DROP TABLE IF EXISTS ${TriggerEntry.TABLE_NAME}"

    const val SQL_CREATE_CONTACT_TABLE =
        "CREATE TABLE ${ContactEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY, " +
                "${ContactEntry.COLUMN_NUMBER} TEXT NOT NULL, " +
                "${ContactEntry.COLUMN_TRIGGER_ID} INTEGER NOT NULL, " +
                "FOREIGN KEY (${ContactEntry.COLUMN_TRIGGER_ID}) REFERENCES ${TriggerEntry.TABLE_NAME}(${BaseColumns._ID}) " +
                "ON DELETE CASCADE)"

    const val SQL_DELETE_CONTACT_TABLE = "DROP TABLE IF EXISTS ${ContactEntry.TABLE_NAME}"

    const val SQL_CREATE_LOCATIONTR_ENTRY =
        "CREATE TABLE ${LocationTriggerEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY, " +
                "${LocationTriggerEntry.COLUMN_LAT} REAL NOT NULL, " +
                "${LocationTriggerEntry.COLUMN_LONG} REAL NOT NULL, " +
                "${LocationTriggerEntry.COLUMN_RADIUS} REAL NOT NULL, " +
                "${LocationTriggerEntry.COLUMN_DIRECTION} INTEGER NOT NULL, " +
                "${LocationTriggerEntry.COLUMN_TRIGGER_ID} INTEGER NOT NULL, " +
                "FOREIGN KEY (${LocationTriggerEntry.COLUMN_TRIGGER_ID}) REFERENCES ${TriggerEntry.TABLE_NAME}(${BaseColumns._ID}) " +
                "ON DELETE CASCADE)"

    const val SQL_DELETE_LOCATIONTR_TABLE =
        "DROP TABLE IF EXISTS ${LocationTriggerEntry.TABLE_NAME}"

    const val SQL_CREATE_TIMETR_ENTRY =
        "CREATE TABLE ${TimeTriggerEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY, " +
                "${TimeTriggerEntry.COLUMN_DATE} INTEGER NOT NULL, " +
                "${TimeTriggerEntry.COLUMN_TRIGGER_ID} INTEGER NOT NULL, " +
                "FOREIGN KEY (${TimeTriggerEntry.COLUMN_TRIGGER_ID}) REFERENCES ${TriggerEntry.TABLE_NAME}(${BaseColumns._ID}) " +
                "ON DELETE CASCADE)"

    const val SQL_DELETE_TIMETR_TABLE = "DROP TABLE IF EXISTS ${TimeTriggerEntry.TABLE_NAME}"

    const val SQL_SELECT_LOCATIONTR_TABLES = "SELECT * FROM ${LocationTriggerEntry.TABLE_NAME} " +
            "INNER JOIN ${TriggerEntry.TABLE_NAME} " +
            "ON ${LocationTriggerEntry.TABLE_NAME}.${LocationTriggerEntry.COLUMN_TRIGGER_ID} = " +
            "${TriggerEntry.TABLE_NAME}.${BaseColumns._ID}"

    const val SQL_SELECT_TIMETR_TABLES = "SELECT * FROM ${TimeTriggerEntry.TABLE_NAME} " +
            "INNER JOIN ${TriggerEntry.TABLE_NAME} " +
            "ON ${TimeTriggerEntry.TABLE_NAME}.${TimeTriggerEntry.COLUMN_TRIGGER_ID} = " +
            "${TriggerEntry.TABLE_NAME}.${BaseColumns._ID}"

    const val SQL_SELECT_ONE_CONTACTS_TABLES = "SELECT * FROM ${ContactEntry.TABLE_NAME} " +
            "INNER JOIN ${TriggerEntry.TABLE_NAME} " +
            "ON ${ContactEntry.TABLE_NAME}.${ContactEntry.COLUMN_TRIGGER_ID} = " +
            "${TriggerEntry.TABLE_NAME}.${BaseColumns._ID} " +
            "WHERE ${TriggerEntry.TABLE_NAME}.${BaseColumns._ID} = ?"

    const val SQL_SELECT_ONE_TRIGGER = "SELECT * FROM ${TriggerEntry.TABLE_NAME} " +
            "WHERE ${TriggerEntry.TABLE_NAME}.${BaseColumns._ID} = ? "

    const val SQL_SELECT_ONE_LOCATION_DATA = "SELECT * FROM ${LocationTriggerEntry.TABLE_NAME} " +
            "WHERE ${LocationTriggerEntry.COLUMN_TRIGGER_ID} = ?"

    const val SQL_SELECT_ONE_TIME_DATA = "SELECT * FROM ${TimeTriggerEntry.TABLE_NAME} " +
            "WHERE ${TimeTriggerEntry.COLUMN_TRIGGER_ID} = ?"

    const val SQL_SELECT_COORDINATES_TABLE = "SELECT * FROM ${CoordinateEntry.TABLE_NAME}"

    const val SQL_DELETE_OLD_COORDINATES = "DELETE FROM ${CoordinateEntry.TABLE_NAME} " +
            "WHERE ${BaseColumns._ID} NOT IN " +
            "(SELECT ${BaseColumns._ID} FROM ${CoordinateEntry.TABLE_NAME} ORDER BY ${BaseColumns._ID} DESC LIMIT 10)"
}
