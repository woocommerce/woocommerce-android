package com.woocommerce.android.ui.products.ai

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Initial
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.Keyword

@Composable
fun PackagePhotoBottomSheet(viewModel: PackagePhotoViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        ProductFromPackagePhoto(viewState = state, modifier = Modifier, viewModel::onKeywordChanged)
    }
}

@Composable
fun ProductFromPackagePhoto(
    viewState: ViewState,
    modifier: Modifier,
    onKeywordChanged: (Int, Keyword) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = dimen.minor_100),
            topEnd = dimensionResource(id = dimen.minor_100)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
            modifier = modifier
                .background(MaterialTheme.colors.surface)
                .padding(dimensionResource(id = dimen.major_100))
        ) {

            ProductImage(viewState)

            Spacer(Modifier)

            NameAndDescription(viewState, modifier)

            Spacer(Modifier)

            Keywords(viewState, onKeywordChanged)
        }
    }
}

@Composable
private fun ProductImage(viewState: ViewState) {
    ConstraintLayout(
        modifier = Modifier
            .border(
                width = dimensionResource(id = dimen.minor_10),
                color = colorResource(id = color.divider_color),
                shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
            )
            .fillMaxWidth()
    ) {
        val (image, button) = createRefs()

        AsyncImage(
            modifier = Modifier
                .constrainAs(image) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            model = Builder(LocalContext.current)
                .data(viewState.imageUrl)
                .crossfade(true)
                .placeholder(drawable.img_empty_products)
                .error(drawable.img_empty_products)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Inside,
        )

        WCColoredButton(
            modifier = Modifier
                .padding(top = dimensionResource(id = dimen.major_100))
                .constrainAs(button) {
                    centerAround(image.top)
                    centerAround(image.end)
                }.size(dimensionResource(id = dimen.button_height_major_100)),
            shape = CircleShape,
            onClick = { /*TODO*/ },
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colorResource(id = color.woo_white),
            )
        }
    }
}

@Composable
fun NameAndDescription(
    viewState: ViewState,
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
        modifier = modifier
    ) {
        val sectionsBorder = Modifier.border(
            width = dimensionResource(id = dimen.minor_10),
            color = colorResource(id = color.divider_color),
            shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
        )

        Text(
            text = stringResource(id = string.product_creation_ai_preview_name_section),
            style = MaterialTheme.typography.body2
        )
        Text(
            text = viewState.title,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
                .padding(dimensionResource(id = dimen.major_100))
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = string.product_creation_ai_preview_description_section),
            style = MaterialTheme.typography.body2
        )
        Text(
            text = viewState.description,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
                .padding(dimensionResource(id = dimen.major_100))
        )
    }
}

@Composable
private fun Keywords(
    viewState: ViewState,
    onKeywordChanged: (Int, Keyword) -> Unit
) {
    val sectionsBorder = Modifier.border(
        width = dimensionResource(id = dimen.minor_10),
        color = colorResource(id = color.divider_color),
        shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
    )

    Text(
        text = stringResource(id = string.product_creation_package_photo_keywords_section),
        style = MaterialTheme.typography.body2
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
        modifier = Modifier
            .fillMaxWidth()
            .then(sectionsBorder)
    ) {
        WCColoredButton(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100))
                .fillMaxWidth(),
            text = stringResource(string.ai_product_name_sheet_regenerate_button),
            onClick = { /*TODO*/ }
        )
        Text(
            text = stringResource(id = string.product_creation_package_photo_keywords_info),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = color.color_on_surface_medium),
            modifier = Modifier.padding(horizontal = dimensionResource(id = dimen.major_100))
        )

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(bottom = dimensionResource(id = dimen.minor_100))
                .background(color = MaterialTheme.colors.surface)
        ) {
            itemsIndexed(items = viewState.keywords) { index, keyword ->
                KeywordListItem(index, keyword.title, keyword.isChecked, onKeywordChanged)

                if (index < viewState.keywords.lastIndex) {
                    Divider(
                        color = colorResource(id = color.divider_color),
                        thickness = dimensionResource(id = dimen.minor_10)
                    )
                }
            }
        }
    }
}

@Composable
fun KeywordListItem(
    index: Int,
    title: String,
    isSelected: Boolean,
    onKeywordChanged: (Int, Keyword) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(
                horizontal = dimensionResource(id = dimen.major_100)
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_100))
    ) {
        val selectionDrawable = when (isSelected) {
            true -> drawable.ic_rounded_chcekbox_checked
            false -> drawable.ic_rounded_chcekbox_unchecked
        }

        BasicTextField(
            value = title,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f),
            textStyle = MaterialTheme.typography.body1,
            onValueChange = {
                onKeywordChanged(index, Keyword(it, isSelected))
            },
        )

        Crossfade(
            targetState = selectionDrawable,
            label = "",
        ) { icon ->
            val onClick = {
                onKeywordChanged(index, Keyword(title, !isSelected))
            }
            IconButton(onClick) {
                Image(painter = painterResource(id = icon), contentDescription = "")
            }
        }
    }
}

@Preview
@Composable
fun PreviewProductFromPackagePhoto() {
    ProductFromPackagePhoto(
        viewState = ViewState(
            title = "Title",
            description = "Description",
            keywords = listOf(Keyword("Keyword 1", true), Keyword("Keyword 2", false)),
            generationState = Initial,
            imageUrl = ""
        ),
        modifier = Modifier,
        onKeywordChanged = { _, _ -> }
    )
}
