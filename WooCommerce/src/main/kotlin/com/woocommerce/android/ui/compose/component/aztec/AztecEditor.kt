package com.woocommerce.android.ui.compose.component.aztec

import android.content.Context
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.databinding.ViewAztecBinding
import com.woocommerce.android.databinding.ViewAztecOutlinedBinding
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecText
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.glideloader.GlideImageLoader
import org.wordpress.aztec.source.SourceViewEditText
import org.wordpress.aztec.toolbar.IAztecToolbar
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener

/**
 * An Aztec editor that can be used in Compose, with an outlined style.
 *
 * @param content The content of the editor
 * @param onContentChanged A callback that will be called when the content of the editor changes
 * @param modifier The modifier to apply to the editor
 * @param label The label to display above the editor
 * @param minLines The minimum number of lines the editor should have
 * @param maxLines The maximum number of lines the editor should have
 * @param calypsoMode Whether the editor should be in calypso mode, for more information on calypso mode see https://github.com/wordpress-mobile/AztecEditor-Android/pull/309
 */
@Composable
fun OutlinedAztecEditor(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    calypsoMode: Boolean = false
) {
    val state = rememberAztecEditorState(initialContent = content)
    val contentState by rememberUpdatedState(content)

    LaunchedEffect(Unit) {
        snapshotFlow { contentState }
            .onEach { state.updateContent(it) }
            .launchIn(this)

        snapshotFlow { state.content }
            .onEach { onContentChanged(it) }
            .launchIn(this)
    }

    OutlinedAztecEditor(
        state = state,
        modifier = modifier,
        label = label,
        minLines = minLines,
        maxLines = maxLines,
        calypsoMode = calypsoMode
    )
}

/**
 * An Aztec editor that can be used in Compose, with an outlined style.
 *
 * @param state The state of the editor, see [rememberAztecEditorState]
 * @param modifier The modifier to apply to the editor
 * @param label The label to display above the editor
 * @param minLines The minimum number of lines the editor should have
 * @param maxLines The maximum number of lines the editor should have
 * @param calypsoMode Whether the editor should be in calypso mode, for more information on calypso mode see https://github.com/wordpress-mobile/AztecEditor-Android/pull/309
 */
@Composable
fun OutlinedAztecEditor(
    state: AztecEditorState,
    modifier: Modifier = Modifier,
    label: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    calypsoMode: Boolean = false
) {
    InternalAztecEditor(
        state = state,
        aztecViewsProvider = { context ->
            val binding = ViewAztecOutlinedBinding.inflate(LayoutInflater.from(context)).apply {
                visualEditor.background = null
                sourceEditor.background = null
            }

            AztecViewsHolder(
                layout = binding.root,
                visualEditor = binding.visualEditor,
                sourceEditor = binding.sourceEditor,
                toolbar = binding.toolbar
            )
        },
        modifier = modifier,
        label = label,
        minLines = minLines,
        maxLines = maxLines,
        calypsoMode = calypsoMode
    )
}

/**
 * An Aztec editor that can be used in Compose.
 *
 * @param content The content of the editor
 * @param onContentChanged A callback that will be called when the content of the editor changes
 * @param modifier The modifier to apply to the editor
 * @param label The label to display above the editor
 * @param minLines The minimum number of lines the editor should have
 * @param maxLines The maximum number of lines the editor should have
 * @param calypsoMode Whether the editor should be in calypso mode, for more information on calypso mode see https://github.com/wordpress-mobile/AztecEditor-Android/pull/309
 */
@Composable
fun AztecEditor(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    calypsoMode: Boolean = false
) {
    val state = rememberAztecEditorState(initialContent = content)
    val contentState by rememberUpdatedState(content)

    LaunchedEffect(Unit) {
        snapshotFlow { contentState }
            .onEach { state.updateContent(it) }
            .launchIn(this)

        snapshotFlow { state.content }
            .onEach { onContentChanged(it) }
            .launchIn(this)
    }

    AztecEditor(
        state = state,
        modifier = modifier,
        label = label,
        minLines = minLines,
        maxLines = maxLines,
        calypsoMode = calypsoMode
    )
}

