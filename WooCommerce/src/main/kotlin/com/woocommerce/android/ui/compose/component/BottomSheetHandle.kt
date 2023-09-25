package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.woocommerce.android.R.dimen

@Composable
fun BottomSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(dimensionResource(id = dimen.major_150))
            )
            .size(
                width = dimensionResource(id = dimen.major_200),
                height = dimensionResource(id = dimen.minor_50)
            )
    )
}
