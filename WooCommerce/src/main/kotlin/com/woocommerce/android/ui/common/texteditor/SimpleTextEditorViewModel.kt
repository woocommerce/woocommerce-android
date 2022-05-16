package com.woocommerce.android.ui.common.texteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SimpleTextEditorViewModel @Inject constructor(savedState: SavedStateHandle) : ScopedViewModel(savedState) {
    companion object {
        private const val TEXT_KEY = "text"
    }

    private val navArgs: SimpleTextEditorFragmentArgs by savedState.navArgs()
    private val textLiveData = savedState.getLiveData<String>(TEXT_KEY, navArgs.currentText)
    val viewState = textLiveData.map {
        ViewState(
            text = it,
            hint = navArgs.hint,
            hasChanges = it != navArgs.currentText
        )
    }

    fun onTextChanged(text: String) {
        textLiveData.value = text
    }

    fun onBackPressed() {
        val event = viewState.value?.takeIf { it.hasChanges }?.let { viewState ->
            ExitWithResult(
                SimpleTextEditorResult(
                    text = viewState.text,
                    requestCode = navArgs.requestCode.takeIf { it != -1 }
                )
            )
        } ?: Exit

        triggerEvent(event)
    }

    data class ViewState(
        val text: String,
        val hint: String,
        val hasChanges: Boolean
    )

    data class SimpleTextEditorResult(
        val text: String,
        val requestCode: Int?
    )
}
