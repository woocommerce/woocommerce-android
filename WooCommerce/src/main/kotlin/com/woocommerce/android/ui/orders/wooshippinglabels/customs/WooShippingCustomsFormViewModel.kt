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
    private val itnRegex by lazy { ITN_REGEX_STRING.toRegex() }

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
        val input = when (newValue.isBlank()) {
            false -> InputValue.Data(newValue)
            true -> InputValue.Error(
                input = newValue,
                errorMessageId = R.string.woo_shipping_labels_customs_other_error_message
            )
        }
        _viewState.update {
            it.copy(otherContentInput = input)
        }
    }

    fun onRestrictionDetailsInputChanged(newValue: String) {
        val input = when (newValue.isBlank()) {
            false -> InputValue.Data(newValue)
            true -> InputValue.Error(
                input = newValue,
                errorMessageId = R.string.woo_shipping_labels_customs_other_error_message
            )
        }
        _viewState.update {
            it.copy(otherRestrictionInput = input)
        }
    }

    fun onITNChanged(newItnValue: String) {
        val input = when {
            newItnValue.isBlank() -> InputValue.Empty
            itnRegex.matches(newItnValue).not() ->
                InputValue.Error(
                    input = newItnValue,
                    errorMessageId = R.string.woo_shipping_labels_customs_itn_error_message
                )
            else -> InputValue.Data(newItnValue)
        }
        _viewState.update { it.copy(itnValue = input) }
    }

    @Parcelize
    data class ViewState(
        val contentType: ContentType = ContentType.MERCHANDISE,
        val otherContentInput: InputValue = InputValue.Empty,
        val restrictionType: RestrictionType = RestrictionType.NONE,
        val otherRestrictionInput: InputValue = InputValue.Empty,
        val itnValue: InputValue = InputValue.Empty,
        val returnToSenderChecked: Boolean = false
    ) : Parcelable {
        val shouldDisplayContentTypeInput: Boolean
            get() = contentType == ContentType.OTHER

        val shouldDisplayRestrictionTypeInput: Boolean
            get() = restrictionType == RestrictionType.OTHER

        val isAddCustomsButtonEnabled: Boolean
            get() = itnValue is InputValue.Data &&
                (contentType != ContentType.OTHER || otherContentInput is InputValue.Data) &&
                (restrictionType != RestrictionType.OTHER || otherRestrictionInput is InputValue.Data)
    }

    @Parcelize
    sealed class InputValue : Parcelable {
        data class Data(val input: String) : InputValue()
        data class Error(
            val input: String,
            val errorMessageId: Int
        ) : InputValue()
        data object Empty : InputValue()

        val currentInput
            get() = when (this) {
                is Data -> input
                is Error -> input
                is Empty -> ""
            }

        val errorMessageOrNull: Int?
            get() = run { this as? Error }?.errorMessageId
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

    companion object {
        /**
         * For information regarding the format of the ITN, check the Appendix A of
         * [Export Compliance Customs Data Requirements](https://postalpro.usps.com/node/3973)
         */
        private const val ITN_REGEX_STRING =
            """^(?:(?:AES X\d{14})|(?:NOEEI 30\.\d{1,2}(?:\([a-z]\)(?:\(\d\))?)?))${'$'}"""
    }
}
