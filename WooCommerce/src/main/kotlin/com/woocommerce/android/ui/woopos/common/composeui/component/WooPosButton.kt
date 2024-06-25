package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme

@Composable
fun WooPosButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
@WooPosPreview
fun WooPosButtonPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            WooPosButton(
                text = "Button",
                onClick = {},
            )
        }
    }
}
