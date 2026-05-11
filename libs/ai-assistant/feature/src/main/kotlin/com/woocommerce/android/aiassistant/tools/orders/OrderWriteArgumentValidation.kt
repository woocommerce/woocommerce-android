package com.woocommerce.android.aiassistant.tools.orders

internal const val ORDER_CUSTOMER_NOTE_MAX_LENGTH = 1000
internal const val ORDER_BILLING_EMAIL_MAX_LENGTH = 254

internal fun validateOrderWriteArguments(
    customerNote: String?,
    billingEmail: String?,
): String? =
    customerNote.validateMaxLength("customer_note", ORDER_CUSTOMER_NOTE_MAX_LENGTH)
        ?: billingEmail.validateMaxLength("billing_email", ORDER_BILLING_EMAIL_MAX_LENGTH)
        ?: billingEmail.validateBillingEmail()

private fun String?.validateMaxLength(fieldName: String, maxLength: Int): String? =
    if (this != null && length > maxLength) {
        "$fieldName must be at most $maxLength characters."
    } else {
        null
    }

private fun String?.validateBillingEmail(): String? =
    if (this != null && !BILLING_EMAIL_REGEX.matches(this)) {
        "billing_email must be a valid email address."
    } else {
        null
    }

private val BILLING_EMAIL_REGEX = Regex(
    "[a-zA-Z0-9+._%\\-]{1,256}@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
)
