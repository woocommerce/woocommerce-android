package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

@Composable
fun WidgetError(
    onContactSupportClicked: () -> Unit,
    onRetryClicked: () -> Unit
) {
    Column(
        modifier = Modifier.padding(WooTheme.padding.padding5),
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
    ) {
        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            contentDescription = null,
            painter = painterResource(id = R.drawable.img_widget_error)
        )

        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.dynamic_dashboard_widget_error_title),
            style = WooTheme.text.titleLarge.strong,
            color = WooTheme.colors.surface.onDefault,
        )

        val errorMessage = dashboardWidgetErrorMessage(onContactSupportClicked)
        Text(
            text = errorMessage,
            textAlign = TextAlign.Center,
            style = WooTheme.text.bodyLarge.regular,
            color = WooTheme.colors.surface.onDefault,
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding8),
        )

        WooOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.retry),
            onClick = onRetryClicked,
        )
    }
}

@Composable
private fun dashboardWidgetErrorMessage(onContactSupportClicked: () -> Unit): AnnotatedString {
    val linkInteractionListener = remember(onContactSupportClicked) {
        LinkInteractionListener { linkAnnotation ->
            when (linkAnnotation) {
                is LinkAnnotation.Url,
                is LinkAnnotation.Clickable -> onContactSupportClicked()
                else -> error("Unsupported LinkAnnotation type: $linkAnnotation")
            }
        }
    }

    return AnnotatedString.fromHtml(
        htmlString = stringResource(id = R.string.dynamic_dashboard_widget_error_description),
        linkInteractionListener = linkInteractionListener,
        linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = WooTheme.colors.container.onSecondaryContainer,
                textDecoration = TextDecoration.Underline,
            ),
        ),
    )
}

@Composable
@PreviewLightDark
fun WidgetErrorPreview() {
    WooDesignSystemThemeWithBackground {
        WidgetError(
            onContactSupportClicked = {},
            onRetryClicked = {},
        )
    }
}
