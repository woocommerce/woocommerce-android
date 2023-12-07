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
        val savedState = OrderCreateEditGiftCardFragmentArgs("").initSavedStateHandle()
        sut = OrderCreateEditGiftCardViewModel(savedState)
    }

    @Test
    fun `onGiftCardChanged should update giftCard`() = testBlocking {
        // Given
        val expectedGiftCardCode = "test gift card code"
        var lastGiftCardUpdate: String? = null
        sut.viewState.observeForever { lastGiftCardUpdate = it.giftCard }

        // When
        sut.onGiftCardChanged("test gift card code")

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo(expectedGiftCardCode)
    }

    @Test
    fun `when gift card code is valid, then isValidCode is true`() = testBlocking {
        // Given
        var lastIsValidCodeUpdate: Boolean? = null
        sut.viewState.observeForever { lastIsValidCodeUpdate = it.isValidCode }

        // When
        sut.onGiftCardChanged("T3ST-W1TH-L33T-C0D3")

        // Then
        assertThat(lastIsValidCodeUpdate).isTrue
    }

    @Test
    fun `when gift card code is empty, then isValidCode is true`() = testBlocking {
        // Given
        var lastIsValidCodeUpdate: Boolean? = null
        sut.viewState.observeForever { lastIsValidCodeUpdate = it.isValidCode }

        // When
        sut.onGiftCardChanged("")

        // Then
        assertThat(lastIsValidCodeUpdate).isTrue
    }

    @Test
    fun `when gift card code is invalid, then isValidCode is false`() = testBlocking {
        // Given
        var lastIsValidCodeUpdate: Boolean? = null
        sut.viewState.observeForever { lastIsValidCodeUpdate = it.isValidCode }

        // When
        sut.onGiftCardChanged("invalid gift card code")

        // Then
        assertThat(lastIsValidCodeUpdate).isFalse
    }
}