/**
 * An Aztec editor that can be used in Compose.
 *
 * @param state The state of the editor, see [rememberAztecEditorState]
 * @param modifier The modifier to apply to the editor
 * @param label The label to display above the editor
 * @param minLines The minimum number of lines the editor should have
 * @param maxLines The maximum number of lines the editor should have
 * @param calypsoMode Whether the editor should be in calypso mode, for more information on calypso mode see https://github.com/wordpress-mobile/AztecEditor-Android/pull/309
 */
@Composable
fun AztecEditor(
    state: AztecEditorState,
    modifier: Modifier = Modifier,
    label: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    calypsoMode: Boolean = false
) {
    InternalAztecEditor(
        state = state,
        aztecViewsProvider = { context ->
            val binding = ViewAztecBinding.inflate(LayoutInflater.from(context))

            AztecViewsHolder(
                layout = binding.root,
                visualEditor = binding.visualEditor,
                sourceEditor = binding.sourceEditor,
                toolbar = binding.toolbar
            )
        },
        modifier = modifier,
        label = label,
        minLines = minLines,
        maxLines = maxLines,
        calypsoMode = calypsoMode
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun InternalAztecEditor(
    state: AztecEditorState,
    aztecViewsProvider: (context: Context) -> AztecViewsHolder,
    modifier: Modifier = Modifier,
    label: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    calypsoMode: Boolean = false
) {
    val localContext = LocalContext.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val textInputService = LocalTextInputService.current

    val viewsHolder = remember(localContext) { aztecViewsProvider(localContext) }
    val listener = remember { createToolbarListener { state.toggleHtmlEditor() } }
    val aztec = remember(localContext) {
        Aztec.with(viewsHolder.visualEditor, viewsHolder.sourceEditor, viewsHolder.toolbar, listener)
            .setImageGetter(GlideImageLoader(localContext))
    }

    // Toggle the editor mode when the state changes
    LaunchedEffect(Unit) {
        snapshotFlow { state.isHtmlEditorEnabled }
            .drop(1) // Skip the initial value to avoid toggling the editor when it's first created
            .collect { aztec.toolbar.toggleEditorMode() }
    }

    // Update the content of the editor when the state changes
    LaunchedEffect(state.content) {
        if (state.isHtmlEditorEnabled) {
            if (aztec.visualEditor.toHtml() != state.content) {
                aztec.visualEditor.fromHtml(state.content)
            }
        } else {
            if (aztec.sourceEditor?.getPureHtml() != state.content) {
                aztec.sourceEditor?.displayStyledAndFormattedHtml(state.content)
            }
        }
    }

    val focusState = remember { MutableStateFlow(false) }
    val isImeVisible = rememberUpdatedState(WindowInsets.isImeVisible)

    LaunchedEffect(Unit) {
        handleFocus(
            focusState = focusState,
            imeVisibility = isImeVisible,
            bringIntoViewRequester = bringIntoViewRequester,
            textInputService = textInputService
        )
    }

    AndroidView(
        factory = {
            aztec.visualEditor.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // Because the editors could have different number of lines, we don't set the minLines
                // of the source editor, so we set the minHeight instead to match the visual editor
                aztec.sourceEditor?.minHeight = aztec.visualEditor.height
            }

            aztec.visualEditor.doAfterTextChanged {
                if (!state.isHtmlEditorEnabled) return@doAfterTextChanged
                state.updateContent(aztec.visualEditor.toHtml())
            }
            aztec.sourceEditor?.doAfterTextChanged {
                val sourceEditor = aztec.sourceEditor
                if (state.isHtmlEditorEnabled || sourceEditor == null) return@doAfterTextChanged
                state.updateContent(sourceEditor.getPureHtml())
            }

            val focusChangeListener = OnFocusChangeListener { _, focused ->
                focusState.value = focused
            }
            aztec.visualEditor.onFocusChangeListener = focusChangeListener
            aztec.sourceEditor?.onFocusChangeListener = focusChangeListener

            viewsHolder.layout
        },
        update = {
            if (aztec.visualEditor.isInCalypsoMode != calypsoMode) {
                aztec.visualEditor.isInCalypsoMode = calypsoMode
                aztec.sourceEditor?.setCalypsoMode(calypsoMode)
            }

            if (minLines != -1 && minLines != aztec.visualEditor.minLines) {
                aztec.visualEditor.minLines = minLines
            }
            if (maxLines != Int.MAX_VALUE && maxLines != aztec.visualEditor.maxLines) {
                aztec.visualEditor.maxLines = maxLines
                aztec.sourceEditor?.maxLines = maxLines
            }

            if (aztec.visualEditor.label != label) {
                aztec.visualEditor.label = label
                aztec.sourceEditor?.label = label
            }
        },
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
    )
}

@OptIn(ExperimentalFoundationApi::class, InternalTextApi::class)
private suspend fun handleFocus(
    focusState: StateFlow<Boolean>,
    imeVisibility: State<Boolean>,
    bringIntoViewRequester: BringIntoViewRequester,
    textInputService: TextInputService?
) = coroutineScope {
    launch(Dispatchers.Main.immediate) {
        // In Compose, text fields use input sessions to manage the input, when focus moves to a non-input field
        // the session is closed and this hides the keyboard.
        // This behavior doesn't work well when focus moves to a non-Compose input field, like the Aztec editor.
        // see: https://issuetracker.google.com/issues/318530776 and https://issuetracker.google.com/issues/363544352
        // To fix this, we are using the internal API to start/stop the input.
        // This is safe to do because even if the API changes, we can remove the logic temporarily until we find a
        // better solution, as this behavior is not critical for the editor.
        focusState.collect {
            if (it) {
                textInputService?.startInput()
            } else {
                textInputService?.stopInput()
            }
        }
    }

    launch {
        // Use collectLatest to make sure the nested collection is cancelled when the focus state changes
        focusState.collectLatest { hasFocus ->
            if (!hasFocus) return@collectLatest
            bringIntoViewRequester.bringIntoView()

            snapshotFlow { imeVisibility.value }
                .filter { it }
                .collect { bringIntoViewRequester.bringIntoView() }
        }
    }
}

private fun createToolbarListener(onHtmlButtonClicked: () -> Unit) = object : IAztecToolbarClickListener {
    override fun onToolbarCollapseButtonClicked() = Unit

    override fun onToolbarExpandButtonClicked() = Unit

    override fun onToolbarFormatButtonClicked(format: ITextFormat, isKeyboardShortcut: Boolean) = Unit

    override fun onToolbarHeadingButtonClicked() = Unit

    override fun onToolbarHtmlButtonClicked() {
        onHtmlButtonClicked()
    }

    override fun onToolbarListButtonClicked() = Unit

    override fun onToolbarMediaButtonClicked(): Boolean = false
}

/**
 * Helper to set the label of an [EditText] depending on whether it is wrapped in a [TextInputLayout]
 */
private var EditText.label
    get() = (parent?.parent as? TextInputLayout)?.hint ?: hint
    set(value) {
        (parent?.parent as? TextInputLayout)?.let { it.hint = value } ?: run {
            hint = value
        }
    }

private data class AztecViewsHolder(
    val layout: ViewGroup,
    val visualEditor: AztecText,
    val sourceEditor: SourceViewEditText,
    val toolbar: IAztecToolbar
)

@Composable
@Preview
private fun OutlinedAztecEditorPreview() {
    val state = rememberAztecEditorState("")

    WooThemeWithBackground {
        Column {
            OutlinedAztecEditor(
                state = state,
                label = "Label",
                minLines = 5,
                modifier = Modifier.padding(16.dp)
            )

            TextButton(onClick = { state.toggleHtmlEditor() }) {
                Text("Toggle Html Mode")
            }
        }
    }
}

@Composable
@Preview
private fun AztecEditorPreview() {
    val state = rememberAztecEditorState("")

    WooThemeWithBackground {
        Column {
            AztecEditor(
                state = state,
                label = "Label",
            )

            TextButton(onClick = { state.toggleHtmlEditor() }) {
                Text("Toggle Html Mode")
            }
        }
    }
}
