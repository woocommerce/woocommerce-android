package com.woocommerce.android.ui.orders.wooshippinglabels.fedex

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FedExTermsOfServiceViewModelTest : BaseUnitTest() {
    private val acceptFedExTerms: AcceptFedExTerms = mock()

    private lateinit var savedState: SavedStateHandle
    private lateinit var sut: FedExTermsOfServiceViewModel

    private fun createViewModel() {
        savedState = SavedStateHandle()
        sut = FedExTermsOfServiceViewModel(savedState, acceptFedExTerms)
    }

    @Test
    fun `when terms are not accepted, then confirm button is disabled`() = testBlocking {
        createViewModel()

        val viewState = sut.viewState.runAndCaptureValues {
            advanceUntilIdle()
        }.last()

        assertThat(viewState.isTermsOfServiceAccepted).isFalse()
    }

    @Test
    fun `when terms are accepted, then confirm button is enabled`() = testBlocking {
        createViewModel()

        val states = sut.viewState.runAndCaptureValues {
            advanceUntilIdle()
            sut.viewState.value!!.onTermsOfServiceCheckedChanged(true)
            advanceUntilIdle()
        }

        assertThat(states.last().isTermsOfServiceAccepted).isTrue()
    }

    @Test
    fun `when url is clicked, then open wordpress terms URL`() = testBlocking {
        createViewModel()
        var url: String? = null
        sut.event.observeForever {
            if (it is MultiLiveEvent.Event.LaunchUrlInChromeTab) {
                url = it.url
            }
        }

        sut.viewState.runAndCaptureValues {
            advanceUntilIdle()
        }.last().onUrlClicked(FedExTermsOfServiceViewModel.TERMS_URL_ID)

        assertThat(url).isEqualTo("https://wordpress.com/tos/")
    }

    @Test
    fun `when confirmation succeeds, then exit with result`() = testBlocking {
        whenever(acceptFedExTerms.invoke()).thenReturn(Result.success(Unit))
        createViewModel()
        var event: MultiLiveEvent.Event.ExitWithResult<*>? = null
        sut.event.observeForever { if (it is MultiLiveEvent.Event.ExitWithResult<*>) event = it }

        sut.viewState.runAndCaptureValues {
            advanceUntilIdle()
            sut.viewState.value!!.onTermsOfServiceCheckedChanged(true)
            advanceUntilIdle()
            sut.viewState.value!!.onContinueClicked()
            advanceUntilIdle()
        }

        assertThat(event?.data).isEqualTo(Unit)
    }

    @Test
    fun `when confirmation fails, then show generic error snackbar`() = testBlocking {
        whenever(acceptFedExTerms.invoke()).thenReturn(Result.failure(Exception("boom")))
        createViewModel()
        var event: MultiLiveEvent.Event.ShowSnackbar? = null
        sut.event.observeForever { if (it is MultiLiveEvent.Event.ShowSnackbar) event = it }

        sut.viewState.runAndCaptureValues {
            advanceUntilIdle()
            sut.viewState.value!!.onTermsOfServiceCheckedChanged(true)
            advanceUntilIdle()
            sut.viewState.value!!.onContinueClicked()
            advanceUntilIdle()
        }

        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
    }
}
