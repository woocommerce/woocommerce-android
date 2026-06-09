package com.woocommerce.android.ui.designsystem.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemTheme

@Composable
fun WooPageTitle(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        color = WooTheme.colors.background.onSection,
        style = WooTheme.text.headlineSmall.strong,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun WooBodyText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        color = WooTheme.colors.surface.onVariant,
        style = WooTheme.text.bodyMedium.regular,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun WooLinkedBodyText(
    text: AnnotatedString,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkColor = WooTheme.colors.primary
    val linkStyles = remember(linkColor) {
        TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
    val linkInteractionListener = remember(onLinkClick) {
        LinkInteractionListener { linkAnnotation ->
            onLinkClick(linkAnnotation.destination())
        }
    }
    val linkedText = remember(text, linkStyles, linkInteractionListener) {
        text.flatMapAnnotations { range ->
            when (val annotation = range.item) {
                is LinkAnnotation.Clickable -> {
                    listOf(
                        AnnotatedString.Range(
                            item = LinkAnnotation.Clickable(
                                tag = annotation.tag,
                                styles = linkStyles,
                                linkInteractionListener = linkInteractionListener,
                            ),
                            start = range.start,
                            end = range.end,
                        ),
                    )
                }
                is LinkAnnotation.Url -> {
                    listOf(
                        AnnotatedString.Range(
                            item = LinkAnnotation.Url(
                                url = annotation.url,
                                styles = linkStyles,
                                linkInteractionListener = linkInteractionListener,
                            ),
                            start = range.start,
                            end = range.end,
                        ),
                    )
                }
                else -> listOf(range)
            }
        }
    }

    Text(
        text = linkedText,
        modifier = modifier,
        color = WooTheme.colors.surface.onVariant,
        style = WooTheme.text.bodyMedium.regular,
        inlineContent = emptyMap<String, InlineTextContent>(),
    )
}

@Composable
fun WooLinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val linkColor = if (enabled) {
        WooTheme.colors.primary
    } else {
        WooTheme.colors.surface.onLowest
    }
    val linkStyles = remember(linkColor) {
        TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
    val linkInteractionListener = remember(enabled, onClick) {
        LinkInteractionListener {
            if (enabled) {
                onClick()
            }
        }
    }
    val linkedText = remember(text, enabled, linkStyles, linkInteractionListener) {
        if (enabled) {
            buildAnnotatedString {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = WOO_LINK_TEXT_TAG,
                        styles = linkStyles,
                        linkInteractionListener = linkInteractionListener,
                    ),
                ) {
                    append(text)
                }
            }
        } else {
            AnnotatedString(text)
        }
    }

    Text(
        text = linkedText,
        modifier = modifier,
        color = linkColor,
        style = WooTheme.text.bodyMedium.emphasized,
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooTextPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Column(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                WooPageTitle("Privacy")
                WooBodyText("Control how diagnostics and usage information help improve the app.")
                WooLinkedBodyText(
                    text = buildAnnotatedString {
                        append("Read the ")
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "privacy_policy",
                                linkInteractionListener = {},
                            ),
                        ) {
                            append("privacy policy")
                        }
                        append(" before enabling diagnostics.")
                    },
                    onLinkClick = {},
                )
                WooLinkText(text = "View policies", onClick = {})
                WooLinkText(text = "Disabled link", onClick = {}, enabled = false)
            }
        }
    }
}

private fun LinkAnnotation.destination(): String =
    when (this) {
        is LinkAnnotation.Clickable -> tag
        is LinkAnnotation.Url -> url
        else -> error("Unsupported LinkAnnotation type: $this")
    }

private const val WOO_LINK_TEXT_TAG = "woo_link_text"
