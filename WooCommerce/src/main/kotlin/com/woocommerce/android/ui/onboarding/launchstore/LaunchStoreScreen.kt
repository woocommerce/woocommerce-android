package com.woocommerce.android.ui.onboarding.launchstore

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.web.WCWebView
import com.woocommerce.android.ui.compose.component.web.WebViewProgressIndicator.Circular
import com.woocommerce.android.ui.compose.drawShadow
import com.woocommerce.android.ui.onboarding.launchstore.LaunchStoreViewModel.LaunchStoreState
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun LaunchStoreScreen(viewModel: LaunchStoreViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = if (!state.isStoreLaunched) {
                    stringResource(id = R.string.store_onboarding_launch_preview_title)
                } else {
                    ""
                },
                onNavigationButtonClick = viewModel::onBackPressed,
            )
        }) { padding ->
            LaunchStoreScreen(
                state = state,
                userAgent = viewModel.userAgent,
                authenticator = viewModel.wpComWebViewAuthenticator,
                onLaunchStoreClicked = viewModel::launchStore,
                onShareStoreUrl = viewModel::shareStoreUrl,
                onBackToStoreClicked = viewModel::onBackPressed,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.surface)
                    .padding(padding)
            )
        }
    }
}

@Composable
fun LaunchStoreScreen(
    state: LaunchStoreState,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator,
    onLaunchStoreClicked: () -> Unit,
    onShareStoreUrl: () -> Unit,
    onBackToStoreClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isStoreLaunched) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = dimensionResource(id = R.dimen.major_100))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(size = dimensionResource(id = R.dimen.major_85))
                                .clip(shape = CircleShape)
                                .background(color = colorResource(id = R.color.color_info))
                        )
                        Text(
                            text = stringResource(id = R.string.store_onboarding_launched_title),
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = state.displayUrl,
                        style = MaterialTheme.typography.caption
                    )
                    SitePreview(
                        url = state.siteUrl,
                        userAgent = userAgent,
                        authenticator = authenticator,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                WebView(
                    state.siteUrl,
                    userAgent,
                    authenticator,
                    isReadOnly = false
                )
            }
        }
        ActionsFooter(state, onLaunchStoreClicked, onShareStoreUrl, onBackToStoreClicked)
    }
}

@Composable
private fun WebView(
    url: String,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator,
    isReadOnly: Boolean
) {
    WCWebView(
        url = url,
        userAgent = userAgent,
        wpComAuthenticator = authenticator,
        captureBackPresses = false,
        loadWithOverviewMode = true,
        isReadOnly = isReadOnly,
        progressIndicator = Circular(
            stringResource(id = string.store_onboarding_launch_store_rendering_preview_label)
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.color_surface))
    )
}

@Composable
private fun ActionsFooter(
    state: LaunchStoreState,
    onLaunchStoreClicked: () -> Unit,
    onShareStoreUrl: () -> Unit,
    onBackToStoreClicked: () -> Unit
) {
    Divider(
        color = colorResource(id = R.color.divider_color),
        thickness = dimensionResource(id = R.dimen.minor_10)
    )
    if (!state.isStoreLaunched) {
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
            onClick = onLaunchStoreClicked,
            enabled = !state.isTrialPlan
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)),
                    color = colorResource(id = R.color.color_on_primary_surface),
                )
            } else {
                Text(text = stringResource(id = R.string.store_onboarding_launch_store_button))
            }
        }
    } else {
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(id = R.dimen.minor_100),
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                ),
            onClick = onShareStoreUrl,
        ) {
            Text(text = stringResource(id = R.string.store_onboarding_launch_store_share_url_button))
        }
        WCOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.major_100)
                ),
            onClick = onBackToStoreClicked
        ) {
            Text(text = stringResource(id = R.string.store_onboarding_launch_store_back_to_store_button))
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
fun SitePreview(
    url: String,
    userAgent: UserAgent,
    authenticator: WPComWebViewAuthenticator,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color = colorResource(id = R.color.color_surface))
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_350),
                vertical = dimensionResource(id = R.dimen.major_200)
            )
    ) {
        Box(
            modifier = Modifier
                .drawShadow(
                    color = colorResource(id = R.color.color_on_surface),
                    backgroundColor = colorResource(id = R.color.color_surface),
                    borderRadius = dimensionResource(id = R.dimen.major_100)
                )
                .padding(dimensionResource(id = R.dimen.minor_100))
                .fillMaxSize()
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
                .border(
                    dimensionResource(id = R.dimen.minor_10),
                    colorResource(id = R.color.empty_state_bg_color),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)),
                )
        ) {
            WebView(
                url,
                userAgent,
                authenticator,
                isReadOnly = true
            )
        }
    }
}
