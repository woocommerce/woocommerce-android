package com.woocommerce.android.ui.orders.creation.giftcards

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateEditGiftCardViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderCreateEditGiftCardViewModel

    @Before
    fun setUp() {
        createSutWith(initialGiftCardValue = "")
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
        createSutWith(initialGiftCardValue = expectedGiftCardCode)
        var lastGiftCardUpdate: String? = null
        sut.giftCard.observeForever { lastGiftCardUpdate = it }

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo(expectedGiftCardCode)
    }

    @Test
    fun `when navArgs has null giftCard, then giftCard should be set with empty string`() {
        // Given
        createSutWith(initialGiftCardValue = null)
        var lastGiftCardUpdate: String? = null
        sut.giftCard.observeForever { lastGiftCardUpdate = it }

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo("")
    }

    private fun createSutWith(initialGiftCardValue: String?) {
        val savedState = OrderCreateEditGiftCardFragmentArgs(initialGiftCardValue)
            .initSavedStateHandle()
        sut = OrderCreateEditGiftCardViewModel(savedState)
    }
}
