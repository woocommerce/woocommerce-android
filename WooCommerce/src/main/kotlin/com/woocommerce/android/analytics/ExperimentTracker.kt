package com.woocommerce.android.analytics

import com.google.firebase.analytics.ktx.ParametersBuilder

interface ExperimentTracker {
    companion object {
        const val PROLOGUE_EXPERIMENT_ELIGIBLE_EVENT = "prologue_carousel_displayed"
        const val LOGIN_SUCCESSFUL_EVENT = "login_successful"
        const val NEW_JETPACK_TIMEOUT_POLICY_ELIGIBLE_EVENT = "new_jetpack_timeout_experiment_eligible"
        const val SITE_VERIFICATION_SUCCESSFUL_EVENT = "site_verification_successful"
    }

    fun log(event: String, block: (ParametersBuilder.() -> Unit)? = null)
}
