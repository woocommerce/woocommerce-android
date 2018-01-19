package com.woocommerce.android.analytics

import java.util.HashMap

class AnalyticsTracker {
    // TODO Connect to Nosara
    enum class Stat {
        SIGNED_IN,
        LOGIN_ACCESSED,
        LOGIN_MAGIC_LINK_EXITED,
        LOGIN_MAGIC_LINK_FAILED,
        LOGIN_MAGIC_LINK_OPENED,
        LOGIN_MAGIC_LINK_REQUESTED,
        LOGIN_MAGIC_LINK_SUCCEEDED,
        LOGIN_FAILED,
        LOGIN_INSERTED_INVALID_URL,
        LOGIN_AUTOFILL_CREDENTIALS_FILLED,
        LOGIN_AUTOFILL_CREDENTIALS_UPDATED,
        LOGIN_EMAIL_FORM_VIEWED,
        LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED,
        LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED,
        LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED,
        LOGIN_PASSWORD_FORM_VIEWED,
        LOGIN_URL_FORM_VIEWED,
        LOGIN_URL_HELP_SCREEN_VIEWED,
        LOGIN_USERNAME_PASSWORD_FORM_VIEWED,
        LOGIN_TWO_FACTOR_FORM_VIEWED,
        LOGIN_FORGOT_PASSWORD_CLICKED,
        LOGIN_SOCIAL_BUTTON_CLICK,
        LOGIN_SOCIAL_BUTTON_FAILURE,
        LOGIN_SOCIAL_CONNECT_SUCCESS,
        LOGIN_SOCIAL_CONNECT_FAILURE,
        LOGIN_SOCIAL_SUCCESS,
        LOGIN_SOCIAL_FAILURE,
        LOGIN_SOCIAL_2FA_NEEDED,
        LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING,
        LOGIN_SOCIAL_ERROR_UNKNOWN_USER,
        LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE,
        ADDED_SELF_HOSTED_SITE
    }

    companion object {
        fun track(stat: Stat) {
            // TODO
        }

        fun track(stat: Stat, properties: Map<String, *>) {
            // TODO
        }

        /**
         * A convenience method for logging an error event with some additional meta data.
         * @param stat The stat to track.
         * @param errorContext A string providing additional context (if any) about the error.
         * @param errorType The type of error.
         * @param errorDescription The error text or other description.
         */
        fun track(stat: Stat, errorContext: String, errorType: String, errorDescription: String) {
            val props = HashMap<String, String>()
            props.put("error_context", errorContext)
            props.put("error_type", errorType)
            props.put("error_description", errorDescription)
            track(stat, props)
        }
    }
}
