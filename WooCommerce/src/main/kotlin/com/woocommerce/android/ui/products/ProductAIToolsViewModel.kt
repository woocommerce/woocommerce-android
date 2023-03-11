package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ProductAIToolsViewModel @Inject constructor(
    private val resProvider: ResourceProvider,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val viewStateFlow = savedState.getStateFlow(
        viewModelScope,
        ViewState()
    )
    val viewState = viewStateFlow.asLiveData()
    init {
        viewStateFlow.update {
            it.copy(
                options = listOf(
                    AIToolOption(
                        title = resProvider.getString(R.string.ai_product_tools_generate_tweet),
                        description = resProvider.getString(R.string.ai_product_tools_generate_tweet_description),
                    )
                )
            )
        }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    data class ViewState(
        val options: List<AIToolOption> = emptyList()
    )
    data class AIToolOption(
        val title: String,
        val description: String,
    )
}
