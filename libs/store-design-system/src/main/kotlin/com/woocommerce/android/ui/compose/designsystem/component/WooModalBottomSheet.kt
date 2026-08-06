@file:OptIn(ExperimentalMaterial3Api::class)

package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooModalBottomSheet(
    state: WooModalBottomSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WooTheme.colors
    val shape = RoundedCornerShape(
        topStart = WooTheme.radius.extraLarge,
        topEnd = WooTheme.radius.extraLarge,
    )

    ModalBottomSheet(
        sheetState = state.materialState,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = shape,
        containerColor = colors.surface.bright,
        contentColor = colors.surface.onDefault,
        tonalElevation = 0.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 32.dp,
                height = 4.dp,
                shape = RoundedCornerShape(WooTheme.radius.full),
                color = colors.surface.onVariantLowest,
            )
        },
        content = content,
    )
}

@PreviewLightDark
@Composable
private fun WooModalBottomSheetPreview() {
    WooDesignSystemTheme {
        val state = rememberWooModalBottomSheetState()
        WooModalBottomSheet(state = state, onDismissRequest = {}) {
            WooModalBottomSheetPreviewContent()
        }
    }
}

@Preview(name = "Long scrolling content", heightDp = 640, showBackground = true)
@Composable
private fun WooModalBottomSheetLongContentPreview() {
    WooDesignSystemTheme {
        val state = rememberWooModalBottomSheetState()
        WooModalBottomSheet(state = state, onDismissRequest = {}) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                repeat(12) { index ->
                    Text(
                        text = "Store setting ${index + 1}",
                        style = WooTheme.text.bodyLarge.regular,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "Tablet max width", widthDp = 1000, heightDp = 720, showBackground = true)
@Composable
private fun WooModalBottomSheetTabletPreview() {
    WooDesignSystemTheme {
        val state = rememberWooModalBottomSheetState()
        WooModalBottomSheet(state = state, onDismissRequest = {}) {
            WooModalBottomSheetPreviewContent()
        }
    }
}

@Composable
private fun WooModalBottomSheetPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Order date type",
            style = WooTheme.text.titleLarge.strong,
        )
        Text(
            text = "Choose which order date is used for dashboard statistics.",
            style = WooTheme.text.bodyLarge.regular,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
    }
}
