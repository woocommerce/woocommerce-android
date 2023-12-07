package com.woocommerce.android.ui.orders.creation.giftcards

import androidx.lifecycle.SavedStateHandle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class OrderCreateEditGiftCardViewModelTest {
    private lateinit var sut: OrderCreateEditGiftCardViewModel

    @Before
    fun setUp() {
        val savedState: SavedStateHandle = mock()
        sut = OrderCreateEditGiftCardViewModel(savedState)
    }

    @Test
    fun `onGiftCardChanged should update giftCard`() {
        // Given
        val expectedGiftCardCode = "test gift card code"
        var lastGiftCardUpdate: String? = null
        sut.viewState.observeForever { lastGiftCardUpdate = it.giftCard }

        // When
        sut.onGiftCardChanged("test gift card code")

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo(expectedGiftCardCode)
    }
}
