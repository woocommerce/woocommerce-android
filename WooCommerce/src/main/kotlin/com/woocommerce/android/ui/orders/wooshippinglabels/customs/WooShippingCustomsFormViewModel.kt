package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooShippingCustomsFormViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    fun onContentTypeClick() {
        val currentSelection = _viewState.value.contentType
        triggerEvent(ShowContentTypeDialog(currentSelection))
    }

    fun onRestrictionTypeClick() {
        val currentSelection = _viewState.value.restrictionType
        triggerEvent(ShowRestrictionTypeDialog(currentSelection))
    }

    fun onITNChanged(newItnValue: String) {
        _viewState.update {
            it.copy(itnValue = newItnValue)
        }
    }

    fun onReturnToSenderChanged(isChecked: Boolean) {
        _viewState.update {
            it.copy(returnToSenderChecked = isChecked)
        }
    }

    fun onContentTypeSelected(contentType: ContentType) {
        _viewState.update {
            it.copy(contentType = contentType)
        }
    }

    fun onRestrictionTypeSelected(restrictionType: RestrictionType) {
        _viewState.update {
            it.copy(restrictionType = restrictionType)
        }
    }

    fun onOtherContentInputChanged(newValue: String) {
        _viewState.update {
            it.copy(otherContentInput = newValue)
        }
    }

    fun onRestrictionDetailsInputChanged(newValue: String) {
        _viewState.update {
            it.copy(otherRestrictionInput = newValue)
        }
    }

    @Parcelize
    data class ViewState(
        val contentType: ContentType = ContentType.MERCHANDISE,
        val otherContentInput: String = "",
        val restrictionType: RestrictionType = RestrictionType.NONE,
        val otherRestrictionInput: String = "",
        val itnValue: String = "",
        val returnToSenderChecked: Boolean = false,
        val isAddCustomsButtonEnabled: Boolean = false
    ) : Parcelable {
        val shouldDisplayContentTypeInput: Boolean
            get() = contentType == ContentType.OTHER

        val shouldDisplayRestrictionTypeInput: Boolean
            get() = restrictionType == RestrictionType.OTHER
    }

    enum class ContentType(val resourceId: Int) {
        MERCHANDISE(R.string.woo_shipping_labels_customs_content_merchandise),
        GIFT(R.string.woo_shipping_labels_customs_content_gift),
        RETURNED_GOODS(R.string.woo_shipping_labels_customs_content_returned_goods),
        SAMPLE(R.string.woo_shipping_labels_customs_content_sample),
        DOCUMENTS(R.string.woo_shipping_labels_customs_content_documents),
        OTHER(R.string.woo_shipping_labels_customs_content_other)
    }

    enum class RestrictionType(val resourceId: Int) {
        NONE(R.string.woo_shipping_labels_customs_restriction_none),
        QUARANTINE(R.string.woo_shipping_labels_customs_restriction_quarantine),
        SANITARY_INSPECTION(R.string.woo_shipping_labels_customs_restriction_sanitary),
        OTHER(R.string.woo_shipping_labels_customs_restriction_other)
    }

    data class ShowContentTypeDialog(val currentSelection: ContentType) : MultiLiveEvent.Event()
    data class ShowRestrictionTypeDialog(val currentSelection: RestrictionType) : MultiLiveEvent.Event()
}
