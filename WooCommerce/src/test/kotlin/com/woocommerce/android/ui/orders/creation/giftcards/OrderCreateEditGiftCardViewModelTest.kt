package com.woocommerce.android.ui.orders.creation.giftcards

import com.woocommerce.android.ui.orders.creation.giftcards.OrderCreateEditGiftCardViewModel.GiftCardResult
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
    fun `when navArgs has valid giftCard, then giftCard should be set with the same value`() {
        // Given
        val expectedGiftCardCode = "test gift card code"
        createSutWith(initialGiftCardValue = expectedGiftCardCode)
        var lastGiftCardUpdate: String? = null
        sut.viewState.observeForever { lastGiftCardUpdate = it.giftCard }

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo(expectedGiftCardCode)
    }

    @Test
    fun `when navArgs has null giftCard, then giftCard should be set with empty string`() {
        // Given
        createSutWith(initialGiftCardValue = null)
        var lastGiftCardUpdate: String? = null
        sut.viewState.observeForever { lastGiftCardUpdate = it.giftCard }

        // Then
        assertThat(lastGiftCardUpdate).isEqualTo("")
    }

    @Test
    fun `when onDoneButtonClicked is triggered, then expected ExitWithResult event should be triggered`() {
        // Given
        var lastEvent: MultiLiveEvent.Event.ExitWithResult<*>? = null
        sut.event.observeForever {
            lastEvent = it as? MultiLiveEvent.Event.ExitWithResult<*>
        }

        // When
        sut.onGiftCardChanged("test-gift-card-code")
        sut.onDoneButtonClicked()

        // Then
        assertThat(lastEvent).isNotNull
        assertThat(lastEvent?.data).isInstanceOf(GiftCardResult::class.java)
        val giftCardResult = lastEvent?.data as GiftCardResult
        assertThat(giftCardResult.selectedGiftCard).isEqualTo("test-gift-card-code")
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

    private fun createSutWith(initialGiftCardValue: String?) {
        val savedState = OrderCreateEditGiftCardFragmentArgs(initialGiftCardValue).toSavedStateHandle()
        sut = OrderCreateEditGiftCardViewModel(savedState)
    }
}
