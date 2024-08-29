package com.woocommerce.android.ui.compose.component.aztec

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class AztecEditorState(
    initialContent: String
) {
    var content by mutableStateOf(initialContent)
        private set

    var isHtmlEditorEnabled by mutableStateOf(true)
        private set

    fun updateContent(newContent: String) {
        content = newContent
    }

    fun toggleHtmlEditor() {
        isHtmlEditorEnabled = !isHtmlEditorEnabled
    }

    companion object {
        fun Saver() = Saver<AztecEditorState, Any>(
            save = { arrayListOf(it.content, it.isHtmlEditorEnabled) },
            restore = {
                val list = it as List<*>
                AztecEditorState(list[0] as String).apply {
                    isHtmlEditorEnabled = list[1] as Boolean
                }
            }
        )
    }
}

/**
 * Remember a [AztecEditorState] that can be used to manage the state of an Aztec editor.
 * The state will be saved to the saved state to survive process death.
 *
 * @param initialContent The initial content of the editor
 */
@Composable
fun rememberAztecEditorState(
    initialContent: String
): AztecEditorState {
    return rememberSaveable(saver = AztecEditorState.Saver()) { AztecEditorState(initialContent) }
}
