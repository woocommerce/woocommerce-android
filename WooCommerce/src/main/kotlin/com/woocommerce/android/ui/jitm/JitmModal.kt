package com.woocommerce.android.ui.jitm

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.UiHelpers

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun JitmModal(state: JitmState.Modal) {
    Dialog(
        onDismissRequest = { state.onDismissClicked },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(size = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState()),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                if (isSystemInDarkTheme()) {
                                    state.backgroundDarkImageUrl
                                } else {
                                    state.backgroundLightImageUrl
                                }
                            )
                            .fallback(R.drawable.img_woo_generic_error)
                            .error(R.drawable.img_woo_generic_error)
                            .decoderFactory(SvgDecoder.Factory())
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .height(194.dp)
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                    Text(
                        text = UiHelpers.getTextOfUiString(LocalContext.current, state.title),
                        style = MaterialTheme.typography.h5,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))

                    Text(
                        text = UiHelpers.getTextOfUiString(LocalContext.current, state.description),
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_125)))

                    WCColoredButton(
                        onClick = { state.onPrimaryActionClicked.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    ) {
                        Text(text = UiHelpers.getTextOfUiString(LocalContext.current, state.primaryActionLabel))
                    }

                    WCTextButton(
                        onClick = { state.onDismissClicked.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    ) {
                        Text(text = stringResource(id = R.string.skip))
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                }
            }
        }
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun JitmDialogPreview() {
    WooThemeWithBackground {
        JitmModal(
            JitmState.Modal(
                onPrimaryActionClicked = {},
                onDismissClicked = {},
                title = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_title),
                description = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_description),
                primaryActionLabel = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_cta),
                backgroundLightImageUrl = "",
                backgroundDarkImageUrl = ""
            )
        )
    }
}
