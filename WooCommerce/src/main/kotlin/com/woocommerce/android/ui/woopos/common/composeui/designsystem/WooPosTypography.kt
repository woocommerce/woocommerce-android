package com.woocommerce.android.ui.woopos.common.composeui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText

sealed class WooPosTypography(val style: TextStyle) {
    object Heading : WooPosTypography(
        style = TextStyle(
            fontSize = 36.sp,
            lineHeight = 40.sp
        )
    )

    object BodyXLarge : WooPosTypography(
        style = TextStyle(
            fontSize = 30.sp,
            lineHeight = 32.sp
        )
    )

    object BodyLarge : WooPosTypography(
        style = TextStyle(
            fontSize = 24.sp,
            lineHeight = 32.sp
        )
    )

    object BodyMedium : WooPosTypography(
        style = TextStyle(
            fontSize = 20.sp,
            lineHeight = 32.sp
        )
    )

    object BodySmall : WooPosTypography(
        style = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp
        )
    )

    object Caption : WooPosTypography(
        style = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    )
}

@Composable
@Preview
fun PreviewTextReferenceTable() {
    WooPosTheme {
        Surface {
            TextReferenceTable()
        }
    }
}

@Composable
private fun TextReferenceTable() {
    val tableData = listOf(
        "Heading" to WooPosTypography.Heading,
        "Body X Large" to WooPosTypography.BodyXLarge,
        "Body Large" to WooPosTypography.BodyLarge,
        "Body Medium" to WooPosTypography.BodyMedium,
        "Body Small" to WooPosTypography.BodySmall,
        "Caption" to WooPosTypography.Caption
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.Medium.value)
            .background(
                MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .verticalScroll(rememberScrollState())
    ) {
        WooPosText(
            text = "Text Styles Preview",
            style = WooPosTypography.Heading,
            modifier = Modifier.padding(WooPosSpacing.Medium.value)
        )

        tableData.forEach { (name, style) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WooPosSpacing.XSmall.value),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WooPosText(
                    text = name,
                    style = WooPosTypography.BodyMedium,
                    modifier = Modifier.weight(1f)
                )
                WooPosText(
                    text = "Sample Text",
                    style = style,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}
