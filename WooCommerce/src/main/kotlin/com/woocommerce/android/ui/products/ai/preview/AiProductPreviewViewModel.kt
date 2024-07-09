package com.woocommerce.android.ui.products.ai.preview

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class AiProductPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    val state: LiveData<State> = flowOf(
        State.Success(
            AIProductModel.buildDefault(
                name = "Product Name",
                description = "Product Description"
            ),
            emptyList()
        )
    ).asLiveData()

    @Suppress("UNUSED_PARAMETER")
    fun onFeedbackReceived(positive: Boolean) {
        TODO()
    }

    sealed interface State {
        data object Loading : State
        data class Success(
            private val product: AIProductModel,
            val propertyGroups: List<List<ProductPropertyCard>>,
            val shouldShowFeedbackView: Boolean = true
        ) : State {
            val title: String
                get() = product.name
            val description: String
                get() = product.description
            val shortDescription: String
                get() = product.shortDescription
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
