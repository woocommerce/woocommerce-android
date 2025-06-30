package com.woocommerce.android.ui.orders.wooshippinglabels.hazmat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory

typealias OnClick = () -> Unit

@Composable
internal fun HazmatCard(
    modifier: Modifier = Modifier,
    selectedCategory: ShippingLabelHazmatCategory? = null,
    onClick: OnClick? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val mainRowModifier = when {
            onClick != null ->
                modifier
                    .clickable { onClick() }
                    .padding(vertical = 16.dp)
            else -> modifier
        }

        Row(modifier = mainRowModifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.shipping_label_hazmat_title),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = dimensionResource(id = R.dimen.major_100))
                    .align(Alignment.CenterVertically)
            )

            Text(
                text = if (selectedCategory == null) stringResource(R.string.no) else stringResource(R.string.yes),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium),
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            if (onClick != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    tint = colorResource(id = R.color.color_on_surface_medium),
                    contentDescription = stringResource(
                        id = R.string.shipping_label_package_details_items_expand_content_description
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        if (selectedCategory != null) {
            HazmatSelectionCard(
                selectedCategory = selectedCategory,
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@Preview("Hazmat non-clickable without selection")
@Composable
private fun NonClickableWithoutSelectionPreview() {
    Surface {
        HazmatCard()
    }
}

@Preview("Hazmat non-clickable with selection")
@Composable
private fun NonClickableWithSelectionPreview() {
    Surface {
        HazmatCard(
            selectedCategory = ShippingLabelHazmatCategory.CLASS_1
        )
    }
}

@Preview("Hazmat clickable without selection")
@Composable
private fun ClickableWithoutSelectionPreview() {
    Surface {
        HazmatCard(
            onClick = {}
        )
    }
}

@Preview("Hazmat clickable with selection")
@Composable
private fun ClickableWithSelectionPreview() {
    Surface {
        HazmatCard(
            selectedCategory = ShippingLabelHazmatCategory.CLASS_1,
            onClick = {}
        )
    }
}
