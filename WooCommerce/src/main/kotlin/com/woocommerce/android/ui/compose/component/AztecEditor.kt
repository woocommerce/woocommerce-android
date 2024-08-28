package com.woocommerce.android.ui.compose.component

import android.view.LayoutInflater
import android.widget.EditText
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.databinding.ViewAztecOutlinedBinding
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.flow.drop
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener

@Composable
fun AztecEditor(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE
) {
    val localContext = LocalContext.current

    var htmlMode by remember { mutableStateOf(true) }

    val listener = remember { createToolbarListener { htmlMode = !htmlMode } }

    val viewBinding = remember(LocalContext.current) {
        ViewAztecOutlinedBinding.inflate(LayoutInflater.from(localContext)).apply {
            visualEditor.background = null
            sourceEditor.background = null
        }
    }

    val aztec = remember(LocalContext.current) { initAztec(viewBinding, listener) }

    LaunchedEffect(Unit) {
        snapshotFlow { htmlMode }
            .drop(1)
            .collect {
                aztec.toolbar.toggleEditorMode()
            }
    }

    AndroidView(
        factory = {
            aztec.visualEditor.doOnTextChanged { _, _, _, _ ->
                aztec.visualEditor.toHtml().takeIf { it != content }?.let {
                    onContentChanged(it)
                }
            }
            aztec.sourceEditor?.doOnTextChanged { _, _, _, _ ->
                aztec.sourceEditor?.getPureHtml()?.takeIf { it != content }?.let {
                    onContentChanged(it)
                }
            }

            viewBinding.root
        },
        update = {
            if (minLines != -1) {
                aztec.visualEditor.minLines = minLines
                aztec.sourceEditor?.minLines = minLines
            }
            if (maxLines != Int.MAX_VALUE) {
                aztec.visualEditor.maxLines = maxLines
                aztec.sourceEditor?.maxLines = maxLines
            }

            aztec.visualEditor.label = label
            aztec.sourceEditor?.label = label

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

private fun initAztec(binding: ViewAztecOutlinedBinding, listener: IAztecToolbarClickListener): Aztec {
    return Aztec.with(binding.visualEditor, binding.sourceEditor, binding.toolbar, listener)
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
            modifier = Modifier.padding(16.dp)
        )
    }
}
