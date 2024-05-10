package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun WCTag(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = colorResource(id = R.color.tag_text_main),
    backgroundColor: Color = colorResource(R.color.tag_bg_main),
    textStyle: TextStyle = MaterialTheme.typography.caption,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(
                horizontal = dimensionResource(id = R.dimen.minor_50),
            )
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = dimensionResource(id = R.dimen.minor_75),
                vertical = dimensionResource(id = R.dimen.minor_50)
            ),
            text = text,
            style = textStyle,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

@Preview
@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WcTagPreview() {
    WCTag(text = "This is a tag")
}
