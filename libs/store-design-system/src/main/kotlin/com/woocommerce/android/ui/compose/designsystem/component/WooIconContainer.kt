package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.Star
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
fun WooIconContainer(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tone: WooIconContainerTone = WooIconContainerTone.Purple,
    contentDescription: String? = null,
) {
    val colors = tone.toIconContainerColors()

    Surface(
        modifier = modifier.size(ICON_CONTAINER_SIZE),
        color = colors.containerColor,
        contentColor = colors.contentColor,
        shape = RoundedCornerShape(WooTheme.radius.medium),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(WooTheme.iconSize.size18),
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooIconContainerPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooIconContainerDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooIconContainerDemo(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
            WooIconContainer(WooIcons.Regular.Star)
            WooIconContainer(
                imageVector = WooIcons.Regular.Star,
                tone = WooIconContainerTone.Sandstone,
            )
            WooIconContainer(
                imageVector = WooIcons.Regular.Star,
                tone = WooIconContainerTone.Blue,
            )
            WooIconContainer(
                imageVector = WooIcons.Regular.Star,
                tone = WooIconContainerTone.Green,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
            WooIconContainer(
                imageVector = WooIcons.Regular.Star,
                tone = WooIconContainerTone.Orange,
            )
            WooIconContainer(
                imageVector = WooIcons.Regular.Star,
                tone = WooIconContainerTone.Pink,
            )
            WooIconContainer(
                imageVector = WooIcons.Regular.Star,
                tone = WooIconContainerTone.DarkPurple,
            )
        }
    }
}

@Composable
private fun WooIconContainerTone.toIconContainerColors(): WooIconContainerColors {
    val colors = WooTheme.colors
    val palette = colors.palette

    return when (this) {
        WooIconContainerTone.Purple -> WooIconContainerColors(palette.wooPurple.shade0, colors.primary)
        WooIconContainerTone.Sandstone -> {
            WooIconContainerColors(palette.sandstone.shade10, palette.sandstone.shade60)
        }
        WooIconContainerTone.Blue -> WooIconContainerColors(palette.wooBlue.shade20, palette.wooBlue.shade60)
        WooIconContainerTone.Green -> WooIconContainerColors(palette.wooGreen.shade20, palette.wooGreen.shade60)
        WooIconContainerTone.Orange -> WooIconContainerColors(palette.wooOrange.shade20, palette.wooOrange.shade60)
        WooIconContainerTone.Pink -> WooIconContainerColors(palette.wooPink.shade20, palette.wooPink.shade60)
        WooIconContainerTone.DarkPurple -> WooIconContainerColors(colors.primary, palette.wooPurple.shade5)
    }
}

private data class WooIconContainerColors(
    val containerColor: Color,
    val contentColor: Color,
)

private val ICON_CONTAINER_SIZE = 44.dp
