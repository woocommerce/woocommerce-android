package com.woocommerce.android.analytics

import com.google.firebase.analytics.ParametersBuilder
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.Firebase
import javax.inject.Inject

class FirebaseTracker @Inject constructor() : ExperimentTracker {
    override fun log(event: String, block: (ParametersBuilder.() -> Unit)?) {
        if (block != null) {
            Firebase.analytics.logEvent(event, block)
        } else {
            Firebase.analytics.logEvent(event, null)
        }
    }
}
