package com.woocommerce.android.ui.products.ai

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.model.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ProductPreviewSubViewModel(
    private val aiRepository: AIRepository,
    private val buildProductPreviewProperties: BuildProductPreviewProperties,
    override val onDone: (Product) -> Unit,
) : AddProductWithAISubViewModel<Product> {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asLiveData()

    fun startGeneratingProduct(name: String, keywords: String) {
        viewModelScope.launch {
            _state.value = State.Loading
            aiRepository.generateProduct(name, keywords).fold(
                onSuccess = { product ->
                    _state.value = State.Success(
                        product = product,
                        propertyGroups = buildProductPreviewProperties(product)
                    )
                    onDone(product)
                },
                onFailure = {
                    TODO()
                }
            )
        }
    }

    override fun close() {
        viewModelScope.cancel()
    }

    sealed interface State {
        object Loading : State
        data class Success(
            private val product: Product,
            val propertyGroups: List<List<ProductPropertyCard>>
        ) : State {
            val title: String
                get() = product.name
            val description: String
                get() = product.description
        }
    }

    data class ProductPropertyCard(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        val content: String
    )
}
