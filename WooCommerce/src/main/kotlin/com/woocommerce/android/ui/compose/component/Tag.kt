package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R

@Composable
fun WcTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 35))
            .background(colorResource(id = R.color.tag_bg_main))
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = dimensionResource(id = R.dimen.minor_75),
                vertical = dimensionResource(id = R.dimen.minor_25)
            ),
            text = text,
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.tag_text_main),
            fontWeight = FontWeight.Bold
        )
    }
}
