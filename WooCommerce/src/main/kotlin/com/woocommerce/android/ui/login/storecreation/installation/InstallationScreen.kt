package com.woocommerce.android.ui.login.storecreation.installation

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.component.WebViewProgressIndicator.Circular
import com.woocommerce.android.ui.compose.drawShadow
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorScreen
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.CreatingStoreState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.SuccessState
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun InstallationScreen(
    viewModel: InstallationViewModel,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator
) {
    viewModel.viewState.observeAsState().value?.let { state ->
        Crossfade(targetState = state) { viewState ->
            when (viewState) {
                is SuccessState -> InstallationSummary(
                    viewState.url,
                    viewModel::onManageStoreButtonClicked,
                    viewModel::onShowPreviewButtonClicked,
                    viewModel::onUrlLoaded,
                    userAgent,
                    authenticator
                )
                is ErrorState -> StoreCreationErrorScreen(
                    viewState.errorType,
                    viewModel::onBackPressed,
                    viewState.message,
                    viewModel::onRetryButtonClicked
                )
                is CreatingStoreState -> CreateStoreState(
                    viewState = viewState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun InstallationSummary(
    url: String,
    onManageStoreButtonClicked: () -> Unit,
    onShowPreviewButtonClicked: () -> Unit,
    onUrlLoaded: (String) -> Unit,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_installation_success),
            color = colorResource(id = R.color.color_on_surface),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .padding(
                    top = dimensionResource(id = R.dimen.major_350)
                )
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .background(color = colorResource(id = R.color.color_surface))
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_350),
                    vertical = dimensionResource(id = R.dimen.major_200)
                )
        ) {
            PreviewWebView(
                url = url,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .drawShadow(
                        color = colorResource(id = R.color.color_on_surface),
                        backgroundColor = colorResource(id = R.color.color_surface),
                        borderRadius = dimensionResource(id = R.dimen.major_100)
                    ),
                userAgent = userAgent,
                authenticator = authenticator,
                onUrlLoaded = onUrlLoaded
            )
        }

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10),
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_100))
        )

        WCColoredButton(
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth(),
            onClick = onManageStoreButtonClicked
        ) {
            Text(
                text = stringResource(id = R.string.store_creation_installation_manage_store_button)
            )
        }

        WCOutlinedButton(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.major_100)
                )
                .fillMaxWidth(),
            onClick = onShowPreviewButtonClicked
        ) {
            Text(
                text = stringResource(id = R.string.store_creation_installation_show_preview_button)
            )
        }
    }
}

@Composable
fun CreateStoreState(
    viewState: CreatingStoreState,
    modifier: Modifier = Modifier
) {
    val progress by remember { mutableStateOf(viewState.progress / viewState.maxProgress) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_250))
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_150)),
        ) {
            Text(
                text = stringResource(id = viewState.title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .height(dimensionResource(id = R.dimen.minor_100))
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
                    .fillMaxWidth(),
                color = MaterialTheme.colors.primary,
                backgroundColor = colorResource(id = R.color.divider_color),
            )
            Text(
                text = stringResource(id = viewState.description),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun PreviewWebView(
    url: String,
    modifier: Modifier = Modifier,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator,
    onUrlLoaded: (String) -> Unit
) {
    Box(
        modifier = modifier
            .padding(dimensionResource(id = R.dimen.minor_100))
            .fillMaxSize()
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .border(
                dimensionResource(id = R.dimen.minor_10),
                colorResource(id = R.color.empty_state_bg_color),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)),
            )
    ) {
        WCWebView(
            url = url,
            userAgent = userAgent,
            wpComAuthenticator = authenticator,
            captureBackPresses = false,
            loadWithOverviewMode = true,
            isReadOnly = true,
            initialScale = 140,
            progressIndicator = Circular(
                stringResource(id = R.string.store_creation_installation_rendering_preview_label)
            ),
            onUrlLoaded = onUrlLoaded,
            modifier = modifier.background(color = colorResource(id = R.color.color_surface))
        )
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun CreateStoreStatePreview() {
    WooThemeWithBackground {
        CreateStoreState(
            viewState = CreatingStoreState(
                progress = 0,
                title = string.store_creation_in_progress_title_1,
                description = string.store_creation_in_progress_description_1
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = color.color_surface))
        )
    }
}
