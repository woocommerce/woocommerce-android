package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.CHECK_MOBILE_DEVICE
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.INSERT_CARD
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.INSERT_OR_SWIPE_CARD
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.MULTIPLE_CONTACTLESS_CARDS_DETECTED
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.REMOVE_CARD
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.RETRY_CARD
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.SWIPE_CARD
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.TRY_ANOTHER_CARD
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage.TRY_ANOTHER_READ_METHOD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType

internal class AdditionalInfoMapper {
    fun map(displayMsg: ReaderDisplayMessage): AdditionalInfoType =
        when (displayMsg) {
            RETRY_CARD -> AdditionalInfoType.RETRY_CARD
            INSERT_CARD -> AdditionalInfoType.INSERT_CARD
            INSERT_OR_SWIPE_CARD -> AdditionalInfoType.INSERT_OR_SWIPE_CARD
            SWIPE_CARD -> AdditionalInfoType.SWIPE_CARD
            REMOVE_CARD -> AdditionalInfoType.REMOVE_CARD
            MULTIPLE_CONTACTLESS_CARDS_DETECTED -> AdditionalInfoType.MULTIPLE_CONTACTLESS_CARDS_DETECTED
            TRY_ANOTHER_READ_METHOD -> AdditionalInfoType.TRY_ANOTHER_READ_METHOD
            TRY_ANOTHER_CARD -> AdditionalInfoType.TRY_ANOTHER_CARD
            CHECK_MOBILE_DEVICE -> AdditionalInfoType.CHECK_MOBILE_DEVICE
        }
}
