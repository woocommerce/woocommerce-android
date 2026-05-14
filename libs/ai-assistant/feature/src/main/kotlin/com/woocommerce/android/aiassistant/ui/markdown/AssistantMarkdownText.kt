package com.woocommerce.android.aiassistant.ui.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun AssistantMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    linkColor: Color = MaterialTheme.colorScheme.primary,
) {
    val uriHandler = LocalUriHandler.current
    val linkInteractionListener = remember(uriHandler) {
        LinkInteractionListener { linkAnnotation ->
            val url = (linkAnnotation as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
            uriHandler.openUri(url)
        }
    }
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
        )
    )
    val annotatedText = remember(text, linkColor, linkInteractionListener) {
        AssistantMarkdownParser.parse(
            markdown = text,
            linkStyles = linkStyles,
            linkInteractionListener = linkInteractionListener,
        )
    }

    Text(
        text = annotatedText,
        modifier = modifier,
        color = color,
        style = style,
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun AssistantMarkdownTextPreview() {
    AssistantMarkdownText(
        text = "Read the [WooCommerce mobile documentation](https://woocommerce.com/documentation/woocommerce/mobile/) " +
            "for **setup** and *support*.",
    )
}
