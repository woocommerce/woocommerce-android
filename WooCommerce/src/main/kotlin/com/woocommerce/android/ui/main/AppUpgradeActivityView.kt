package com.woocommerce.android.ui.main

import android.view.View
import com.google.android.play.core.install.model.AppUpdateType

interface AppUpgradeActivityView {
    /**
     * Method to display a snackBar once the [AppUpdateType.FLEXIBLE] upgrade
     * is completed successfully
     */
    fun showAppUpdateSuccessSnack(actionListener: View.OnClickListener)

    /**
     * Method to display a snackBar once the [AppUpdateType.FLEXIBLE] upgrade
     * results in error
     */
    fun showAppUpdateFailedSnack(actionListener: View.OnClickListener)
}
