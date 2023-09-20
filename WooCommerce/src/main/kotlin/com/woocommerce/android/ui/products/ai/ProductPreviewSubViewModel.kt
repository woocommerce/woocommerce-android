package com.woocommerce.android.ui.products.ai

import androidx.annotation.DrawableRes
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf

class ProductPreviewSubViewModel(
    override val onDone: (Unit) -> Unit
) : AddProductWithAISubViewModel<Unit> {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val state = flowOf<State>(State.Loading).asLiveData()

    override fun close() {
        viewModelScope.cancel()
    }

    sealed interface State {
        object Loading : State
        data class Success(
            val title: String,
            val description: String,
            val propertyGroups: List<List<ProductPropertyCard>>
        ) : State
    }

    data class ProductPropertyCard(
        @DrawableRes val icon: Int,
        val title: String,
        val content: String
    )
}
