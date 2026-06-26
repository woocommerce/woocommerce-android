@file:Suppress("WooPosDesignSystemButtonUsageRule")

package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize

private const val PHONE_FULL_SCREEN_BUTTON_WIDTH_FRACTION = 1.0f
private const val TABLET_FULL_SCREEN_BUTTON_WIDTH_FRACTION = 0.5f

@Composable
fun Modifier.wooPosFullScreenActionButton(): Modifier {
    val fraction = if (currentWooPosBreakpoint() == WooPosBreakpoint.Phone) {
        PHONE_FULL_SCREEN_BUTTON_WIDTH_FRACTION
    } else {
        TABLET_FULL_SCREEN_BUTTON_WIDTH_FRACTION
    }
    return this.fillMaxWidth(fraction)
}

@Composable
fun WooPosButton(
    modifier: Modifier = Modifier,
    text: String,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        height = WooPosComponentSize.Small.value,
        loadingIndicatorSize = WooPosIconSize.Medium.value.toAdaptiveIconSize(),
        textStyle = WooPosTypography.BodyLarge,
        text = text,
        state = state,
        baseColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = WooPosTheme.colors.disabledContainer,
            disabledContentColor = WooPosTheme.colors.onDisabledContainer,
        ),
        onClick = onClick,
    )
}

@Composable
fun WooPosButtonSmall(
    modifier: Modifier = Modifier,
    text: String,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        height = WooPosComponentSize.XXSmall.value,
        loadingIndicatorSize = 20.dp.toAdaptiveIconSize(),
        textStyle = WooPosTypography.BodySmall,
        text = text,
        state = state,
        baseColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = WooPosTheme.colors.disabledContainer,
            disabledContentColor = WooPosTheme.colors.onDisabledContainer,
        ),
        onClick = onClick,
    )
}

@Composable
fun WooPosOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    maxLines: Int = Int.MAX_VALUE,
    textStyle: WooPosTypography = WooPosTypography.BodyLarge,
    onClick: () -> Unit,
) {
    val borderColor = if (state != WooPosButtonState.DISABLED) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        WooPosTheme.colors.disabledContainer
    }
    Button(
        modifier = modifier,
        height = WooPosComponentSize.Small.value,
        loadingIndicatorSize = WooPosIconSize.Medium.value.toAdaptiveIconSize(),
        textStyle = textStyle,
        text = text,
        border = BorderStroke(2.dp, borderColor),
        baseColors = ButtonDefaults.buttonColors(
            containerColor = WooPosTheme.colors.transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = WooPosTheme.colors.transparent,
            disabledContentColor = WooPosTheme.colors.onDisabledContainer,
        ),
        state = state,
        maxLines = maxLines,
        onClick = onClick,
    )
}

@Composable
fun WooPosOutlinedButtonSmall(
    modifier: Modifier = Modifier,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    text: String,
    onClick: () -> Unit,
) {
    val borderColor = if (state != WooPosButtonState.DISABLED) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        WooPosTheme.colors.disabledContainer
    }
    Button(
        modifier = modifier,
        height = WooPosComponentSize.XXSmall.value,
        loadingIndicatorSize = 20.dp.toAdaptiveIconSize(),
        textStyle = WooPosTypography.BodySmall,
        text = text,
        border = BorderStroke(2.dp, borderColor),
        baseColors = ButtonDefaults.buttonColors(
            containerColor = WooPosTheme.colors.transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = WooPosTheme.colors.transparent,
            disabledContentColor = WooPosTheme.colors.onDisabledContainer,
        ),
        state = state,
        onClick = onClick,
    )
}

@Composable
fun WooPosToggleButton(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    onClick: () -> Unit,
) {
    if (isSelected) {
        WooPosButtonSmall(
            modifier = modifier,
            text = text,
            state = state,
            onClick = onClick,
        )
    } else {
        WooPosOutlinedButtonSmall(
            modifier = modifier,
            text = text,
            state = state,
            onClick = onClick,
        )
    }
}

@Composable
fun WooPosCircularIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(WooPosComponentSize.XSmall.value)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun WooPosIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            modifier = Modifier
                .size(WooPosIconSize.Medium.value),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
    }
}

