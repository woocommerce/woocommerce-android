package com.woocommerce.android.ui.compose.component

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.databinding.ViewAztecBinding
import com.woocommerce.android.databinding.ViewAztecOutlinedBinding
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.flow.drop
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecText
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.glideloader.GlideImageLoader
import org.wordpress.aztec.source.SourceViewEditText
import org.wordpress.aztec.toolbar.IAztecToolbar
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener

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
    InternalAztecEditor(
        content = content,
        onContentChanged = onContentChanged,
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
    InternalAztecEditor(
        content = content,
        onContentChanged = onContentChanged,
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


@Composable
private fun InternalAztecEditor(
    content: String,
    onContentChanged: (String) -> Unit,
    aztecViewsProvider: (context: Context) -> AztecViewsHolder,
    modifier: Modifier = Modifier,
    label: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    calypsoMode: Boolean = false
) {
    val localContext = LocalContext.current
    var htmlMode by rememberSaveable { mutableStateOf(true) }

    val viewsHolder = remember(LocalContext.current) { aztecViewsProvider(localContext) }
    val listener = remember { createToolbarListener { htmlMode = !htmlMode } }
    val aztec = remember(LocalContext.current) {
        Aztec.with(viewsHolder.visualEditor, viewsHolder.sourceEditor, viewsHolder.toolbar, listener)
            .setImageGetter(GlideImageLoader(localContext))
    }

    LaunchedEffect(Unit) {
        snapshotFlow { htmlMode }
            .drop(1)
            .collect {
                aztec.toolbar.toggleEditorMode()
            }
    }

    val contentState by rememberUpdatedState(content)

    AndroidView(
        factory = {
            aztec.visualEditor.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // Because the editors could have different number of lines, we don't set the minLines
                // of the source editor, so we set the minHeight instead to match the visual editor
                aztec.sourceEditor?.minHeight = aztec.visualEditor.height
            }

            aztec.visualEditor.doAfterTextChanged {
                if (!htmlMode) return@doAfterTextChanged
                aztec.visualEditor.toHtml().takeIf { it != contentState }?.let {
                    onContentChanged(it)
                }
            }
            aztec.sourceEditor?.doAfterTextChanged {
                if (htmlMode) return@doAfterTextChanged
                aztec.sourceEditor?.getPureHtml()?.takeIf { it != contentState }?.let {
                    onContentChanged(it)
                }
            }

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

            if (htmlMode) {
                if (aztec.visualEditor.toHtml() != content) {
                    aztec.visualEditor.fromHtml(content)
                }
            } else {
                if (aztec.sourceEditor?.getPureHtml() != content) {
                    aztec.sourceEditor?.displayStyledAndFormattedHtml(content)
                }
            }
        },
        modifier = modifier
    )
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
    var content by remember { mutableStateOf("") }
    WooThemeWithBackground {
        OutlinedAztecEditor(
            content = content,
            onContentChanged = {
                content = it
            },
            label = "Label",
            minLines = 5,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
@Preview
private fun AztecEditorPreview() {
    var content by remember { mutableStateOf("") }
    WooThemeWithBackground {
        AztecEditor(
            content = content,
            onContentChanged = {
                content = it
            },
            label = "Label",
            minLines = 5,
            modifier = Modifier
        )
    }
}
