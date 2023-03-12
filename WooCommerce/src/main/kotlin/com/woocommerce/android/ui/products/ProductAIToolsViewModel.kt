package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ai.AIPrompts
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductAIToolsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val aiRepository: AIRepository,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState) {

    private val navArgs: ProductAIToolsFragmentArgs by savedState.navArgs()
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
                        title = resourceProvider.getString(R.string.ai_product_tools_generate_tweet),
                        description = resourceProvider.getString(R.string.ai_product_tools_generate_tweet_description),
                    )
                )
            )
        }
    }

    fun onGenerateTweetClicked() {
        val name = navArgs.productName
        if(name.isEmpty()) {
            triggerEvent(ShowSnackbar(R.string.ai_product_missing_name_error))
        }
        else {
            launch {
                val result = aiRepository.openAIGenerateChat(
                    AIPrompts.GENERATE_PROMO_TWEET_FROM_PRODUCT_TITLE + name
                )
                triggerEvent(ProductNavigationTarget.NavigateToAIResult(
                    result,
                    R.string.ai_product_details_generate_tweet_heading
                ))
            }
        }

    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val options: List<AIToolOption> = emptyList()
    ) : Parcelable

    @Parcelize
    data class AIToolOption(
        val title: String,
        val description: String,
    ) : Parcelable
}
