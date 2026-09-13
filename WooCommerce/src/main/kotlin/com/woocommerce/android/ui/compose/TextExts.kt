package com.woocommerce.android.ui.compose

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration

/**
 * Creates an [AnnotatedString] from the passed String resource.
 *
 * @param args The arguments to be used in the string resource.
 */
@Composable
fun annotatedStringRes(@StringRes stringResId: Int, vararg args: Any): AnnotatedString {
    val string = stringResource(id = stringResId, *args)

    return AnnotatedString.fromHtml(string)
}

/**
 * Creates an [AnnotatedString] from the passed String resource.
 *
 * @param stringResId The resource ID of the string to be converted to an [AnnotatedString].
 * @param onUrlClick Allows overriding the default behavior of URL clicks, by default it will open the URL in an
 * external browser.
 * @param args The arguments to be used in the string resource.
 */
@Composable
fun clickableAnnotatedStringRes(
    @StringRes stringResId: Int,
    onUrlClick: ((String) -> Unit),
    vararg args: Any
): AnnotatedString {
    val linkInteractionListener = remember(onUrlClick) {
        LinkInteractionListener { linkAnnotation ->
            val url = when (linkAnnotation) {
                is LinkAnnotation.Url -> linkAnnotation.url
                is LinkAnnotation.Clickable -> linkAnnotation.tag
                else -> error("Unsupported LinkAnnotation type: $linkAnnotation")
            }
            onUrlClick.invoke(url)
        }
    }

    return AnnotatedString.fromHtml(
        stringResource(id = stringResId, *args),
        linkInteractionListener = linkInteractionListener,
        linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.None
            ),
        )
    )
}
