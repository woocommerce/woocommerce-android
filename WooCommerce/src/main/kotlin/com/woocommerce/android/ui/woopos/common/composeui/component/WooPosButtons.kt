package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme

@Composable
fun WooPosButton(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle = MaterialTheme.typography.h5,
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
            style = textStyle,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun WooPosOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onBackground,
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.SemiBold,
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

@Composable
@WooPosPreview
fun WooPosOutlinedButtonPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            WooPosOutlinedButton(
                text = "Button",
                onClick = {},
            )
        }
    }
}
