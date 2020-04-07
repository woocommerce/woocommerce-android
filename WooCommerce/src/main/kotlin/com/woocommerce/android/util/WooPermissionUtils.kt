package com.woocommerce.android.util

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import java.util.HashMap

object WooPermissionUtils {
    /**
     * called by the onRequestPermissionsResult() of various activities and fragments - tracks
     * the permission results, remembers that the permissions have been asked for, and optionally
     * shows a dialog enabling the user to edit permissions if any are always denied
     *
     * @param activity host activity
     * @param requestCode request code passed to ContextCompat.checkSelfPermission
     * @param permissions list of permissions
     * @param grantResults list of results for above permissions
     * @param checkForAlwaysDenied show dialog if any permissions always denied
     * @return true if all permissions granted
     */
    fun setPermissionListAsked(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        checkForAlwaysDenied: Boolean
    ): Boolean {
        for (i in permissions.indices) {
            AppPrefs.getPermissionAskedKey(permissions[i])?.let { key ->
                val isFirstTime = !AppPrefs.exists(key)
                trackPermissionResult(requestCode, permissions[i], grantResults[i], isFirstTime)
                AppPrefs.setBoolean(key, true)
            }
        }

        var allGranted = true
        for (i in grantResults.indices) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                allGranted = false
                if (checkForAlwaysDenied && !ActivityCompat
                                .shouldShowRequestPermissionRationale(activity, permissions[i])) {
                    showPermissionAlwaysDeniedDialog(activity, permissions[i])
                    break
                }
            }
        }

        return allGranted
    }

    /*
     * returns true if we know the app has asked for the passed permission
     */
    private fun isPermissionAsked(context: Context, permission: String): Boolean {
        val key = AppPrefs.getPermissionAskedKey(permission) ?: return false

        // if the key exists, we've already stored whether this permission has been asked for
        if (AppPrefs.exists(key)) {
            return AppPrefs.getBoolean(key, false)
        }

        // otherwise, check whether permission has already been granted - if so we know it has
        // been asked
        if (ContextCompat.checkSelfPermission(
                        context,
                        permission
                ) == PackageManager.PERMISSION_GRANTED) {
            AppPrefs.setBoolean(key, true)
            return true
        }

        return false
    }

    /*
     * returns true if the passed permission has been denied AND the user checked "never show again"
     * in the native permission dialog
     */
    fun isPermissionAlwaysDenied(activity: Activity, permission: String): Boolean {
        // shouldShowRequestPermissionRationale returns false if the permission has been permanently
        // denied, but it also returns false if the app has never requested that permission - so we
        // check it only if we know we've asked for this permission
        if (isPermissionAsked(activity, permission) && ContextCompat.checkSelfPermission(
                        activity,
                        permission
                ) == PackageManager.PERMISSION_DENIED) {
            val shouldShow = ActivityCompat
                    .shouldShowRequestPermissionRationale(activity, permission)
            return !shouldShow
        }

        return false
    }

    private fun trackPermissionResult(
        requestCode: Int,
        permission: String,
        result: Int,
        isFirstTime: Boolean
    ) {
        val props = HashMap<String, String>()
        props["permission"] = permission
        props["request_code"] = requestCode.toString()
        props["is_first_time"] = java.lang.Boolean.toString(isFirstTime)

        if (result == PackageManager.PERMISSION_GRANTED) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_PERMISSION_GRANTED, props)
        } else if (result == PackageManager.PERMISSION_DENIED) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_PERMISSION_DENIED, props)
        }
    }

    /*
     * returns the name to display for a permission, ex: "permission.CAMERA" > "Camera"
     */
    private fun getPermissionName(context: Context, permission: String): String {
        return when (permission) {
            android.Manifest.permission.CAMERA ->
                context.getString(R.string.permission_camera)
            else -> {
                WooLog.w(WooLog.T.UTILS, "No name for requested permission")
                context.getString(R.string.unknown)
            }
        }
    }

    /*
     * called when the app detects that the user has permanently denied a permission, shows a dialog
     * alerting them to this fact and enabling them to visit the app settings to edit permissions
     */
    private fun showPermissionAlwaysDeniedDialog(
        activity: Activity,
        permission: String
    ) {
        val message = String.format(
                activity.getString(R.string.permissions_denied_message),
                getPermissionName(activity, permission)
        )

        val builder = MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.permissions_denied_title))
                .setMessage(HtmlCompat.fromHtml(message, FROM_HTML_MODE_LEGACY))
                .setPositiveButton(
                        R.string.button_edit_permissions
                ) { _, _ -> showAppSettings(activity) }
                .setNegativeButton(R.string.button_not_now, null)
        builder.show()
    }

    /*
     * open the device's settings page for this app so the user can edit permissions
     */
    private fun showAppSettings(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
                context, permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
