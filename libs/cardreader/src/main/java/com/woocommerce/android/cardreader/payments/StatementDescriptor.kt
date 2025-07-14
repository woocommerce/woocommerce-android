@file:Suppress("MaxLineLength") // this is needed for the long link in kdoc.

package com.woocommerce.android.cardreader.payments

/**
 * Class which acts as a wrapper to raw statement descriptor text and applies set of sanitation operations to prepare it
 * for use with Stripe API. There are the following requirements applied:
 * 1. Text must be max 22 chars long.
 * 2. Text must be min 5 chars long.
 * 3. It must not contain the following characters: <, >, ', ", *.
 * 4. It must contain at least one letter.
 *
 * **See:** [Stripe docs](https://stripe.dev/stripe-terminal-android/external/com.stripe.stripeterminal.external.models/-payment-intent-parameters/-builder/set-statement-descriptor.html).
 *
 * [StatementDescriptor] exposes the [StatementDescriptor.value] property which is ready to use with Stripe API.
 *
 * @param rawValue Raw statement descriptor value used while instantiating [StatementDescriptor] object.
 * @property value Statement descriptor value after sanitation.
 */
class StatementDescriptor(rawValue: String?) {

    val value: String? by lazy {
        // checks if rawValue contains at least one letter
        if (rawValue == null || rawValue.matches(".*[a-zA-Z]+.*".toRegex()).not()) return@lazy null
        StringBuilder().apply {
            // replaces illegal chars with replacement char
            append(rawValue.take(MAX_LENGTH).split("<", ">", "'", "\"", "*").joinToString(REPLACEMENT_CHAR))
            // in case rawValue is too short appends with replacement chars
            if (length < MIN_LENGTH) {
                val missingCharsCount = MIN_LENGTH - length
                append((1..missingCharsCount).joinToString("") { REPLACEMENT_CHAR })
            }
        }.toString()
    }

    companion object {
        const val MAX_LENGTH = 22
        const val MIN_LENGTH = 5
        const val REPLACEMENT_CHAR = "-"
    }
}