@Composable
private fun Button(
    modifier: Modifier = Modifier,
    height: Dp,
    textStyle: WooPosTypography,
    text: String,
    loadingIndicatorSize: Dp,
    baseColors: ButtonColors,
    border: BorderStroke? = null,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    maxLines: Int = Int.MAX_VALUE,
    onClick: () -> Unit,
) {
    val isSuccess = state == WooPosButtonState.SUCCESS
    val animatedContainerColor by animateColorAsState(
        targetValue = if (isSuccess) WooPosTheme.colors.success else baseColors.containerColor,
        animationSpec = tween(200),
        label = "button_container_color",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSuccess) WooPosTheme.colors.onSuccess else baseColors.contentColor,
        animationSpec = tween(200),
        label = "button_content_color",
    )
    val onClickLocal: () -> Unit = if (state == WooPosButtonState.ENABLED || isSuccess) onClick else ({})
    Button(
        onClick = onClickLocal,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        enabled = state == WooPosButtonState.ENABLED || state == WooPosButtonState.LOADING || isSuccess,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedContainerColor,
            contentColor = animatedContentColor,
            disabledContainerColor = baseColors.disabledContainerColor,
            disabledContentColor = baseColors.disabledContentColor,
        ),
        modifier = modifier
            .heightIn(min = height, max = height * 3),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = WooPosSpacing.None.value,
            pressedElevation = WooPosSpacing.None.value,
            disabledElevation = WooPosSpacing.None.value,
            hoveredElevation = WooPosSpacing.None.value,
            focusedElevation = WooPosSpacing.None.value
        )
    ) {
        AnimatedContent(
            targetState = isSuccess,
            transitionSpec = {
                if (targetState) {
                    slideInVertically(tween(250)) { -it } togetherWith slideOutVertically(tween(250)) { it }
                } else {
                    slideInVertically(tween(250)) { it } togetherWith slideOutVertically(tween(250)) { -it }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
            label = "button_state_content",
        ) { showSuccess ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (showSuccess) {
                    Icon(
                        imageVector = WooPosIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(loadingIndicatorSize),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        // Always include the text. When loading, hide it with alpha to keep the width
                        WooPosText(
                            text = text,
                            style = textStyle,
                            fontWeight = FontWeight.Bold,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(if (state == WooPosButtonState.LOADING) 0f else 1f)
                        )
                        if (state == WooPosButtonState.LOADING) {
                            ButtonsLoadingIndicator(size = loadingIndicatorSize)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonsLoadingIndicator(size: Dp) {
    WooPosButtonLoadingIndicator(
        modifier = Modifier.size(size),
    )
}

@Composable
@PreviewFontScale
fun WooPosButtonsPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier
                .padding(WooPosSpacing.Medium.value)
                .width(600.dp.toAdaptiveComponentSize())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        ) {
            WooPosButton(
                text = "Button",
                state = WooPosButtonState.ENABLED,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )

            WooPosButton(
                text = "Button",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                state = WooPosButtonState.DISABLED,
            )

            WooPosButton(
                text = "Button",
                state = WooPosButtonState.LOADING,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )

            WooPosButton(
                text = "Button",
                state = WooPosButtonState.SUCCESS,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButton(
                text = "Button Outlined",
                state = WooPosButtonState.ENABLED,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )
            WooPosOutlinedButton(
                text = "Button Outlined",
                state = WooPosButtonState.DISABLED,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )
            WooPosOutlinedButton(
                text = "Button Outlined",
                state = WooPosButtonState.LOADING,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                WooPosToggleButton(
                    text = "Selected",
                    isSelected = true,
                    onClick = {}
                )
                WooPosToggleButton(
                    text = "Unselected",
                    isSelected = false,
                    onClick = {}
                )
                WooPosToggleButton(
                    text = "Disabled",
                    isSelected = false,
                    state = WooPosButtonState.DISABLED,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
@PreviewFontScale
fun WooPosSmallButtonsPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier
                .padding(WooPosSpacing.Medium.value)
                .width(600.dp.toAdaptiveComponentSize())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        ) {
            WooPosButtonSmall(
                text = "Button Small",
                state = WooPosButtonState.ENABLED,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosButtonSmall(
                text = "Button Small",
                onClick = {},
                state = WooPosButtonState.DISABLED,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosButtonSmall(
                text = "Button Small",
                state = WooPosButtonState.LOADING,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosCircularIconButton(
                icon = ImageVector.vectorResource(R.drawable.ic_search_24dp),
                onClick = {}
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosIconButton(icon = ImageVector.vectorResource(R.drawable.ic_delete_24dp)) {}

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButtonSmall(
                text = "Button Outlined Small",
                state = WooPosButtonState.ENABLED,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButtonSmall(
                text = "Button Outlined Small",
                state = WooPosButtonState.DISABLED,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButtonSmall(
                text = "Button Outlined Small",
                state = WooPosButtonState.LOADING,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                WooPosToggleButton(
                    text = "Selected",
                    isSelected = true,
                    onClick = {}
                )
                WooPosToggleButton(
                    text = "Unselected",
                    isSelected = false,
                    onClick = {}
                )
            }
        }
    }
}

enum class WooPosButtonState {
    ENABLED, DISABLED, LOADING, SUCCESS
}
