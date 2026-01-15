package com.woocommerce.android.e2e.helpers

import androidx.test.platform.app.InstrumentationRegistry

object TestSecrets {
    val apiUrl: String
        get() = getSecret("API_URL")

    val apiEmail: String
        get() = getSecret("API_EMAIL")

    val apiPassword: String
        get() = getSecret("API_PASSWORD")

    private fun getSecret(key: String): String {
        val args = InstrumentationRegistry.getArguments()
        val fromArgs = args.getString(key)
        if (!fromArgs.isNullOrEmpty()) {
            return fromArgs
        }

        val fromSystemProperty = System.getProperty(key)
        if (!fromSystemProperty.isNullOrEmpty()) {
            return fromSystemProperty
        }

        throw IllegalStateException(
            "Secret '$key' not found. Ensure secrets are configured in secretProperties " +
                "and forwarded to tests via Gradle or Fladle."
        )
    }
}
