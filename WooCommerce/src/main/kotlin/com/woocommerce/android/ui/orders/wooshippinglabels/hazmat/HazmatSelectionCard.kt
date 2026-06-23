package com.woocommerce.android.ui.orders.wooshippinglabels.hazmat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingLabelHazmatCategory

@Composable
fun HazmatSelectionCard(
    selectedCategory: ShippingLabelHazmatCategory,
    modifier: Modifier = Modifier,
    onClick: OnClick? = null,
) {
    Spacer(modifier = Modifier.height(4.dp))
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)))
                .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
                .background(color = colorResource(R.color.light_colored_button_background))
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(selectedCategory.stringResourceID),
                color = colorResource(id = R.color.color_on_surface),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
