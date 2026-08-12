package com.woocommerce.android.ui.woopos.cardreader.remote

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.remote.CardReaderRemoteError
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosRemoteReaderErrorMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
) {
    fun toUserMessage(error: CardReaderRemoteError, @StringRes fallback: Int): String = resourceProvider.getString(
        when (error) {
            CardReaderRemoteError.PhoneNotEligible -> R.string.woopos_remote_reader_failed_phone_not_eligible
            CardReaderRemoteError.NfcDisabled -> R.string.woopos_remote_reader_failed_nfc_disabled
            CardReaderRemoteError.TokenInvalid -> R.string.woopos_remote_reader_failed_token_invalid
            CardReaderRemoteError.ConnectFailed,
            CardReaderRemoteError.CollectFailed,
            CardReaderRemoteError.CreateIntentFailed,
            CardReaderRemoteError.UnexpectedReply,
            is CardReaderRemoteError.Unknown -> fallback
        }
    )
}
