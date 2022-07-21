package com.woocommerce.android.analytics

import com.google.firebase.analytics.ktx.ParametersBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import javax.inject.Inject

class FirebaseTracker @Inject constructor() {
    object Event {
        const val PROLOGUE_CAROUSEL_DISPLAYED = "prologue_carousel_displayed"
        const val SURVEY_DISPLAYED = "survey_displayed"
        const val LOGIN_SUCCESSFUL = "login_successful"
    }

    fun log(event: String, block: (ParametersBuilder.() -> Unit)? = null) {
        if (block != null) {
            Firebase.analytics.logEvent(event, block)
        } else {
            Firebase.analytics.logEvent(event, null)
        }
    }
}
