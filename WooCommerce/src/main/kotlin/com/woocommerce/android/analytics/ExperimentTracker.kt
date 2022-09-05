package com.woocommerce.android.analytics

import com.google.firebase.analytics.ktx.ParametersBuilder

interface ExperimentTracker {
    companion object {
        const val PROLOGUE_EXPERIMENT_ELIGIBLE_EVENT = "prologue_carousel_displayed"
        const val MAGIC_LINK_SENT_EXPERIMENT_ELIGIBLE_EVENT = "magic_link_sent_experiment_eligible"
        const val MAGIC_LINK_EXPERIMENT_ELIGIBLE_EVENT = "magic_link_experiment_eligible"
        const val LOGIN_BUTTON_SWAP_EXPERIMENT_ELIGIBLE_EVENT = "login_swap_experiment_eligible"
        const val LOGIN_SUCCESSFUL_EVENT = "login_successful"
        const val NEW_JETPACK_TIMEOUT_POLICY_ELIGIBLE_EVENT = "new_jetpack_timeout_experiment_eligible"
        const val SITE_VERIFICATION_SUCCESSFUL_EVENT = "site_verification_successful"
    }

    fun log(event: String, block: (ParametersBuilder.() -> Unit)? = null)
}
