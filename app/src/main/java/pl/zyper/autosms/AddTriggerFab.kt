package pl.zyper.autosms

import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AddTriggerFab(val context: Context) {
    private val animationRotate45c: Animation =
        AnimationUtils.loadAnimation(context, R.anim.rotate_45_c)
    private val animationRotate45cw: Animation =
        AnimationUtils.loadAnimation(context, R.anim.rotate_45_cw)

    val buttons: ArrayList<FloatingActionButton> = ArrayList()


    fun setFab(fab: FloatingActionButton?) {
        fab?.setOnClickListener { switchFab(fab) }
    }

    fun switchFab(fab: FloatingActionButton, newState: Boolean? = null) {
        if (newState != null) {
            isOpen = !newState
        }

        if (isOpen) { // closing
            fab.startAnimation(animationRotate45cw)
            buttons.forEach { b -> b.hide() }
        } else { // opening
            fab.startAnimation(animationRotate45c)
            buttons.forEach { b -> b.show() }
        }

        isOpen = !isOpen
    }

    fun setButton(name: String, otherFab: FloatingActionButton, function: (Context) -> Unit) {
        otherFab.setOnClickListener { function(context) }

        buttons.add(otherFab)
    }

    var isOpen = false
}