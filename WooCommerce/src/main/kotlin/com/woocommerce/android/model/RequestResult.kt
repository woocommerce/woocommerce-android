package com.woocommerce.android.model

/**
 * Can be used to communicate the result of an event-related request. An example
 * of a request would be fetching orders from FluxC.
 */
enum class RequestResult {
    SUCCESS,
    API_ERROR, // errors from the FluxC side
    ERROR, // network or generic errors
    NO_ACTION_NEEDED,
    RETRY
}
