package com.woocommerce.android.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.products.ProductSharingViewModel.ViewState.ProductSharingViewState

@Composable
fun ProductSharingBottomSheet(viewModel: ProductSharingViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        when (it) {
            is ProductSharingViewState -> {
                ProductShareWithAI(
                    viewState = it,
                    onGenerateButtonClick = viewModel::onGenerateButtonClicked,
                )
            }

            else -> {
                // TODO
            }
        }
    }
}

@Composable
fun ProductShareWithAI(
    viewState: ProductSharingViewState,
    onGenerateButtonClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
    ) {
        Text(
            text = stringResource(id = R.string.share) + " ${viewState.productTitle}",
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
        )
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            WCOutlinedTextField(
                value = viewState.shareMessage,
                onValueChange = { /*TODO*/ },
                label = stringResource(id = R.string.product_sharing_optional_message_label),
                maxLines = 5
            )

            Row(
                modifier = Modifier
                    .padding(
                        vertical = dimensionResource(id = R.dimen.minor_100)
                    )
            ) {
                WCOutlinedButton(onClick = onGenerateButtonClick) {
                    Text(
                        text = stringResource(id = R.string.product_sharing_write_with_ai)
                    )
                }
                IconButton(
                    onClick = { /* TODO */ }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info_outline_20dp),
                        contentDescription = stringResource(
                            id = R.string.product_sharing_ai_info_label
                        ),
                        tint = colorResource(id = R.color.color_on_surface)
                    )
                }
            }

            WCColoredButton(
                onClick = { /*TODO*/ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.share))
            }
        }
    }
}
