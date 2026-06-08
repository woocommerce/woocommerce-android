package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.woocommerce.android.ui.compose.theme.Material3Typography as LegacyMaterial3Typography

@Immutable
@Suppress("LongParameterList")
data class WooTypography(
    val displayLarge: WooTextRole,
    val displayMedium: WooTextRole,
    val displaySmall: WooTextRole,
    val headlineLarge: WooTextRole,
    val headlineMedium: WooTextRole,
    val headlineSmall: WooTextRole,
    val titleLarge: WooTextRole,
    val titleMedium: WooTextRole,
    val titleSmall: WooTextRole,
    val bodyLarge: WooTextRole,
    val bodyMedium: WooTextRole,
    val bodySmall: WooTextRole,
    val labelLarge: WooTextRole,
    val labelMedium: WooTextRole,
    val labelSmall: WooTextRole,
)

@Immutable
data class WooTextRole(
    val regular: TextStyle,
    val emphasized: TextStyle,
    val strong: TextStyle,
)

internal val DefaultWooTypography = WooTypography(
    displayLarge = textRole(size = 56, lineHeight = 64, letterSpacing = -0.41f),
    displayMedium = textRole(size = 48, lineHeight = 52, letterSpacing = -0.41f),
    displaySmall = textRole(size = 36, lineHeight = 44, letterSpacing = -0.41f),
    headlineLarge = textRole(size = 34, lineHeight = 40, letterSpacing = -1.40f),
    headlineMedium = textRole(size = 28, lineHeight = 36, letterSpacing = -0.41f),
    headlineSmall = textRole(size = 24, lineHeight = 32, letterSpacing = -0.75f),
    titleLarge = textRole(size = 20, lineHeight = 28, letterSpacing = -0.41f),
    titleMedium = textRole(size = 17, lineHeight = 20, letterSpacing = -0.41f),
    titleSmall = textRole(size = 14, lineHeight = 16, letterSpacing = -0.41f),
    bodyLarge = textRole(size = 17, lineHeight = 24, letterSpacing = -0.41f),
    bodyMedium = textRole(size = 14, lineHeight = 20, letterSpacing = -0.41f),
    bodySmall = textRole(size = 13, lineHeight = 16, letterSpacing = -0.41f),
    labelLarge = textRole(size = 16, lineHeight = 24, letterSpacing = -0.41f),
    labelMedium = textRole(size = 14, lineHeight = 20, letterSpacing = -0.41f),
    labelSmall = textRole(size = 10, lineHeight = 14, letterSpacing = -0.07f),
)

// Preserves current app text metrics when DS components render in the legacy root.
internal val LegacyWooTypography = WooTypography(
    displayLarge = legacyTextRole(LegacyMaterial3Typography.displayLarge),
    displayMedium = legacyTextRole(LegacyMaterial3Typography.displayMedium),
    displaySmall = legacyTextRole(LegacyMaterial3Typography.displaySmall),
    headlineLarge = legacyTextRole(LegacyMaterial3Typography.headlineLarge),
    headlineMedium = legacyTextRole(LegacyMaterial3Typography.headlineMedium),
    headlineSmall = legacyTextRole(LegacyMaterial3Typography.headlineSmall),
    titleLarge = legacyTextRole(LegacyMaterial3Typography.titleLarge),
    titleMedium = legacyTextRole(LegacyMaterial3Typography.titleMedium),
    titleSmall = legacyTextRole(LegacyMaterial3Typography.titleSmall),
    bodyLarge = legacyTextRole(LegacyMaterial3Typography.bodyLarge),
    bodyMedium = legacyTextRole(LegacyMaterial3Typography.bodyMedium),
    bodySmall = legacyTextRole(LegacyMaterial3Typography.bodySmall),
    labelLarge = legacyTextRole(LegacyMaterial3Typography.labelLarge),
    labelMedium = legacyTextRole(LegacyMaterial3Typography.labelMedium),
    labelSmall = legacyTextRole(LegacyMaterial3Typography.labelSmall),
)

internal fun WooTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = displayLarge.regular,
    displayMedium = displayMedium.regular,
    displaySmall = displaySmall.regular,
    headlineLarge = headlineLarge.regular,
    headlineMedium = headlineMedium.regular,
    headlineSmall = headlineSmall.regular,
    titleLarge = titleLarge.regular,
    titleMedium = titleMedium.regular,
    titleSmall = titleSmall.regular,
    bodyLarge = bodyLarge.regular,
    bodyMedium = bodyMedium.regular,
    bodySmall = bodySmall.regular,
    labelLarge = labelLarge.regular,
    labelMedium = labelMedium.regular,
    labelSmall = labelSmall.regular,
)

internal val LocalWooText = staticCompositionLocalOf<WooTypography> {
    error("WooTheme.text is not available. Wrap content in WooDesignSystemTheme or WooThemeWithBackground.")
}

private fun legacyTextRole(style: TextStyle): WooTextRole = WooTextRole(
    regular = style,
    emphasized = style.copy(fontWeight = FontWeight.Medium),
    strong = style.copy(fontWeight = FontWeight.Bold),
)

private fun textRole(
    size: Int,
    lineHeight: Int,
    letterSpacing: Float,
): WooTextRole = WooTextRole(
    regular = textStyle(
        size = size,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        fontWeight = FontWeight.Normal,
    ),
    emphasized = textStyle(
        size = size,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        fontWeight = FontWeight.Medium,
    ),
    strong = textStyle(
        size = size,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        fontWeight = FontWeight.Bold,
    ),
)

private fun textStyle(
    size: Int,
    lineHeight: Int,
    letterSpacing: Float,
    fontWeight: FontWeight,
): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = fontWeight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)
