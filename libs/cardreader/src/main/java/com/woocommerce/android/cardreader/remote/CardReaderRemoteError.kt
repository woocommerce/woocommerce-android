package com.woocommerce.android.cardreader.remote

sealed class CardReaderRemoteError(val code: String) {
    data object PhoneNotEligible : CardReaderRemoteError(CODE_PHONE_NOT_ELIGIBLE)

    data object NfcDisabled : CardReaderRemoteError(CODE_NFC_DISABLED)

    data object TokenInvalid : CardReaderRemoteError(CODE_TOKEN_INVALID)

    data object ConnectFailed : CardReaderRemoteError(CODE_CONNECT_FAILED)

    data object CollectFailed : CardReaderRemoteError(CODE_COLLECT_FAILED)

    data object CreateIntentFailed : CardReaderRemoteError(CODE_CREATE_INTENT_FAILED)

    data object UnexpectedReply : CardReaderRemoteError(CODE_UNEXPECTED_REPLY)

    data class Unknown(val raw: String) : CardReaderRemoteError(raw)

    companion object {
        private const val CODE_PHONE_NOT_ELIGIBLE = "phone_not_eligible"
        private const val CODE_NFC_DISABLED = "nfc_disabled"
        private const val CODE_TOKEN_INVALID = "token_invalid"
        private const val CODE_CONNECT_FAILED = "connect_failed"
        private const val CODE_COLLECT_FAILED = "collect_failed"
        private const val CODE_CREATE_INTENT_FAILED = "create_intent_failed"
        private const val CODE_UNEXPECTED_REPLY = "unexpected_reply"

        fun fromCode(code: String): CardReaderRemoteError = when (code) {
            CODE_PHONE_NOT_ELIGIBLE -> PhoneNotEligible
            CODE_NFC_DISABLED -> NfcDisabled
            CODE_TOKEN_INVALID -> TokenInvalid
            CODE_CONNECT_FAILED -> ConnectFailed
            CODE_COLLECT_FAILED -> CollectFailed
            CODE_CREATE_INTENT_FAILED -> CreateIntentFailed
            CODE_UNEXPECTED_REPLY -> UnexpectedReply
            else -> Unknown(code)
        }
    }
}
