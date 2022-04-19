package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.CHECK_MOBILE_DEVICE
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.INSERT_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.INSERT_OR_SWIPE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.MULTIPLE_CONTACTLESS_CARDS_DETECTED
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.REMOVE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.RETRY_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.SWIPE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.TRY_ANOTHER_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.TRY_ANOTHER_READ_METHOD
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class AdditionalInfoMapperTest : CardReaderBaseUnitTest() {
    private lateinit var additionalInfoMapper: AdditionalInfoMapper

    @Before
    fun setup() {
        additionalInfoMapper = AdditionalInfoMapper()
    }

    @Test
    fun `given RETRY_CARD display message, then it is correctly mapped into AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.RETRY_CARD)

        assertThat(additionalInfoType).isEqualTo(RETRY_CARD)
    }

    @Test
    fun `given INSERT_CARD display message, then it is correctly mapped into AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.INSERT_CARD)

        assertThat(additionalInfoType).isEqualTo(INSERT_CARD)
    }

    @Test
    fun `given INSERT_OR_SWIPE_CARD display message, then it is correctly mapped into AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.INSERT_OR_SWIPE_CARD)

        assertThat(additionalInfoType).isEqualTo(INSERT_OR_SWIPE_CARD)
    }

    @Test
    fun `given SWIPE_CARD display message, then it is correctly mapped into AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.SWIPE_CARD)

        assertThat(additionalInfoType).isEqualTo(SWIPE_CARD)
    }

    @Test
    fun `given REMOVE_CARD display message, then it is correctly mapped into AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.REMOVE_CARD)

        assertThat(additionalInfoType).isEqualTo(REMOVE_CARD)
    }

    @Test
    fun `given MULTIPLE_CONTACTLESS_CARDS_DETECTED display message, then it's correctly mapped AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.MULTIPLE_CONTACTLESS_CARDS_DETECTED)

        assertThat(additionalInfoType)
            .isEqualTo(MULTIPLE_CONTACTLESS_CARDS_DETECTED)
    }

    @Test
    fun `given TRY_ANOTHER_READ_METHOD display message, then it is correctly mapped into AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.TRY_ANOTHER_READ_METHOD)

        assertThat(additionalInfoType).isEqualTo(TRY_ANOTHER_READ_METHOD)
    }

    @Test
    fun `given TRY_ANOTHER_CARD display message, then it is correctly mapped into AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.TRY_ANOTHER_CARD)

        assertThat(additionalInfoType).isEqualTo(TRY_ANOTHER_CARD)
    }

    @Test
    fun `given CHECK_MOBILE_DEVICE display message, then it is correctly mapped into AdditionalInfo type`() {
        val additionalInfoType = additionalInfoMapper.map(ReaderDisplayMessage.CHECK_MOBILE_DEVICE)

        assertThat(additionalInfoType).isEqualTo(CHECK_MOBILE_DEVICE)
    }
}
