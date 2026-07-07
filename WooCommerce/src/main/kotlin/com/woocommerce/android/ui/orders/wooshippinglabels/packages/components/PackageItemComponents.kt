package com.woocommerce.android.ui.orders.wooshippinglabels.packages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.SelectionCheck
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData

@Composable
fun WooShippingPackageListItem(
    packageData: PackageData,
    onPackageSelected: (PackageData, Boolean) -> Unit,
    packageItemSupportsStarring: Boolean,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
    onPackageStarred: (PackageData, Boolean) -> Unit = { _, _ -> },
    onPackageRemoved: ((PackageData) -> Unit)? = null,
) {
    Column(modifier = modifier) {
        if (onPackageRemoved == null) {
            WooShippingPackageListItemContent(
                packageData = packageData,
                onPackageSelected = onPackageSelected,
                packageItemSupportsStarring = packageItemSupportsStarring,
                onPackageStarred = onPackageStarred
            )
        } else {
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState()

            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                modifier = modifier.fillMaxSize(),
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    if (swipeToDismissBoxState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_delete_filled_24dp),
                            contentDescription = stringResource(id = R.string.remove),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colorResource(R.color.woo_red_50))
                                .wrapContentSize(Alignment.CenterEnd)
                                .padding(end = 16.dp),
                            tint = colorResource(R.color.woo_white)
                        )
                    }
                },
                onDismiss = {
                    if (it == SwipeToDismissBoxValue.EndToStart) onPackageRemoved(packageData)
                    it != SwipeToDismissBoxValue.EndToStart
                }
            ) {
                WooShippingPackageListItemContent(
                    packageData = packageData,
                    onPackageSelected = onPackageSelected,
                    packageItemSupportsStarring = packageItemSupportsStarring,
                    onPackageStarred = onPackageStarred
                )
            }
        }
        if (divider) Divider(modifier = Modifier.padding(start = 48.dp))
    }
}

@Composable
fun WooShippingPackageListItemContent(
    packageData: PackageData,
    onPackageSelected: (PackageData, Boolean) -> Unit,
    packageItemSupportsStarring: Boolean,
    modifier: Modifier = Modifier,
    onPackageStarred: (PackageData, Boolean) -> Unit = { _, _ -> }
) {
    Surface {
        Row(
            modifier = modifier
                .clickable { onPackageSelected(packageData, !packageData.isSelected) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionCheck(
                isSelected = packageData.isSelected,
                onSelectionChange = { onPackageSelected(packageData, it) }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
            if (packageItemSupportsStarring) {
                Icon(
                    modifier = Modifier.clickable { onPackageStarred(packageData, !packageData.isStarred) },
                    tint = colorResource(id = R.color.color_on_surface_disabled),
                    imageVector = ImageVector.vectorResource(
                        if (packageData.isStarred) R.drawable.ic_star_filled_24dp else R.drawable.ic_star_24dp
                    ),
                    contentDescription = stringResource(
                        id = when (packageData.isStarred) {
                            true -> R.string.woo_shipping_labels_package_creation_unstarred
                            else -> R.string.woo_shipping_labels_package_creation_starred
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun WooShippingPackageListItemSkeleton(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
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
            onPackageSelected = { _, _ -> },
            packageItemSupportsStarring = true
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
