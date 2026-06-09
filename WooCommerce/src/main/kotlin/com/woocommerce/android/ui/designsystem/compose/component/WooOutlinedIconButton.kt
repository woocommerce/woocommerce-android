package com.woocommerce.android.ui.designsystem.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.foundation.DefaultWooStroke
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemTheme
import androidx.compose.material3.OutlinedIconButton as MaterialOutlinedIconButton

@Composable
fun WooOutlinedIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: WooIconButtonEmphasis = WooIconButtonEmphasis.Neutral,
) {
    WooOutlinedIconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        enabled = enabled,
        emphasis = emphasis,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(ICON_SIZE),
        )
    }
}

@Composable
fun WooOutlinedIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: WooIconButtonEmphasis = WooIconButtonEmphasis.Neutral,
    icon: @Composable () -> Unit,
) {
    require(contentDescription.isNotBlank()) {
        "WooOutlinedIconButton contentDescription must not be blank"
    }

    val colors = WooTheme.colors
    val contentColor = when (emphasis) {
        WooIconButtonEmphasis.Neutral -> colors.surface.onDefault
        WooIconButtonEmphasis.Primary -> colors.primary
    }

    MaterialOutlinedIconButton(
        onClick = onClick,
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
            },
        enabled = enabled,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.surface.onLowest,
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(DefaultWooStroke.extraThin, colors.outlineVariant),
    ) {
        Box(
            modifier = Modifier.clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooOutlinedIconButtonPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Row(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                WooOutlinedIconButton(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_help_24dp),
                    contentDescription = "Help",
                    onClick = {},
                )
                WooOutlinedIconButton(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                    contentDescription = "Open",
                    onClick = {},
                    emphasis = WooIconButtonEmphasis.Primary,
                )
                WooOutlinedIconButton(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_help_24dp),
                    contentDescription = "Disabled help",
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}

private val ICON_SIZE = 18.dp
