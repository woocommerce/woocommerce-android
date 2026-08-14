package com.woocommerce.android.cardreader.remote

import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException

private const val MAX_CAUSE_DEPTH = 5

internal fun Throwable.toCardReaderRemoteError(fallback: CardReaderRemoteError): CardReaderRemoteError = when (this) {
    is CardReaderRemoteFailure -> error
    else -> terminalErrorCode()?.toCardReaderRemoteError() ?: fallback
}

private fun Throwable.terminalErrorCode(): TerminalErrorCode? {
    val visited = mutableListOf<Throwable>()
    var current: Throwable? = this
    while (current != null && visited.size < MAX_CAUSE_DEPTH && visited.none { it === current }) {
        if (current is TerminalException) return current.errorCode
        visited.add(current)
        current = current.cause
    }
    return null
}

private fun TerminalErrorCode.toCardReaderRemoteError(): CardReaderRemoteError? = when (this) {
    TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
    TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED,
    TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_ANDROID_VERSION,
    TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_PROCESSOR,
    TerminalErrorCode.TAP_TO_PAY_INSECURE_ENVIRONMENT -> CardReaderRemoteError.PhoneNotEligible
    TerminalErrorCode.TAP_TO_PAY_NFC_DISABLED -> CardReaderRemoteError.NfcDisabled
    TerminalErrorCode.CONNECTION_TOKEN_PROVIDER_ERROR,
    TerminalErrorCode.CONNECTION_TOKEN_PROVIDER_ERROR_WHILE_FORWARDING,
    TerminalErrorCode.SESSION_EXPIRED -> CardReaderRemoteError.TokenInvalid
    else -> null
}
