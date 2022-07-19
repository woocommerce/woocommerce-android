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
        const val LOGIN_OPTION_SELECTED = "login_option_selected"
        const val SURVEY_BUTTON_TAPPED = "survey_button_tapped"
        const val LOGIN_SUCCESSFUL = "login_successful"
    }

    object Param {
        const val SURVEY_OPTION = "survey_option"
        const val LOGIN_TYPE = "login_type"
    }

    object Value {
        const val SURVEY_OPTION_JUST_EXPLORING = "survey_option_just_exploring"
        const val SURVEY_OPTION_FREE_PLUGIN = "survey_option_free_plugin"
        const val SURVEY_OPTION_SET_UP_STORE = "survey_option_set_up_store"
        const val SURVEY_OPTION_FEATURES_AVAILABLE = "survey_option_features_available"

        const val LOGIN_TYPE_SITE_CREDENTIALS = "site_credentials"
        const val LOGIN_TYPE_SITE_ADDRESS = "site_address"
        const val LOGIN_TYPE_WPCOM = "wpcom"
        const val LOGIN_TYPE_SOCIAL = "social"
        const val LOGIN_TYPE_MAGIC_LINK = "magic_link"
        const val LOGIN_TYPE_QR_CODE = "qr_code"
    }

    fun log(event: String, block: (ParametersBuilder.() -> Unit)? = null) {
        if (block != null) {
            Firebase.analytics.logEvent(event, block)
        } else {
            Firebase.analytics.logEvent(event, null)
        }
    }
}
