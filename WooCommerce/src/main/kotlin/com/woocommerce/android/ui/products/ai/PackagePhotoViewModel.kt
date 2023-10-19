package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Initial
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.Keyword
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class PackagePhotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(
        ViewState()
    )

    val viewState = _viewState.asLiveData()

    fun onKeywordChanged(index: Int, keyword: Keyword) {
        _viewState.value = _viewState.value.copy(
            keywords = _viewState.value.keywords.mapIndexed { i, old ->
                if (i == index) {
                    old.copy(title = keyword.title, isChecked = keyword.isChecked)
                } else {
                    old
                }
            }
        )
    }

    data class ViewState(
        val imageUrl: String = "",
        val title: String = "Title",
        val description: String = "Description",
        val keywords: List<Keyword> = listOf(Keyword("Keyword 1", true), Keyword("Keyword 2", false)),
        val generationState: GenerationState = Initial
    ) {
        data class Keyword(val title: String, val isChecked: Boolean)

        sealed class GenerationState {
            object Initial : GenerationState()
            object Scanning : GenerationState()
            object Generating : GenerationState()
            object Success : GenerationState()
            data class Failure(val error: String) : GenerationState()
        }
    }
}
