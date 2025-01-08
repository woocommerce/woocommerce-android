package com.woocommerce.android.ui.orders.wooshippinglabels.packages.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.SelectionCheck
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData

@Composable
fun WooShippingPackageListItem(
    modifier: Modifier,
    packageData: PackageData,
    onPackageSelected: (PackageData, Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onPackageSelected(packageData, packageData.isSelected.not()) }
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionCheck(
                isSelected = packageData.isSelected,
                onSelectionChange = { onPackageSelected(packageData, it) }
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = packageData.descriptionResId),
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = R.color.color_on_surface_disabled)
                )
                Text(
                    text = packageData.name,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = packageData.weight
                        .takeIf { it.isNotEmpty() }
                        ?.let { "${packageData.dimensionForDisplay} • ${packageData.weightForDisplay}" }
                        ?: packageData.dimensionForDisplay,
                    style = MaterialTheme.typography.body2
                )
            }
        }
        Divider()
    }
}

@Composable
fun WooShippingPackageListItemSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonView(
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SkeletonView(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.5f)
                )
                SkeletonView(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(0.7f)
                )
                SkeletonView(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.6f)
                )
            }
        }
        Divider()
    }
}

@Preview
@Composable
fun WooSavedPackageListItemPreview() {
    WooThemeWithBackground {
        WooShippingPackageListItem(
            modifier = Modifier,
            packageData = PackageData(
                name = "Small Flat Rate Box",
                dimensions = "5 x 5 x 5",
                weight = "1.5",
                isLetter = false,
                isSelected = false,
                id = "1",
            ),
            onPackageSelected = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun WooSavedPackageListItemSkeletonPreview() {
    WooThemeWithBackground {
        WooShippingPackageListItemSkeleton()
    }
}
