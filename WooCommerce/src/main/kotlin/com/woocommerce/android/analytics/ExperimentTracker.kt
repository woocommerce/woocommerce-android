package com.woocommerce.android.analytics

import com.google.firebase.analytics.ktx.ParametersBuilder

interface ExperimentTracker {
    companion object {
        const val PROLOGUE_EXPERIMENT_ELIGIBLE_EVENT = "prologue_carousel_displayed"
        const val LOGIN_SUCCESSFUL_EVENT = "login_successful"
        const val SITE_VERIFICATION_SUCCESSFUL_EVENT = "site_verification_successful"
        const val SIMPLIFIED_LOGIN_ELIGIBLE_EVENT = "simplified_login_experiment_eligible"
        const val JETPACK_INSTALLATION_ELIGIBLE_EVENT = "wcandroid_jetpack_installation_eligible"
        const val SIMPLIFIED_LOGIN_SUCCESSFUL_EVENT = "my_store_displayed"
    }

    fun log(event: String, block: (ParametersBuilder.() -> Unit)? = null)
}
