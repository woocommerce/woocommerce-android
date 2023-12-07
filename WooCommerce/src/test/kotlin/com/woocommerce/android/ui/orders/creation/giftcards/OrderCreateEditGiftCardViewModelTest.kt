package com.woocommerce.android.ui.orders.creation.giftcards

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateEditGiftCardViewModelTest: BaseUnitTest() {
    private lateinit var sut: OrderCreateEditGiftCardViewModel

    @Before
    fun setUp() {
        val savedState = OrderCreateEditGiftCardFragmentArgs("").initSavedStateHandle()
        sut = OrderCreateEditGiftCardViewModel(savedState)
    }

    @Test
    fun `onGiftCardChanged should update giftCard`() {
        // Given
        val expectedGiftCardCode = "test gift card code"
        var lastGiftCardUpdate: String? = null
        sut.giftCard.observeForever { lastGiftCardUpdate = it }

        // When
        sut.onGiftCardChanged("test gift card code")

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo(expectedGiftCardCode)
    }

    @Test
    fun `when navArgs has valid giftCard, then giftCard should be set with the same value`() {
        // Given
        val expectedGiftCardCode = "test gift card code"
        val savedState = OrderCreateEditGiftCardFragmentArgs(expectedGiftCardCode).initSavedStateHandle()
        sut = OrderCreateEditGiftCardViewModel(savedState)
        var lastGiftCardUpdate: String? = null
        sut.giftCard.observeForever { lastGiftCardUpdate = it }

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo(expectedGiftCardCode)
    }

    @Test
    fun `when navArgs has null giftCard, then giftCard should be set with empty string`() {
        // Given
        val savedState = OrderCreateEditGiftCardFragmentArgs(null).initSavedStateHandle()
        sut = OrderCreateEditGiftCardViewModel(savedState)
        var lastGiftCardUpdate: String? = null
        sut.giftCard.observeForever { lastGiftCardUpdate = it }

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo("")
    }
}
