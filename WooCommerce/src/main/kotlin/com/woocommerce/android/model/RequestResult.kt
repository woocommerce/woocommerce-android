package com.woocommerce.android.model

/**
 * Can be used to communicate the result of an event-related request. An example
 * of a request would be fetching orders from FluxC.
 */
enum class RequestResult {
    SUCCESS,
    ERROR,
    NO_ACTION_NEEDED,
    RETRY
}
