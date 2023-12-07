package com.woocommerce.android.ui.orders.creation.giftcards

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateEditGiftCardViewModelTest: BaseUnitTest() {
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
        val expectedIsValidCode = true
        var lastIsValidCodeUpdate: Boolean? = null
        sut.viewState.observeForever { lastIsValidCodeUpdate = it.isValidCode }

        // When
        sut.onGiftCardChanged("T3ST-W1TH-L33T-C0D3")

        // Then
        assertThat(lastIsValidCodeUpdate).isEqualTo(expectedIsValidCode)
    }
}
