package pl.zyper.autosms

import org.junit.Assert.*
import org.junit.Test
import org.osmdroid.util.GeoPoint
import java.util.*

class UtilsTest {
    @Test
    fun geoPointInRadius_true() {
        val g1 = GeoPoint(30.0, 20.0001)
        val g2 = GeoPoint(30.0, 20.00001)
        val r = 10000.0

        assertTrue(g1.isInRadius(g2, r))
    }

    @Test
    fun geoPointInRadius_false() {
        val g1 = GeoPoint(40.0, 22.0)
        val g2 = GeoPoint(30.0, 0.00001)
        val r = 10000.0

        assertFalse(g1.isInRadius(g2, r))
    }

    @Test
    fun geoPointInRadiusNoRadius_false() {
        val g1 = GeoPoint(40.0, 22.0)
        val g2 = GeoPoint(30.0, 0.00001)
        val r = 0.0

        assertFalse(g1.isInRadius(g2, r))
    }

    @Test
    fun locationFromCoordinate_notNull() {
        //val loc = Location("dummy").fromCoordinate(10.0, 20.0)
        //assertNotNull(loc)
    }

    @Test
    fun locationFromCoordinate_correct() {
        //val loc = Location("dummy").fromCoordinate(10.0, 20.0)

        //assert(loc.latitude == 10.0 && loc.longitude == 20.0)
    }

    @Test
    fun calendarFromLong_notNull() {
        val cal = Calendar.getInstance(Locale.ENGLISH).fromLong(10)
        assertNotNull(cal)
    }

    @Test
    fun calendarFromLong_correct() {
        val cal = Calendar.getInstance(Locale.ENGLISH).fromLong(1579256556)
        val d = cal?.get(Calendar.DAY_OF_MONTH)
        val m = cal?.get(Calendar.MONTH)
        val y = cal?.get(Calendar.YEAR)
        val h = cal?.get(Calendar.HOUR)
        val min = cal?.get(Calendar.MINUTE)
        val s = cal?.get(Calendar.SECOND)

        assert(d == 17 && m == 0 && y == 2020 && h == 11 && min == 22 && s == 36)
    }
}
