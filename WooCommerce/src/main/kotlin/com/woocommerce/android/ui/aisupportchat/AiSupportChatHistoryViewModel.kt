package com.woocommerce.android.ui.aisupportchat

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSupportChatHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SupportChatRepository
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(AiSupportChatHistoryViewState())
    val viewState = _viewState.asStateFlow()

    fun loadHistory() {
        if (_viewState.value.isLoading) return

        _viewState.update { it.copy(isLoading = true, showError = false) }
        launch {
            runCatching { repository.loadChatHistory() }
                .onSuccess { bookmarks ->
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            bookmarks = bookmarks,
                            showError = false
                        )
                    }
                }
                .onFailure { error ->
                    WooLog.e(WooLog.T.AI, "Loading AI support chat history failed", error)
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            showError = true
                        )
                    }
                }
        }
    }
}

data class AiSupportChatHistoryViewState(
    val isLoading: Boolean = false,
    val bookmarks: List<SupportChatBookmark> = emptyList(),
    val showError: Boolean = false
)
