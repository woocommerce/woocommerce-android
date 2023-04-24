package com.woocommerce.android.ui.login.storecreation.installation

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.component.WebViewProgressIndicator.Circular
import com.woocommerce.android.ui.compose.drawShadow
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorScreen
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.StoreCreationLoadingState
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.SuccessState
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun InstallationScreen(
    viewModel: StoreInstallationViewModel,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator
) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
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

            is StoreCreationLoadingState -> CreateStoreState(
                viewState = viewState,
                modifier = Modifier.fillMaxSize()
            )
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
    viewState: StoreCreationLoadingState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(MaterialTheme.colors.surface)) {
        Image(
            modifier = Modifier.align(Alignment.TopEnd),
            painter = painterResource(id = R.drawable.store_creation_loading_image_top_end),
            contentDescription = null,
        )
        Image(
            modifier = Modifier.align(Alignment.BottomStart),
            painter = painterResource(id = R.drawable.store_creation_loading_image_bottom_start),
            contentDescription = null,
        )
        ConstraintLayout(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_250))
                .fillMaxSize(),
        ) {
            val (title, progress, description) = createRefs()
            val margin = dimensionResource(id = R.dimen.major_150)
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(title) {
                        bottom.linkTo(
                            anchor = progress.top,
                            margin = margin
                        )
                    },
                text = stringResource(id = viewState.title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
            )
            AnimatedProgress(
                progress = viewState.progress,
                modifier = Modifier.constrainAs(progress) {
                    centerHorizontallyTo(parent)
                    centerVerticallyTo(parent)
                }
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(description) {
                        top.linkTo(
                            anchor = progress.bottom,
                            margin = margin
                        )
                    },
                text = annotatedStringRes(stringResId = viewState.description),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }
    }
}

@Composable
private fun AnimatedProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    LinearProgressIndicator(
        progress = animatedProgress,
        color = MaterialTheme.colors.primary,
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.minor_100))
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))),
        backgroundColor = colorResource(id = R.color.creating_store_linear_progress_background),
    )
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
@Suppress("unused")
private fun CreateStoreStatePreview() {
    WooThemeWithBackground {
        CreateStoreState(
            viewState = StoreCreationLoadingState(
                progress = 0F,
                title = R.string.store_creation_in_progress_title_3,
                description = R.string.store_creation_in_progress_description_1
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.color_surface))
        )
    }
}
