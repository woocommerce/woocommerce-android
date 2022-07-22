package com.woocommerce.android.analytics

import com.google.firebase.analytics.ktx.ParametersBuilder

interface ExperimentTracker {
    companion object {
        const val PROLOGUE_CAROUSEL_DISPLAYED_EVENT = "prologue_carousel_displayed"
        const val LOGIN_SUCCESSFUL_EVENT = "login_successful"
    }

    fun log(event: String, block: (ParametersBuilder.() -> Unit)? = null)
}
