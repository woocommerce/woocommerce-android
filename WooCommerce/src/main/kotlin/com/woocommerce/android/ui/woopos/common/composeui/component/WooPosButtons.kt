package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme

@Composable
fun WooPosButton(
    modifier: Modifier = Modifier,
    text: String,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
    ),
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        enabled = enabled,
        colors = colors,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
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
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun WooPosButtonLarge(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun WooPosOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
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
        shape = shape,
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
fun WooPosButtonsPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            WooPosButtonLarge(
                text = "Button Large",
                onClick = {},
            )

            Spacer(modifier = Modifier.height(16.dp))

            WooPosOutlinedButton(
                text = "Button Outlined",
                onClick = {},
            )

            Spacer(modifier = Modifier.height(16.dp))

            WooPosButton(
                text = "Button",
                onClick = {},
            )

            Spacer(modifier = Modifier.height(16.dp))

            WooPosButton(
                text = "Button Black And White",
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.onBackground
                ),
                onClick = {},
            )
        }
    }
}
