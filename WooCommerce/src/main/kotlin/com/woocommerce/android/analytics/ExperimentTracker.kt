package com.woocommerce.android.analytics

import com.google.firebase.analytics.ParametersBuilder

interface ExperimentTracker {
    companion object {
        const val LOGIN_SUCCESSFUL_EVENT = "login_successful"
        const val SITE_VERIFICATION_SUCCESSFUL_EVENT = "site_verification_successful"
    }

    fun log(event: String, block: (ParametersBuilder.() -> Unit)? = null)
}
