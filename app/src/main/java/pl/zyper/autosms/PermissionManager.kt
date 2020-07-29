package pl.zyper.autosms

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.absoluteValue

class PermissionManager(val activity: Activity) {

    fun request(permission: String, dialogMessage: String? = null): Boolean {
        if (ContextCompat.checkSelfPermission(activity, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("PermissionManager", "Permission $permission not granted (activity=$activity)!")

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            ) {
                Log.w(
                    "PermissionManager",
                    "Should show request for $permission (activity=$activity)!"
                )

                showDialog(
                    permission,
                    dialogMessage
                        ?: activity.resources.getString(R.string.permission_dialog_message)
                )
            } else {
                Log.w(
                    "PermissionManager",
                    "Permission $permission requesting... (activity=$activity)!"
                )

                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permission),
                    permission.hashCode().absoluteValue
                )
            }
        } else {
            Log.i(
                "PermissionManager",
                "Permission $permission already granted (activity=$activity)"
            )
            return true
        }

        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showDialog(permission: String, dialogMessage: String) {
        val builder: AlertDialog.Builder? = activity.let { AlertDialog.Builder(it) }

        builder?.setMessage(dialogMessage)?.setTitle(R.string.permission_dialog_title)

        val dialog: AlertDialog? = builder?.create()
        dialog?.show()

        Log.d(
            "PermissionManager",
            "permission=$permission hashCode=${permission.hashCode()} dialogMessage=$dialogMessage"
        )
        dialog?.setOnDismissListener {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                permission.hashCode().absoluteValue
            )
        }
    }
}
