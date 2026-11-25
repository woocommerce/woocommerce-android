package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

private const val ANIMATION_DURATION_MS = 300

@Composable
fun WooPosFullScreenModal(
    isVisible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    BackHandler(enabled = isVisible) { (onBackClick ?: onDismissRequest)() }
    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier.fillMaxSize(),
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {},
            )
            Surface(
                modifier = modifier
                    .fillMaxSize()
                    .animateEnterExit(
                        enter = slideInVertically(
                            animationSpec = tween(ANIMATION_DURATION_MS),
                            initialOffsetY = { it },
                        ),
                        exit = slideOutVertically(
                            animationSpec = tween(ANIMATION_DURATION_MS),
                            targetOffsetY = { it },
                        ),
                    ),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FullScreenModalHeader(
                        title = title,
                        onBackClick = onBackClick,
                        onCloseClick = onCloseClick,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = WooPosSpacing.XLarge.value,
                                end = WooPosSpacing.XLarge.value,
                                bottom = WooPosSpacing.XLarge.value,
                            ),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenModalHeader(
    title: String,
    onBackClick: (() -> Unit)?,
    onCloseClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = WooPosIconSize.XLarge.value)
            .padding(horizontal = WooPosSpacing.Small.value, vertical = WooPosSpacing.Small.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
    ) {
        if (onBackClick != null) {
            WooPosBackButton(
                contentDescription = stringResource(R.string.back),
                onClick = onBackClick,
            )
        } else {
            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        }
        WooPosText(
            text = title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (onCloseClick != null) {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(WooPosIconSize.Large.value),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosFullScreenModalPreview() {
    WooPosTheme {
        WooPosFullScreenModal(
            isVisible = true,
            title = "Set up barcode scanner",
            onDismissRequest = {},
            onBackClick = {},
            onCloseClick = {},
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WooPosText(
                    text = "Content in the center, buttons at the bottom.",
                    style = WooPosTypography.BodyLarge,
                )
            }
        }
    }
}
