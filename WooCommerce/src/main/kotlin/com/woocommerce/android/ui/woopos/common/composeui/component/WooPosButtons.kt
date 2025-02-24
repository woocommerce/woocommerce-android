package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosButton(
    modifier: Modifier = Modifier,
    text: String,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        height = 80.dp,
        loadingIndicatorSize = 32.dp,
        textStyle = WooPosTypography.BodyLarge,
        text = text,
        state = state,
        colors = ButtonDefaults.buttonColors(
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
        height = 40.dp,
        loadingIndicatorSize = 20.dp,
        textStyle = WooPosTypography.BodySmall,
        text = text,
        state = state,
        colors = ButtonDefaults.buttonColors(
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
    onClick: () -> Unit,
) {
    val borderColor = if (state == WooPosButtonState.ENABLED || state == WooPosButtonState.LOADING) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        WooPosTheme.colors.disabledContainer
    }
    Button(
        modifier = modifier,
        height = 80.dp,
        loadingIndicatorSize = 32.dp,
        textStyle = WooPosTypography.BodyLarge,
        text = text,
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
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
fun WooPosOutlinedButtonSmall(
    modifier: Modifier = Modifier,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    text: String,
    onClick: () -> Unit,
) {
    val borderColor = if (state == WooPosButtonState.ENABLED) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        WooPosTheme.colors.disabledContainer
    }
    Button(
        modifier = modifier,
        height = 40.dp,
        loadingIndicatorSize = 20.dp,
        textStyle = WooPosTypography.BodySmall,
        text = text,
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
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
private fun Button(
    modifier: Modifier = Modifier,
    height: Dp,
    textStyle: WooPosTypography,
    text: String,
    loadingIndicatorSize: Dp,
    colors: ButtonColors,
    border: BorderStroke? = null,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    onClick: () -> Unit,
) {
    val onClickLocal = if (state == WooPosButtonState.ENABLED) {
        onClick
    } else {
        {}
    }
    Button(
        onClick = onClickLocal,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        enabled = state == WooPosButtonState.ENABLED || state == WooPosButtonState.LOADING,
        border = border,
        colors = colors,
        modifier = modifier
            .height(height),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = WooPosSpacing.None.value,
            pressedElevation = WooPosSpacing.None.value,
            disabledElevation = WooPosSpacing.None.value,
            hoveredElevation = WooPosSpacing.None.value,
            focusedElevation = WooPosSpacing.None.value
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Always include the text. When loading, hide it with alpha to keep the width
            WooPosText(
                text = text,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(if (state == WooPosButtonState.LOADING) 0f else 1f)
            )
            when (state) {
                WooPosButtonState.ENABLED,
                WooPosButtonState.DISABLED -> {
                }

                WooPosButtonState.LOADING -> {
                    ButtonsLoadingIndicator(size = loadingIndicatorSize)
                }
            }
        }
    }
}

@Composable
private fun ButtonsLoadingIndicator(size: Dp) {
    WooPosCircularLoadingIndicator(
        modifier = Modifier.size(size),
        spinnerPrimaryColor = MaterialTheme.colorScheme.secondary,
        spinnerSecondaryColor = Color.White.copy(alpha = 0.2f)
            .compositeOver(MaterialTheme.colorScheme.primaryContainer),
    )
}

@Composable
@WooPosPreview
fun WooPosButtonsPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier
                .padding(WooPosSpacing.Medium.value)
                .width(600.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                WooPosButtonSmall(
                    text = "Button Small",
                    state = WooPosButtonState.ENABLED,
                    onClick = {}
                )

                WooPosButtonSmall(
                    text = "Button Small",
                    onClick = {},
                    state = WooPosButtonState.DISABLED,
                )

                WooPosButtonSmall(
                    text = "Button Small",
                    state = WooPosButtonState.LOADING,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WooPosOutlinedButtonSmall(
                    text = "Button Outlined Small",
                    state = WooPosButtonState.ENABLED,
                    onClick = {}
                )
                WooPosOutlinedButtonSmall(
                    text = "Button Outlined Small",
                    state = WooPosButtonState.DISABLED,
                    onClick = {}
                )
                WooPosOutlinedButtonSmall(
                    text = "Button Outlined Small",
                    state = WooPosButtonState.LOADING,
                    onClick = {}
                )
            }
        }
    }
}

enum class WooPosButtonState {
    ENABLED, DISABLED, LOADING
}
