package com.woocommerce.android.ui.compose.component

import android.content.Context
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
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
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.flow.drop
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecText
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.source.SourceViewEditText
import org.wordpress.aztec.toolbar.AztecToolbar
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener

@Composable
fun AztecEditor(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE
) {
    val localContext = LocalContext.current

    var htmlMode by remember { mutableStateOf(true) }

    val listener = remember { createToolbarListener { htmlMode = !htmlMode } }

    val aztec = remember(LocalContext.current) { initAztec(localContext, listener) }

    LaunchedEffect(Unit) {
        snapshotFlow { htmlMode }
            .drop(1)
            .collect {
                aztec.toolbar.toggleEditorMode()
            }
    }

    AndroidView(
        factory = { context ->
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

            createLayout(context, aztec)
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

private fun initAztec(context: Context, listener: IAztecToolbarClickListener): Aztec {
    val visualEditor = AztecText(context).apply {
        gravity = android.view.Gravity.TOP
    }
    val sourceEditor = SourceViewEditText(context).apply {
        gravity = android.view.Gravity.TOP
        visibility = android.view.View.GONE
    }
    val toolbar = AztecToolbar(context)

    return Aztec.with(visualEditor, sourceEditor, toolbar, listener)
}

private fun createLayout(context: Context, aztec: Aztec) = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    val editorLayout = FrameLayout(context)
    editorLayout.addView(aztec.visualEditor)
    aztec.visualEditor.updateLayoutParams<FrameLayout.LayoutParams> {
        width = FrameLayout.LayoutParams.MATCH_PARENT
        height = FrameLayout.LayoutParams.MATCH_PARENT
    }
    editorLayout.addView(aztec.sourceEditor)
    aztec.sourceEditor?.updateLayoutParams<FrameLayout.LayoutParams> {
        width = FrameLayout.LayoutParams.MATCH_PARENT
        height = FrameLayout.LayoutParams.MATCH_PARENT
    }
    addView(editorLayout)

    val toolbar = aztec.toolbar as AztecToolbar
    addView(toolbar)
    toolbar.updateLayoutParams<LinearLayout.LayoutParams> {
        width = FrameLayout.LayoutParams.MATCH_PARENT
        height = FrameLayout.LayoutParams.WRAP_CONTENT
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

@Composable
@Preview
private fun AztecEditorPreview() {
    var content by remember { mutableStateOf("<h1>Heading</h1>") }
    AztecEditor(
        content = content,
        onContentChanged = {
            content = it
        },
        minLines = 5,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface,
                shape = RoundedCornerShape(8.dp)
            )
    )
}
