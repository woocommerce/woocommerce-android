package com.woocommerce.android.ui.products.selector.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.products.selector.SelectionState
import com.woocommerce.android.ui.products.selector.SelectionState.PARTIALLY_SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.UNSELECTED

@Composable
fun SelectorListItem(
    title: String,
    imageUrl: String?,
    infoLine1: String?,
    infoLine2: String?,
    onClickLabel: String?,
    imageContentDescription: String?,
    selectionState: SelectionState,
    isArrowVisible: Boolean,
    isCogwheelVisible: Boolean,
    enabled: Boolean,
    onEditConfiguration: () -> Unit,
    onItemClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    onItemClick()
                },
                onClickLabel = onClickLabel
            )
            .padding(
                start = dimensionResource(id = dimen.major_100),
                top = dimensionResource(id = dimen.major_75),
                bottom = dimensionResource(id = dimen.major_75),
                end = dimensionResource(id = dimen.minor_75)
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_100))
    ) {
        val selectionDrawable = when (selectionState) {
            SELECTED -> drawable.ic_rounded_chcekbox_checked
            UNSELECTED -> drawable.ic_rounded_chcekbox_unchecked
            PARTIALLY_SELECTED -> drawable.ic_rounded_chcekbox_partially_checked
        }
        Crossfade(
            targetState = selectionDrawable,
            modifier = Modifier.padding(top = dimensionResource(dimen.major_75))
        ) { icon ->
            Image(
                painter = painterResource(id = icon),
                contentDescription = imageContentDescription
            )
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(drawable.ic_product),
            error = painterResource(drawable.ic_product),
            contentDescription = stringResource(string.product_image_content_description),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .size(dimensionResource(dimen.major_300))
                .clip(RoundedCornerShape(3.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )

            if (!infoLine1.isNullOrEmpty()) {
                SelectorListItemInfo(infoLine1)
            }

            if (!infoLine2.isNullOrEmpty()) {
                SelectorListItemInfo(infoLine2)
            }
        }

        if (isArrowVisible || isCogwheelVisible) {
            val image = if (isArrowVisible) {
                Icons.Filled.ChevronRight
            } else {
                Icons.Filled.Tune
            }

            val contentDescription = if (isArrowVisible) {
                stringResource(id = string.product_selector_arrow_content_description)
            } else {
                stringResource(id = string.extension_configure_button)
            }

            val color = if (isArrowVisible) {
                MaterialTheme.colors.onSurface
            } else {
                MaterialTheme.colors.primary
            }

            val iconModifier = if (isArrowVisible) {
                Modifier
                    .size(dimensionResource(id = dimen.major_200))
            } else {
                Modifier
                    .clickable { onEditConfiguration() }
                    .padding(8.dp)
                    .size(dimensionResource(id = dimen.major_150))
            }

            Icon(
                imageVector = image,
                contentDescription = contentDescription,
                modifier = iconModifier.align(Alignment.CenterVertically),
                tint = color
            )
        }
    }
}

@Composable
private fun SelectorListItemInfo(
    summary: String,
) {
    Text(
        text = summary,
        style = MaterialTheme.typography.caption,
        color = colorResource(id = color.color_on_surface_medium)
    )
}

@Preview
@Composable
private fun SelectorListItemPreview() =
    SelectorListItem(
        title = "Item name",
        imageUrl = null,
        infoLine1 = "Information 1",
        infoLine2 = "Information 2",
        selectionState = SELECTED,
        isArrowVisible = false,
        onItemClick = {},
        onClickLabel = null,
        imageContentDescription = null,
        isCogwheelVisible = true,
        enabled = true,
        onEditConfiguration = {}
    )
