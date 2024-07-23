package com.woocommerce.android.ui.products.ai.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.Image
import com.woocommerce.android.ui.compose.component.ProductThumbnail

@Composable
fun SelectImageSection(
    image: Image?,
    onImageActionSelected: (ImageAction) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    dropDownActions: List<ImageAction> = ImageAction.entries,
    onReadTextFromProductPhoto: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clickable { onReadTextFromProductPhoto?.invoke() }
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            image == null -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_gridicons_camera_primary),
                    contentDescription = stringResource(id = R.string.product_image_content_description),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color = colorResource(id = R.color.woo_gray_5))
                        .padding(16.dp)
                )
            }

            else ->
                ProductThumbnail(
                    imageUrl = image.uri,
                    contentDescription = stringResource(id = R.string.product_image_content_description),
                    modifier = Modifier.padding(end = 16.dp)
                )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    image == null -> stringResource(id = R.string.ai_product_creation_read_text_from_photo_button)
                    else -> stringResource(id = R.string.ai_product_creation_image_selected)
                },
                style = MaterialTheme.typography.subtitle1,
                color = when {
                    image == null -> MaterialTheme.colors.primary
                    else -> MaterialTheme.colors.onSurface
                },
                fontWeight = FontWeight.Bold,
            )

            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                )
            }
        }

        ImageActionsMenu(
            dropDownActions,
            onImageActionSelected
        )
    }
}

@Composable
private fun ImageActionsMenu(
    actions: List<ImageAction>,
    onImageActionSelected: (ImageAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = !showMenu }) {
            Icon(
                imageVector = Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu),
                tint = colorResource(id = R.color.color_on_surface_medium)
            )
        }
        DropdownMenu(
            offset = DpOffset(
                x = dimensionResource(id = R.dimen.major_100),
                y = 0.dp
            ),
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            actions.forEachIndexed { index, item ->
                DropdownMenuItem(
                    modifier = Modifier
                        .height(dimensionResource(id = R.dimen.major_175))
                        .width(200.dp),
                    onClick = {
                        showMenu = false
                        onImageActionSelected(item)
                    }
                ) {
                    Text(
                        text = stringResource(id = item.displayName),
                        color = when (item) {
                            ImageAction.Remove -> MaterialTheme.colors.error
                            else -> Color.Unspecified
                        }
                    )
                }
                if (index < actions.size - 1) {
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                }
            }
        }
    }
}

enum class ImageAction(@StringRes val displayName: Int) {
    View(R.string.ai_product_creation_view_image),
    Replace(R.string.ai_product_creation_replace_image),
    Remove(R.string.ai_product_creation_remove_image)
}
