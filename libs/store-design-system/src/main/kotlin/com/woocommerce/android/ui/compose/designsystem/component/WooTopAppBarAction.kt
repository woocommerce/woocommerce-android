package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.ui.graphics.vector.ImageVector

sealed interface WooTopAppBarAction {
    data class Icon(
        val imageVector: ImageVector,
        val contentDescription: String,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
    ) : WooTopAppBarAction {
        init {
            assert(contentDescription.isNotBlank()) {
                "WooTopAppBarAction.Icon contentDescription must not be blank"
            }
        }
    }

    data class Text(
        val text: String,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
    ) : WooTopAppBarAction {
        init {
            require(text.isNotBlank()) {
                "WooTopAppBarAction.Text text must not be blank"
            }
        }
    }
}
