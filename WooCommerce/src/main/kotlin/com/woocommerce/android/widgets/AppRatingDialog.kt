/**
 * Loosely based on https://github.com/kobakei/Android-RateThisApp but highly modified and simplified for our use
 */
package com.woocommerce.android.widgets

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.APP_FEEDBACK_RATE_APP
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_DECLINED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_LATER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_RATED
import com.woocommerce.android.extensions.packageInfo
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import java.lang.ref.WeakReference
import java.util.Date
import java.util.concurrent.TimeUnit

object AppRatingDialog {
    private const val PREF_NAME = "rate_woo"
    private const val KEY_INSTALL_DATE = "rate_install_date"
    private const val KEY_LAUNCH_TIMES = "rate_launch_times"
    private const val KEY_OPT_OUT = "rate_opt_out"
    private const val KEY_ASK_LATER_DATE = "rate_ask_later_date"
    private const val KEY_INTERACTIONS = "rate_interactions"

    // app must have been installed this long before the rating dialog will appear
    private const val CRITERIA_INSTALL_DAYS: Int = 7
    // app must have been launched this many times before the rating dialog will appear
    private const val CRITERIA_LAUNCH_TIMES: Int = 10
    // user must have performed this many interactions before the rating dialog will appear
    private const val CRITERIA_INTERACTIONS: Int = 10

    private var installDate = Date()
    private var askLaterDate = Date()
    private var launchTimes = 0
    private var interactions = 0
    private var optOut = false

    private lateinit var preferences: SharedPreferences

    // Weak ref to avoid leaking the context
    private var dialogRef: WeakReference<AlertDialog>? = null

    /**
     * Call this when the launcher activity is launched.
     */
    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()

        // If it is the first launch, save the date in shared preference.
        if (preferences.getLong(KEY_INSTALL_DATE, 0) == 0L) {
            storeInstallDate(context)
        }

        // Increment launch times
        launchTimes = preferences.getInt(KEY_LAUNCH_TIMES, 0)
        launchTimes++
        editor.putInt(KEY_LAUNCH_TIMES, launchTimes)
        editor.apply()

        interactions = preferences.getInt(KEY_INTERACTIONS, 0)
        optOut = preferences.getBoolean(KEY_OPT_OUT, false)
        installDate = Date(preferences.getLong(KEY_INSTALL_DATE, 0))
        askLaterDate = Date(preferences.getLong(KEY_ASK_LATER_DATE, 0))
    }

    /**
     * Show the rate dialog if the criteria is satisfied.
     * @return true if shown, false otherwise.
     */
    fun showIfNeeded(context: Context): Boolean {
        return if (shouldShowRateDialog()) {
            showRateDialog(context)
            true
        } else {
            false
        }
    }

    /**
     * Called from various places in the app where the user has performed a non-trivial action, such as fulfilling
     * an order. We use this to avoid showing the rating dialog to uninvolved users
     */
    fun incrementInteractions() {
        if (!optOut) {
            interactions++
            preferences.edit().putInt(KEY_INTERACTIONS, interactions)?.apply()
        }
    }

    /**
     * Check whether the rate dialog should be shown or not.
     * @return true if the dialog should be shown
     */
    private fun shouldShowRateDialog(): Boolean {
        return if (optOut or (launchTimes < CRITERIA_LAUNCH_TIMES) or (interactions < CRITERIA_INTERACTIONS)) {
            false
        } else {
            val thresholdMs = TimeUnit.DAYS.toMillis(CRITERIA_INSTALL_DAYS.toLong())
            Date().time - installDate.time >= thresholdMs && Date().time - askLaterDate.time >= thresholdMs
        }
    }

    fun showRateDialog(
        context: Context,
        ratingAccepted: () -> Unit = {},
        ratingPostponed: () -> Unit = {},
        ratingDeclined: () -> Unit = {}
    ) {
        dialogRef?.get()?.let {
            // Dialog is already present
            return
        }

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.app_rating_title)
            .setMessage(R.string.app_rating_message)
            .setCancelable(true)
            .setPositiveButton(R.string.app_rating_rate_now) { _, _ ->
                AnalyticsTracker.track(APP_FEEDBACK_RATE_APP, mapOf(KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_RATED))
                ratingAccepted()
                val appPackage = context.packageName
                val url: String? = "market://details?id=$appPackage"
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: android.content.ActivityNotFoundException) {
                    // play store app isn't on this device so open app's page in browser instead
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + context.packageName)
                        )
                    )
                }

                setOptOut(true)
            }
            .setNeutralButton(R.string.app_rating_rate_later) { _, _ ->
                AnalyticsTracker.track(APP_FEEDBACK_RATE_APP, mapOf(KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_LATER))
                ratingPostponed()
                clearSharedPreferences()
                storeAskLaterDate()
            }
            .setNegativeButton(R.string.app_rating_rate_never) { _, _ ->
                AnalyticsTracker.track(APP_FEEDBACK_RATE_APP, mapOf(KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_DECLINED))
                ratingDeclined()
                setOptOut(true)
            }
            .setOnCancelListener {
                clearSharedPreferences()
                storeAskLaterDate()
            }
            .setOnDismissListener { dialogRef?.clear() }
        dialogRef = WeakReference(builder.show())
    }

    /**
     * Clear data other than opt-out in shared preferences - called when the "Later" is pressed or dialog is canceled.
     */
    private fun clearSharedPreferences() {
        preferences.edit().remove(KEY_INSTALL_DATE)?.remove(KEY_LAUNCH_TIMES)?.remove(KEY_INTERACTIONS)?.apply()
    }

    /**
     * Set opt out flag - when true, the rate dialog will never be shown unless app data is cleared.
     */
    private fun setOptOut(optOut: Boolean) {
        preferences.edit().putBoolean(KEY_OPT_OUT, optOut)?.apply()
        this.optOut = optOut
    }

    /**
     * Store install date - retrieved from package manager if possible.
     */
    private fun storeInstallDate(context: Context) {
        var installDate = Date()
        val packMan = context.packageManager
        try {
            val pkgInfo = packMan.packageInfo(context.packageName, 0)
            installDate = Date(pkgInfo.firstInstallTime)
        } catch (e: PackageManager.NameNotFoundException) {
            WooLog.e(T.UTILS, e)
        }
        preferences.edit().putLong(KEY_INSTALL_DATE, installDate.time)?.apply()
    }

    /**
     * Store the date the user asked for being asked again later.
     */
    private fun storeAskLaterDate() {
        val nextAskDate = System.currentTimeMillis()
        preferences.edit().putLong(KEY_ASK_LATER_DATE, nextAskDate)?.apply()
    }
}
