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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

enum class WooIconContainerTone {
    Purple,
    Sandstone,
    Blue,
    Green,
    Orange,
    Pink,
    DarkPurple,
}

@Composable
fun WooIconContainer(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tone: WooIconContainerTone = WooIconContainerTone.Purple,
    contentDescription: String? = null,
) {
    if (contentDescription != null) {
        require(contentDescription.isNotBlank()) {
            "WooIconContainer contentDescription must not be blank when provided"
        }
    }

    val colors = tone.toIconContainerColors()

    Surface(
        modifier = modifier.size(ICON_CONTAINER_SIZE),
        color = colors.containerColor,
        contentColor = colors.contentColor,
        shape = ICON_CONTAINER_SHAPE,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(ICON_SIZE),
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
            Column(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
                    WooIconContainer(ImageVector.vectorResource(R.drawable.ic_star_24dp))
                    WooIconContainer(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                        tone = WooIconContainerTone.Sandstone,
                    )
                    WooIconContainer(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                        tone = WooIconContainerTone.Blue,
                    )
                    WooIconContainer(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                        tone = WooIconContainerTone.Green,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
                    WooIconContainer(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                        tone = WooIconContainerTone.Orange,
                    )
                    WooIconContainer(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                        tone = WooIconContainerTone.Pink,
                    )
                    WooIconContainer(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                        tone = WooIconContainerTone.DarkPurple,
                    )
                }
            }
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
            WooIconContainerColors(palette.sandstone.shade10, colors.surface.onHighest)
        }
        WooIconContainerTone.Blue -> WooIconContainerColors(palette.wooBlue.shade20, palette.wooBlue.shade60)
        WooIconContainerTone.Green -> WooIconContainerColors(palette.wooGreen.shade20, palette.wooGreen.shade60)
        WooIconContainerTone.Orange -> WooIconContainerColors(palette.wooOrange.shade20, palette.wooOrange.shade60)
        WooIconContainerTone.Pink -> WooIconContainerColors(palette.wooPink.shade20, palette.wooPink.shade60)
        WooIconContainerTone.DarkPurple -> WooIconContainerColors(colors.primary, colors.onPrimary)
    }
}

private data class WooIconContainerColors(
    val containerColor: Color,
    val contentColor: Color,
)

private val ICON_CONTAINER_SHAPE = RoundedCornerShape(8.dp)
private val ICON_CONTAINER_SIZE = 44.dp
private val ICON_SIZE = 18.dp
