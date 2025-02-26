package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.InputValue
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.RestrictionType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ShowContentTypeDialog
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ShowRestrictionTypeDialog
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class WooShippingCustomsFormViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: WooShippingCustomsFormViewModel

    @Before
    fun setup() {
        viewModel = WooShippingCustomsFormViewModel(
            savedState = SavedStateHandle()
        )
    }

    @Test
    fun `onContentTypeClick should trigger ShowContentTypeDialog event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever {
            latestEvent = it
        }
        viewModel.onContentTypeClick()
        assertThat(latestEvent).isEqualTo(ShowContentTypeDialog(ContentType.MERCHANDISE))
    }

    @Test
    fun `onRestrictionTypeClick should trigger ShowRestrictionTypeDialog event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever {
            latestEvent = it
        }
        viewModel.onRestrictionTypeClick()
        assertThat(latestEvent).isEqualTo(ShowRestrictionTypeDialog(RestrictionType.NONE))
    }

    @Test
    fun `onITNChanged should update itnValue with valid input`() = testBlocking {
        val newItnValue = "AES X20201234567890"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onITNChanged(newItnValue)
        assertThat(capturedViewState?.itnValue).isEqualTo(InputValue.Data(newItnValue))
    }

    @Test
    fun `onITNChanged should update itnValue with invalid input`() = testBlocking {
        val newItnValue = "INVALID_ITN"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onITNChanged(newItnValue)
        assertThat(capturedViewState?.itnValue).isInstanceOf(InputValue.Error::class.java)
    }

    @Test
    fun `onReturnToSenderChanged should update returnToSenderChecked in viewState`() = testBlocking {
        val isChecked = true
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onReturnToSenderChanged(isChecked)
        assertThat(capturedViewState?.returnToSenderChecked).isEqualTo(isChecked)
    }

    @Test
    fun `onContentTypeSelected should update contentType in viewState`() = testBlocking {
        val contentType = ContentType.GIFT
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onContentTypeSelected(contentType)
        assertThat(capturedViewState?.contentType).isEqualTo(contentType)
    }

    @Test
    fun `onRestrictionTypeSelected should update restrictionType in viewState`() = testBlocking {
        val restrictionType = RestrictionType.QUARANTINE
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onRestrictionTypeSelected(restrictionType)
        assertThat(capturedViewState?.restrictionType).isEqualTo(restrictionType)
    }

    @Test
    fun `onOtherContentInputChanged should update otherContentInput with valid input`() = testBlocking {
        val newValue = "Important Stuff"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onOtherContentInputChanged(newValue)
        assertThat(capturedViewState?.otherContentInput).isEqualTo(InputValue.Data(newValue))
    }

    @Test
    fun `onOtherContentInputChanged should update otherContentInput with invalid input`() = testBlocking {
        val newValue = ""
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onOtherContentInputChanged(newValue)
        assertThat(
            capturedViewState?.otherContentInput
        ).isInstanceOf(InputValue.Error::class.java)
    }

    @Test
    fun `onRestrictionDetailsInputChanged should update otherRestrictionInput with valid input`() = testBlocking {
        val newValue = "Restricted Stuff"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onRestrictionDetailsInputChanged(newValue)
        assertThat(capturedViewState?.otherRestrictionInput).isEqualTo(InputValue.Data(newValue))
    }

    @Test
    fun `onRestrictionDetailsInputChanged should update otherRestrictionInput with invalid input`() = testBlocking {
        val newValue = ""
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onRestrictionDetailsInputChanged(newValue)
        assertThat(
            capturedViewState?.otherRestrictionInput
        ).isInstanceOf(InputValue.Error::class.java)
    }
}
