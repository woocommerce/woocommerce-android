package com.woocommerce.android.ui.products

import android.content.res.Configuration
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ProductSharingViewModel.AIButtonState.Regenerate
import com.woocommerce.android.ui.products.ProductSharingViewModel.AIButtonState.WriteWithAI
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewState.buttonState is Regenerate) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_refresh_grey),
                                contentDescription = null
                            )
                        }
                        Text(
                            text = viewState.buttonState.label,
                            modifier = Modifier
                                .padding(
                                    horizontal = dimensionResource(id = R.dimen.minor_100)
                                )
                        )
                    }
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

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Smaller screen", device = Devices.NEXUS_5)
@Composable
fun DefaultUIWithSharingContent() {
    val shareMessage =
        "Hey! 🎵 I just listened to the new album \"Album Title\" by Artist Name, and it's fantastic! Check it out " +
            "now on your favorite music platform and join the conversation using #AlbumTitleByArtistName. Let's " +
            "spread the love for this amazing music! 🎧💕 #NewMusicAlert"
    WooThemeWithBackground {
        ProductShareWithAI(
            viewState = ProductSharingViewState(
                productTitle = "Music Album",
                shareMessage = shareMessage,
                buttonState = WriteWithAI(stringResource(id = R.string.product_sharing_write_with_ai))
            )
        )
    }
}

@Preview
@Composable
fun DefaultUIWithRegenerateButton() {
    val shareMessage =
        "Hey! 🎵 I just listened to the new album \"Album Title\" by Artist Name, and it's fantastic! Check it out " +
            "now on your favorite music platform and join the conversation using #AlbumTitleByArtistName. Let's " +
            "spread the love for this amazing music! 🎧💕 #NewMusicAlert"
    WooThemeWithBackground {
        ProductShareWithAI(
            viewState = ProductSharingViewState(
                productTitle = "Music Album",
                shareMessage = shareMessage,
                buttonState = Regenerate(stringResource(id = R.string.product_sharing_regenerate))
            )
        )
    }
}
