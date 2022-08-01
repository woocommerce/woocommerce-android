package com.woocommerce.android.analytics

import com.google.firebase.analytics.ktx.ParametersBuilder

interface ExperimentTracker {
    companion object {
        const val PROLOGUE_EXPERIMENT_ELIGIBLE_EVENT = "prologue_carousel_displayed"
        const val SITE_CREDENTIALS_EXPERIMENT_ELIGIBLE_EVENT = "site_credentials_experiment_eligible"
        const val MAGIC_LINK_SENT_EXPERIMENT_ELIGIBLE_EVENT = "magic_link_sent_experiment_eligible"
        const val LOGIN_SUCCESSFUL_EVENT = "login_successful"
    }

    fun log(event: String, block: (ParametersBuilder.() -> Unit)? = null)
}
