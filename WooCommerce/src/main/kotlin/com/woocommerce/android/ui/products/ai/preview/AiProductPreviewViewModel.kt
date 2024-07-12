package com.woocommerce.android.ui.products.ai.preview

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.ui.products.ai.BuildProductPreviewProperties
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class AiProductPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buildProductPreviewProperties: BuildProductPreviewProperties
) : ScopedViewModel(savedStateHandle) {
    val state: LiveData<State> = flow {
        // TODO Replace with actual AI generation
        val productModel = AIProductModel.buildDefault(
            name = "Product Name",
            description = "Product Description"
        )

        val propertyGroups = buildProductPreviewProperties(
            productModel.toProduct(
                variant = 0,
                existingCategories = emptyList(),
                existingTags = emptyList()
            )
        )

        emit(
            State.Success(
                selectedVariant = 0,
                product = productModel,
                propertyGroups = propertyGroups.map { group ->
                    group.map { property ->
                        ProductPropertyCard(
                            icon = property.icon,
                            title = property.title,
                            content = property.content
                        )
                    }
                }
            )
        )
    }.asLiveData()

    @Suppress("UNUSED_PARAMETER")
    fun onFeedbackReceived(positive: Boolean) {
        TODO()
    }

    fun onBackButtonClick() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    sealed interface State {
        data object Loading : State
        data class Success(
            private val selectedVariant: Int,
            private val product: AIProductModel,
            val propertyGroups: List<List<ProductPropertyCard>>,
            val shouldShowFeedbackView: Boolean = true
        ) : State {
            val title: String
                get() = product.names[selectedVariant]
            val description: String
                get() = product.descriptions[selectedVariant]
            val shortDescription: String
                get() = product.shortDescriptions[selectedVariant]
        }

        data class Error(
            val onRetryClick: () -> Unit,
            val onDismissClick: () -> Unit
        ) : State
    }

    data class ProductPropertyCard(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        val content: String
    )
}
