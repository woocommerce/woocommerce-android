package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun IdleCircle() {
    val indicatorColor = colorResource(id = R.color.color_on_surface_medium)

    Canvas(modifier = Modifier.size(26.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(
            color = indicatorColor,
            radius = (size.minDimension - stroke.width) / 2,
            style = stroke
        )
    }
}
