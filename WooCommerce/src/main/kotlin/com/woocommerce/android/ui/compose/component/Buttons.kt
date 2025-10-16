package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import androidx.compose.material.LocalContentColor as LocalContentColorM2
import androidx.compose.material.LocalTextStyle as LocalTextStyleM2
import androidx.compose.material.Text as TextM2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCColoredButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.wcColoredButtonColors(),
    elevation: ButtonElevation? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    rippleColor: Color = colorResource(R.color.color_primary_variant),
    content: @Composable RowScope.() -> Unit,
) {
    val rippleConfiguration = RippleConfiguration(color = rippleColor)

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            elevation = elevation,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            modifier = modifier,
            shape = shape,
            content = {
                ProvideMaterial3CompositionForMaterial2 {
                    content()
                }
            }
        )
    }
}

@Composable
fun WCColoredButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.wcColoredButtonColors(),
    elevation: ButtonElevation? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    rippleColor: Color = colorResource(R.color.color_primary_variant),
) {
    WCColoredButton(
        onClick = { if (!loading) onClick() },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        rippleColor = rippleColor
    ) {
        Box {
            if (loading) {
                ButtonCircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (loading) 0f else 1f)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
                }
                Text(text = text)
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
                    trailingIcon()
                }
            }
        }
    }
}

@Composable
fun WCOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.wcOutlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.wcOutlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        border = border,
        modifier = modifier,
        content = {
            ProvideMaterial3CompositionForMaterial2 {
                content()
            }
        }
    )
}

@Composable
fun WCOutlinedButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.wcOutlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.wcOutlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    WCOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        if (loading) {
            ButtonCircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
        } else {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
            }
            Text(text = text)
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
                trailingIcon()
            }
        }
    }
}

@Composable
fun WCRemoveButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.wcOutlinedButtonColors().copy(
        contentColor = MaterialTheme.colorScheme.error,
    ),
    border: BorderStroke? = ButtonDefaults.wcOutlinedButtonBorder(
        enabled = enabled,
        color = MaterialTheme.colorScheme.error
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    WCOutlinedButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}

@Composable
fun WCSelectableChip(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    selectedButtonColors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = colorResource(id = R.color.primary_colored_button_background),
        contentColor = colorResource(id = R.color.woo_white)
    ),
    defaultButtonColors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color.Transparent,
    ),
    isSelected: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(50),
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = if (isSelected) selectedButtonColors else defaultButtonColors,
        shape = shape,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
        }
        Text(text = text)
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
            trailingIcon()
        }
    }
}

@Composable
fun WCTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.wcTextButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        shape = MaterialTheme.shapes.small,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = {
            ProvideMaterial3CompositionForMaterial2 {
                content()
            }
        }
    )
}

@Composable
fun WCTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    allCaps: Boolean = true,
    colors: ButtonColors = ButtonDefaults.wcTextButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    WCTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = colors
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp)
                )
            }

            Text(text = text.let { if (allCaps) it.toUpperCase(Locale.current) else it })
        }
    }
}

@Composable
private fun ButtonDefaults.wcColoredButtonColors(): ButtonColors {
    return buttonColors(
        containerColor = colorResource(id = R.color.primary_colored_button_background),
        // For now, our onPrimary color is black for dark theme, which seems to be a mistake.
        // We are using white here to match the design, until confirming if we need to change the onPrimary color.
        contentColor = colorResource(id = R.color.woo_white)
    )
}

@Composable
private fun ButtonDefaults.wcOutlinedButtonColors(): ButtonColors {
    return outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun ButtonDefaults.wcOutlinedButtonBorder(
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.outline
): BorderStroke {
    return outlinedButtonBorder(enabled).copy(
        brush = SolidColor(
            color.let {
                if (!enabled) {
                    it.copy(alpha = 0.12f) // Disabled state
                } else {
                    it
                }
            }
        )
    )
}

@Composable
private fun ButtonDefaults.wcTextButtonColors(): ButtonColors {
    return textButtonColors(
        disabledContentColor = colorResource(id = R.color.color_on_surface_medium),
    )
}

/**
 * Provides Material 3 compositions for Material 2 components.
 * This is needed for now while we mix Material 2 components with Material 3 ones,
 * as Material 2 components use `androidx.compose.material` compositions from Material 2 and not
 * `androidx.compose.material3` ones.
 */
@Composable
private fun ProvideMaterial3CompositionForMaterial2(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColorM2 provides LocalContentColor.current,
        LocalTextStyleM2 provides LocalTextStyle.current,
    ) {
        content()
    }
}

@Composable
private fun ButtonCircularProgressIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        color = LocalContentColor.current,
        modifier = modifier
            .size(24.dp)
    )
}

@LightDarkThemePreviews
@Composable
private fun ButtonsPreview() {
    WooThemeWithBackground {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
        ) {
            WCColoredButton(onClick = {}) {
                Text("Button")
            }
            WCColoredButton(onClick = {}) {
                TextM2("Button M2")
            }
            WCColoredButton(
                onClick = {},
                enabled = false
            ) {
                Text("Disabled Button")
            }
            WCColoredButton(
                onClick = {},
                text = "Button With icon",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorResource(id = R.color.woo_white),
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorResource(id = R.color.woo_white),
                    )
                }
            )
            WCColoredButton(
                text = "Button Loading",
                loading = true,
                onClick = {},
            )

            WCOutlinedButton(onClick = {}) {
                Text(text = "Outlined Button")
            }
            WCOutlinedButton(onClick = {}, enabled = false) { Text(text = "Disabled Outlined Button") }

            WCOutlinedButton(
                onClick = {},
                text = "Outlined Button with icon",
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
            WCOutlinedButton(
                text = "Outlined Button Loading",
                loading = true,
                onClick = {},
            )

            WCTextButton(onClick = {}) {
                Text(text = "Text button")
            }
            WCTextButton(onClick = {}) {
                TextM2(text = "Text Button M2")
            }
            WCTextButton(onClick = {}, text = "Text Button")
            WCTextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                text = "Text Button",
                icon = Icons.Default.Add
            )

            WCSelectableChip(
                onClick = {},
                text = "Selectable Chip: selected",
                isSelected = true
            )
            WCSelectableChip(
                onClick = {},
                text = "Selectable Chip: not selected",
                isSelected = false
            )

            WCRemoveButton(
                onClick = {},
                text = "Remove",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Recycling,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
