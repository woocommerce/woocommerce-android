package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosEmptyScreen(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    contentDescription: String,
    icon: ImageVector = WooPosIcons.NotFound,
) {
    WooPosItemsEmptyListInternal(
        modifier = modifier,
        title = title,
        message = message,
        contentDescription = contentDescription,
        icon = icon,
        actionLabel = null,
        onActionClicked = null
    )
}

@Composable
fun WooPosEmptyScreen(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    contentDescription: String,
    icon: ImageVector = WooPosIcons.NotFound,
    actionLabel: String,
    onActionClicked: (() -> Unit),
) {
    WooPosItemsEmptyListInternal(
        modifier = modifier,
        title = title,
        message = message,
        contentDescription = contentDescription,
        icon = icon,
        actionLabel = actionLabel,
        onActionClicked = onActionClicked
    )
}

@Composable
private fun WooPosItemsEmptyListInternal(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    contentDescription: String,
    icon: ImageVector = WooPosIcons.NotFound,
    actionLabel: String?,
    onActionClicked: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        WooPosEmptyContent(
            title = title,
            message = message,
            image = {
                Image(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    imageVector = icon,
                    contentDescription = contentDescription,
                )
            },
            action = {
                if (onActionClicked != null && actionLabel != null) {
                    WooPosButton(
                        text = actionLabel,
                        onClick = onActionClicked,
                        modifier = Modifier.wooPosFullScreenActionButton()
                    )
                }
            },
        )
    }
}

@Composable
fun WooPosEmptyScreen(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    image: @Composable () -> Unit,
    bottomActionLabel: String,
    onActionClicked: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            WooPosEmptyContent(
                title = title,
                message = message,
                image = image,
                isTitleHeading = true,
            )
        }
        WooPosButton(
            text = bottomActionLabel,
            onClick = onActionClicked,
            modifier = Modifier.wooPosFullScreenActionButton(),
        )
    }
}

@Composable
private fun WooPosEmptyContent(
    title: String,
    message: String,
    image: @Composable () -> Unit,
    isTitleHeading: Boolean = false,
    action: @Composable () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        image()

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        WooPosText(
            text = title,
            modifier = if (isTitleHeading) Modifier.semantics { heading() } else Modifier,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        action()
    }
}

@WooPosPreview
@Composable
fun WooPosEmptyScreenPreview() {
    WooPosTheme {
        WooPosEmptyScreen(
            modifier = Modifier.fillMaxSize(),
            title = "Empty List",
            message = "This list is empty",
            contentDescription = ""
        )
    }
}
