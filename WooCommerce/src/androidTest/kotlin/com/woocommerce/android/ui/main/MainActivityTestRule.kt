package com.woocommerce.android.ui.main

import android.content.Intent
import android.support.test.rule.ActivityTestRule
import com.woocommerce.android.di.MockedSelectedSiteModule
import org.wordpress.android.fluxc.model.SiteModel

class MainActivityTestRule : ActivityTestRule<MainActivity>(MainActivity::class.java, false, false) {
    /**
     * Bypass the Login flow and launch directly into the [MainActivity].
     */
    fun launchMainActivityLoggedIn(startIntent: Intent?, siteModel: SiteModel): MainActivity {
        // Configure the mocked MainPresenter to pretend the user is logged in.
        // We normally wouldn't need the MockedMainModule method, and just configure the mocked presenter directly
        // using whenever(activityTestRule.activity.presenter.userIsLoggedIn()).thenReturn(true)
        // In this case, however, userIsLoggedIn() is called in the activity's onCreate(), which means after
        // launchActivity() is too late, but the activity's presenter is null before that.
        // So, we need to configure this at the moment the injection is happening: when the presenter is initialized.
        MockedMainModule.setUserIsLoggedInResponse(true)
        // Preload the SelectedSite with a SiteModel, to satisfy the expectation that it was set during login
        // The reason for doing this here is the same as for the MockedMainModule
        MockedSelectedSiteModule.setSiteModel(siteModel)
        return super.launchActivity(startIntent)
    }
}
