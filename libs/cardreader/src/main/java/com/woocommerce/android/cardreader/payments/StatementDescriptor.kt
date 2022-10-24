package com.woocommerce.android.cardreader.payments

/**
 * Class which acts as a wrapper to raw statement descriptor text and applies set of sanitation operations to prepare it
 * for use with Stripe API. There are the following requirements applied:
 * 1. Text must be 22 chars long.
 * 2. It must not contain the following characters: <, >, ', ".
 *
 * [StatementDescriptor] exposes the [StatementDescriptor.value] property which is ready to use with Stripe API.
 *
 * @param rawValue Raw statement descriptor value used while instantiating [StatementDescriptor] object.
 * @property value Statement descriptor value after sanitation.
 */
class StatementDescriptor(rawValue: String?) {

    val value: String? = rawValue?.take(MAX_LENGTH)?.split("<", ">", "'", "\"")?.joinToString(REPLACEMENT_CHAR)

    companion object {
        const val MAX_LENGTH = 22
        const val REPLACEMENT_CHAR = "-"
    }
}
