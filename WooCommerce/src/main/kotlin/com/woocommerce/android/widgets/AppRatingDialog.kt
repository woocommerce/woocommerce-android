/**
 * Adapted and converted to Kotlin from https://github.com/kobakei/Android-RateThisApp
 */
package com.woocommerce.android.widgets

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AlertDialog
import com.woocommerce.android.R
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

    private var installDate = Date()
    private var askLaterDate = Date()
    private var launchTimes = 0
    private var optOut = false
    private var config = Config()

    // Weak ref to avoid leaking the context
    private var dialogRef: WeakReference<AlertDialog>? = null

    /**
     * Initialize RateThisApp configuration.
     * @param config Configuration object.
     */
    fun init(config: Config) {
        this.config = config
    }

    /**
     * Call this API when the launcher activity is launched.<br></br>
     * It is better to call this API in onCreate() of the launcher activity.
     * @param context Context
     */
    fun onCreate(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()

        // If it is the first launch, save the date in shared preference.
        if (pref.getLong(KEY_INSTALL_DATE, 0) == 0L) {
            storeInstallDate(context, editor)
        }

        // Increment launch times
        var launchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0)
        launchTimes++

        editor.putInt(KEY_LAUNCH_TIMES, launchTimes)
        editor.apply()

        installDate = Date(pref.getLong(KEY_INSTALL_DATE, 0))
        this.launchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0)
        optOut = pref.getBoolean(KEY_OPT_OUT, false)
        askLaterDate = Date(pref.getLong(KEY_ASK_LATER_DATE, 0))
    }

    /**
     * Show the rate dialog if the criteria is satisfied.
     * @param context Context
     * @param themeId Theme ID
     * @return true if shown, false otherwise.
     */
    fun showRateDialogIfNeeded(context: Context, themeId: Int): Boolean {
        return if (shouldShowRateDialog()) {
            val builder = AlertDialog.Builder(context, themeId)
            showRateDialog(context, builder)
            true
        } else {
            false
        }
    }

    /**
     * Check whether the rate dialog should be shown or not.
     * Developers may call this method directly if they want to show their own view instead of
     * dialog provided by this library.
     * @return
     */
    private fun shouldShowRateDialog(): Boolean {
        if (optOut) {
            return false
        } else {
            if (launchTimes >= config.criteriaLaunchTimes) {
                return true
            }
            val thresholdMs = TimeUnit.DAYS.toMillis(config.criteriaInstallDays.toLong())
            return Date().time - installDate.time >= thresholdMs && Date().time - askLaterDate.time >= thresholdMs
        }
    }

    private fun showRateDialog(context: Context, builder: AlertDialog.Builder) {
        if (dialogRef != null && dialogRef!!.get() != null) {
            // Dialog is already present
            return
        }

        builder.setTitle(R.string.app_rating_title)
                .setMessage(R.string.app_rating_message)
                .setCancelable(true)
                .setPositiveButton(R.string.app_rating_rate_now) { dialog, which ->
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

                    setOptOut(context, true)
                }
                .setNeutralButton(R.string.app_rating_rate_later) { dialog, which ->
                    clearSharedPreferences(context)
                    storeAskLaterDate(context)
                }
                .setNegativeButton(R.string.app_rating_rate_never) { dialog, which ->
                    setOptOut(context, true)
                }
                .setOnCancelListener {
                    clearSharedPreferences(context)
                    storeAskLaterDate(context)
                }
                .setOnDismissListener { dialogRef!!.clear() }
        dialogRef = WeakReference(builder.show())
    }

    /**
     * Clear data in shared preferences.<br></br>
     * This API is called when the "Later" is pressed or canceled.
     * @param context
     */
    private fun clearSharedPreferences(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.remove(KEY_INSTALL_DATE)
        editor.remove(KEY_LAUNCH_TIMES)
        editor.apply()
    }

    /**
     * Set opt out flag.
     * If it is true, the rate dialog will never shown unless app data is cleared.
     * This method is called when Yes or No is pressed.
     * @param context
     * @param optOut
     */
    private fun setOptOut(context: Context, optOut: Boolean) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putBoolean(KEY_OPT_OUT, optOut)
        editor.apply()
        this.optOut = optOut
    }

    /**
     * Store install date.
     * Install date is retrieved from package manager if possible.
     * @param context
     * @param editor
     */
    private fun storeInstallDate(context: Context, editor: SharedPreferences.Editor) {
        var installDate = Date()
        val packMan = context.packageManager
        try {
            val pkgInfo = packMan.getPackageInfo(context.packageName, 0)
            installDate = Date(pkgInfo.firstInstallTime)
        } catch (e: PackageManager.NameNotFoundException) {
            WooLog.e(T.UTILS, e)
        }
        editor.putLong(KEY_INSTALL_DATE, installDate.time)
    }

    /**
     * Store the date the user asked for being asked again later.
     * @param context
     */
    private fun storeAskLaterDate(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putLong(KEY_ASK_LATER_DATE, System.currentTimeMillis())
        editor.apply()
    }

    /**
     * Configuration.
     */
    class Config
    /**
     * Constructor.
     * @param criteriaInstallDays
     * @param criteriaLaunchTimes
     */
    @JvmOverloads constructor(val criteriaInstallDays: Int = 7, val criteriaLaunchTimes: Int = 10) {
        //
    }
}
