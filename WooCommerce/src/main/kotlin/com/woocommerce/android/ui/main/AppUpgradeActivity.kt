package com.woocommerce.android.ui.main

import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

abstract class AppUpgradeActivity : AppCompatActivity(),
        AppUpgradeActivityView,
        InstallStateUpdatedListener {
    private lateinit var appUpdateManager: AppUpdateManager

    /**
     * The type of in app update to display:
     * [AppUpdateType.FLEXIBLE] OR [AppUpdateType.IMMEDIATE]
     */
    private val inAppUpdateType = BuildConfig.IN_APP_UPDATE_TYPE.toInt()

    /**
     * The latest app version code that is available for download
     */
    private var appUpdateVersionCode: Int? = null

    /**
     * Listener that is passed to the calling activity, if the update has failed for some reason.
     * If the user clicks on the Retry button, the update process will be tried again.
     */
    private val updateFailedActionListener: View.OnClickListener = View.OnClickListener {
        checkForAppUpdates()
    }

    /**
     * Listener that is passed to the calling activity, if the update has succeeded.
     * If the user clicks on the Restart button, the install process will begin and the app
     * will be restarted.
     */
    private val updateSuccessActionListener: View.OnClickListener = View.OnClickListener {
        appUpdateManager.completeUpdate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initialise appUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this)
    }

    override fun onResume() {
        super.onResume()
        handleAppUpdateOnResumed()
    }

    override fun onStart() {
        super.onStart()
        appUpdateManager.registerListener(this)
    }

    override fun onStop() {
        super.onStop()
        appUpdateManager.unregisterListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.IN_APP_UPDATE) {
            when (resultCode) {
                //  handle user's rejection
                Activity.RESULT_CANCELED -> {
                    // Store the current available app version code to note that the user has cancelled this update.
                    // This will ensure that the update dialog is not displayed again for this version
                    appUpdateVersionCode?.let { AppPrefs.setCancelledAppVersionCode(it) }
                }
                //  handle update failure
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> showAppUpdateFailedSnack(updateFailedActionListener)
            }
            return
        }
    }

    override fun onStateUpdate(installState: InstallState) {
        // Show module progress, log state, or install the update.
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                // After the update is downloaded, show a notification
                // and request user confirmation to restart the app.
                handleFlexibleUpdateSuccess()
            }
            InstallStatus.FAILED -> {
                // App update failed for some reason. This could happen due to network
                // issues as well. Display an error snack and ask users to try again
                appUpdateManager.unregisterListener(this)
                showAppUpdateFailedSnack(updateFailedActionListener)
            }
        }
    }

    /**
     * Method is called from the child activity to check if there are any app updates pending.
     * This will display either [AppUpdateType.FLEXIBLE] or [AppUpdateType.IMMEDIATE] dialog to the user,
     * if there is a new app update.
     *
     * The reason this is called from the child activity and not from this activity, is to provide control
     * to the calling activity when to display the update dialog.
     *
     * If an update is available and supported, and only if the user has not cancelled the update for this current,
     * the update dialog will be displayed
     */
    internal fun checkForAppUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    // Checks that the platform will allow the specified type of update. This will always return false
                    // if there is no internet
                    if (appUpdateInfo.isUpdateTypeAllowed(inAppUpdateType)) {
                        // Display the upgrade dialog only if the user has not cancelled the upgrade for this version.
                        // This is checked by getting the current available app version code and checking
                        // if this version > that the stored app version code.
                        val availableAppVersionCode = appUpdateInfo.availableVersionCode()
                        val storedAppVersionCode = AppPrefs.getCancelledAppVersionCode()
                        if (availableAppVersionCode > storedAppVersionCode) {
                            if (isAppUpdateImmediate()) {
                                // initiate immediate update flow
                                requestAppUpdate(appUpdateInfo)
                            } else {
                                // Before starting an update, register a listener for updates.
                                // initiate flexible update flow
                                requestAppUpdate(appUpdateInfo)
                            }
                        }
                        appUpdateVersionCode = availableAppVersionCode
                    }
                }
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    WooLog.v(WooLog.T.UTILS, "App update not available")
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // If the app download initiated by the user is in progress and it is a fexible install,
                    // and if the download is completed, we need to inform the user to manually restart the app,
                    // in order to install the update. This is only true for FLEXIBLE updates so we need to check
                    // if the AppUpdateType is FLEXIBLE before proceeding
                    if (isAppUpdateFlexible() && appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            handleFlexibleUpdateSuccess()
                        }
                    }
                }
            }
    }

    /**
     * Method is called from the [onResume] of the activity to check if the update process is
     * started and if so, verify that the UI is updated accordingly
     */
    private fun handleAppUpdateOnResumed() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    // A flexible update means that the user is able to use the app while the update is
                    // being downloaded. if the user goes back or kills the app, or gets a call etc, and the app goes
                    // into the background, it won’t stop the update process. But once the app comes back to the
                    // foreground, if the update is already downloaded, we need to ask the user to manually restart the
                    // app.
                    if (isAppUpdateFlexible() && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            handleFlexibleUpdateSuccess()
                        }
                    }
                }

                // The integer UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS means that an immediate update
                // has been started, and update is still in progress. Triggering the request flow using update info’s
                // intent will ask Google Play to show that blocking, immediate app update screen.
                // Post the update, Play will automatically restart the app.
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // After calling appUpdateManager.startUpdateFlowForResult() for an immediate update
                    // the play store takes control. The user will see the update progress till the time they are on the
                    // new version. And if the user goes back or kills the app, or gets a call etc, and the app goes
                    // into the background, it won’t stop the update process. This should be communicated to the user
                    // the moment the app gets back to the foreground.
                    if (isAppUpdateImmediate() && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        requestAppUpdate(appUpdateInfo)
                    }
                }
            }
        }
    }

    private fun isAppUpdateImmediate() = AppUpdateType.IMMEDIATE == inAppUpdateType
    private fun isAppUpdateFlexible() = AppUpdateType.FLEXIBLE == inAppUpdateType

    private fun requestAppUpdate(appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                inAppUpdateType,
                this,
                RequestCodes.IN_APP_UPDATE
            )
        } catch (e: SendIntentException) {
            // It looks like there are cases when the in app update gets cancelled without any cause for the
            // exception. But this is causing the app to crash when using this feature.
            // See [Activity.startIntentSenderForResultInner] and #2458 for more details
            WooLog.e(T.DEVICE, "In app update has been cancelled")
        }
    }

    private fun handleFlexibleUpdateSuccess() {
        appUpdateManager.unregisterListener(this)
        showAppUpdateSuccessSnack(updateSuccessActionListener)
    }
}
