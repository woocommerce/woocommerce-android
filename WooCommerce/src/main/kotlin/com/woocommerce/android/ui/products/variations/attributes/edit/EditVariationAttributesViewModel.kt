package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.pairMap
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class EditVariationAttributesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val productRepository: ProductDetailRepository,
    private val variationRepository: VariationDetailRepository
) : ScopedViewModel(savedState) {
    private val _editableVariationAttributeList =
        MutableLiveData<List<VariationAttributeSelectionGroup>>()

    val editableVariationAttributeList = _editableVariationAttributeList

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private val selectedVariationDeferred = async(start = LAZY) {
        variationRepository.getVariation(
            viewState.parentProductID,
            viewState.editableVariationID
        )
    }

    private val parentProduct by lazy { productRepository.getProduct(viewState.parentProductID) }

    private val hasChanges
        get() = editableVariationAttributeList.value?.toTypedArray()
            ?.contentDeepEquals(viewState.updatedAttributeSelection.toTypedArray())
            ?: false

    fun start(productId: Long, variationId: Long) {
        viewState = viewState.copy(
            parentProductID = productId,
            editableVariationID = variationId
        )
        loadProductAttributes()
    }

    fun exit() {
        if (hasChanges) {
            triggerEvent(
                ExitWithResult(
                    viewState.updatedAttributeSelection
                        .mapNotNull { it.toVariantOption() }
                        .toTypedArray()
                )
            )
        } else {
            triggerEvent(Exit)
        }
    }

    fun updateData(attributeSelection: List<VariationAttributeSelectionGroup>) {
        viewState = viewState.copy(updatedAttributeSelection = attributeSelection)
    }

    private fun loadProductAttributes() =
        viewState.copy(isSkeletonShown = true).let { viewState = it }.also {
            launch(context = dispatchers.computation) {
                parentProduct?.variationEnabledAttributes
                    ?.pairAttributeWithSelectedOption()
                    ?.pairAttributeWithUnselectedOption()
                    ?.mapToAttributeSelectionGroupList()
                    ?.dispatchListResult()
                    ?: updateSkeletonVisibility(visible = false)
            }
        }

    private suspend fun List<ProductAttribute>.pairAttributeWithSelectedOption() =
        mapNotNull { attribute ->
            selectedVariationDeferred.await()?.attributes
                ?.find { it.name == attribute.name }
                ?.let { Pair(attribute, it) }
        }

    private fun List<Pair<ProductAttribute, VariantOption>>.pairAttributeWithUnselectedOption() =
        map { it.first }.let { selectedAttributes ->
            parentProduct?.variationEnabledAttributes
                ?.filter { selectedAttributes.contains(it).not() }
                ?.map { it to VariantOption.empty }
                ?.let { toMutableList().apply { addAll(it) } }
        }

    private fun List<Pair<ProductAttribute, VariantOption>>.mapToAttributeSelectionGroupList() =
        pairMap { productAttribute, selectedOption ->
            VariationAttributeSelectionGroup(
                id = productAttribute.id,
                attributeName = productAttribute.name,
                options = productAttribute.terms,
                selectedOptionIndex = productAttribute.terms.indexOf(selectedOption.option),
                noOptionSelected = selectedOption.option.isNullOrEmpty()
            )
        }

    private suspend fun List<VariationAttributeSelectionGroup>.dispatchListResult() =
        withContext(dispatchers.main) {
            editableVariationAttributeList.value = this@dispatchListResult
            updateSkeletonVisibility(visible = false)
        }

    private suspend fun updateSkeletonVisibility(visible: Boolean) =
        withContext(dispatchers.main) {
            viewState = viewState.copy(isSkeletonShown = visible)
        }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val parentProductID: Long = 0L,
        val editableVariationID: Long = 0L,
        val updatedAttributeSelection: List<VariationAttributeSelectionGroup> = emptyList()
    ) : Parcelable
}
