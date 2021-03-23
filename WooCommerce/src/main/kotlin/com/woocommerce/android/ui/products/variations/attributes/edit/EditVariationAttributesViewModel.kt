package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.pairMap
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditVariationAttributesViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productRepository: ProductDetailRepository,
    private val variationRepository: VariationDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val _editableVariationAttributeList =
        MutableLiveData<List<VariationAttributeSelectionGroup>>()

    val editableVariationAttributeList = _editableVariationAttributeList

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    fun start(productId: Long, variationId: Long) {
        viewState = viewState.copy(
            parentProduct = productRepository.getProduct(productId),
            editableVariation = variationRepository.getVariation(productId, variationId)
        )
        loadProductAttributes(productId)
    }

    private fun loadProductAttributes(productId: Long) = launch {
        productRepository.fetchProduct(productId)?.attributes?.mapNotNull { attribute ->
            viewState.editableVariation?.attributes
                ?.find { it.name == attribute.name }
                ?.let { Pair(attribute, it) }
        }?.pairMap { productAttribute, selectedOption ->
            VariationAttributeSelectionGroup(
                attributeName = productAttribute.name,
                options = productAttribute.terms,
                selectedOptionIndex = productAttribute.terms.indexOf(selectedOption.option)
            )
        }?.let {
            withContext(dispatchers.main) {
                editableVariationAttributeList.value = it
            }
        }
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val parentProduct: Product? = null,
        val editableVariation: ProductVariation? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<EditVariationAttributesViewModel>
}
