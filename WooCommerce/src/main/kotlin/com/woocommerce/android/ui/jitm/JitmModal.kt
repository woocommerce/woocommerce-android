package com.woocommerce.android.ui.jitm

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.util.UiHelpers

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun JitmModal(state: JitmState.Modal) {
    Dialog(
        onDismissRequest = state.onDismissClicked,
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
                shape = RoundedCornerShape(WooTheme.radius.extraLarge),
                color = WooTheme.colors.surface.bright,
                contentColor = WooTheme.colors.surface.onDefault,
                shadowElevation = WooTheme.spacing.space0,
                tonalElevation = WooTheme.spacing.space0,
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
                            .padding(
                                start = WooTheme.padding.padding5,
                                top = WooTheme.padding.padding5,
                                end = WooTheme.padding.padding5,
                            )
                            .fillMaxWidth()
                            .height(JITM_MODAL_HERO_HEIGHT)
                    )

                    Spacer(modifier = Modifier.height(WooTheme.spacing.space5))

                    Text(
                        text = UiHelpers.getTextOfUiString(LocalContext.current, state.title),
                        color = WooTheme.colors.surface.onDefault,
                        style = WooTheme.text.titleLarge.strong,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WooTheme.padding.padding5),
                    )

                    Spacer(modifier = Modifier.height(WooTheme.spacing.space3))

                    Text(
                        text = UiHelpers.getTextOfUiString(LocalContext.current, state.description),
                        color = WooTheme.colors.surface.onVariant,
                        style = WooTheme.text.bodyMedium.regular,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WooTheme.padding.padding5),
                    )

                    Spacer(modifier = Modifier.height(WooTheme.spacing.space6))

                    WooFilledButton(
                        text = UiHelpers.getTextOfUiString(LocalContext.current, state.primaryActionLabel),
                        onClick = state.onPrimaryActionClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WooTheme.padding.padding5),
                        size = WooButtonSize.Medium,
                    )

                    Spacer(modifier = Modifier.height(WooTheme.spacing.space3))

                    WooOutlinedButton(
                        text = stringResource(id = R.string.skip),
                        onClick = state.onDismissClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WooTheme.padding.padding5),
                        size = WooButtonSize.Medium,
                    )
                    Spacer(modifier = Modifier.height(WooTheme.spacing.space5))
                }
            }
        }
    )
}

@PreviewLightDark
@Composable
fun JitmDialogPreview() {
    WooDesignSystemThemeWithBackground {
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

private val JITM_MODAL_HERO_HEIGHT = 194.dp
