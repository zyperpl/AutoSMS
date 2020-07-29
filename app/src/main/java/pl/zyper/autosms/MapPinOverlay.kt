package pl.zyper.autosms

import android.graphics.Color
import android.widget.SeekBar
import android.widget.TextView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polygon
import java.text.MessageFormat
import kotlin.math.roundToLong
import kotlin.math.sqrt


class MapPinOverlay(
    val pinEventsReceiver: MapPinEventsReceiver
) :
    MapEventsOverlay(pinEventsReceiver),
    SeekBar.OnSeekBarChangeListener {

    private val minRadius = 30.0
    private val radiusStep = 5.0

    internal var radiusInfo: TextView? = null
        set(value) {
            field = value
            updateRadiusInfo()
        }

    init {
        pinEventsReceiver.radius = minRadius
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        pinEventsReceiver.radius = progressToRadius(seekBar.progress)

        updateRadiusInfo()

        pinEventsReceiver.update()
    }

    fun progressToRadius(progress: Int): Double {
        return minRadius + (((progress * progress).toDouble() * 1.034 / radiusStep).roundToLong() * radiusStep)
    }

    fun radiusToProgress(radius: Double): Int {
        return sqrt((radius - minRadius) / 1.034).toInt()
    }

    fun updateRadiusInfo() {
        (radiusInfo ?: return).text = MessageFormat.format(
            (radiusInfo ?: return).context.getString(R.string.radius_info_text),
            pinEventsReceiver.radius
        )

    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }

    fun getRadius(): Double {
        return pinEventsReceiver.radius
    }

    fun getPosition(): GeoPoint? {
        return pinEventsReceiver.point
    }

    class MapPinEventsReceiver(val map: MapView) : MapEventsReceiver {
        var radius: Double = 100.0
        private val polygon = Polygon(map)
        var point: GeoPoint? = null
            set(value) {
                field = value
                update()
            }

        init {
            polygon.fillColor = Color.argb(40, 0, 255, 120)
            polygon.strokeColor = Color.argb(200, 0, 255 / 2, 120 / 2)
            polygon.strokeWidth = 1.4f
            map.overlays.add(polygon)
        }

        override fun longPressHelper(p: GeoPoint?): Boolean {
            return true
        }

        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
            if (p != null) {
                point = p
            }
            return true
        }

        fun update() {
            if (point != null) {
                polygon.points = Polygon.pointsAsCircle(point, radius)
            }
            map.invalidate()
        }
    }
}