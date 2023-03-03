package com.woocommerce.android.ui.login.storecreation.onboarding.launchstore

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.login.storecreation.onboarding.launchstore.LaunchStoreViewModel.LaunchStoreState

@Composable
fun LaunchStoreScreen(viewModel: LaunchStoreViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = stringResource(id = R.string.store_onboarding_launch_preview_title),
                onNavigationButtonClick = viewModel::onBackPressed,
            )
        }) { padding ->
            LaunchStoreScreen(
                state = state,
                onLaunchStoreClicked = viewModel::launchStore,
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
    onLaunchStoreClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        ) {
            EcommerceTrialBanner(modifier = Modifier.fillMaxWidth())
            SitePreview(url = state.siteUrl)
        }
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
                    .padding(dimensionResource(id = R.dimen.major_100)),
                onClick = { },
            ) {
                Text(text = stringResource(id = R.string.store_onboarding_launch_store_share_url_button))
            }
            WCOutlinedButton(onClick = {}) {
                Text(text = stringResource(id = R.string.store_onboarding_launch_store_back_to_store_button))
            }
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
fun SitePreview(url: String) {
    var progress by remember { mutableStateOf(0) }
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                this.settings.javaScriptEnabled = true
                this.settings.loadWithOverviewMode = true
                this.setInitialScale(140)

                this.webViewClient = WebViewClient()
                this.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        progress = newProgress
                        if (progress == 100) {
                            view?.settings?.javaScriptEnabled = false
                        }
                    }
                }

                this.setOnTouchListener { _, _ -> true }
            }.also {
                it.loadUrl(url)
            }
        },
        modifier = Modifier
            .alpha(if (progress == 100) 1f else 0f)
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_75)))
    )
}

@Composable
fun EcommerceTrialBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(color = colorResource(id = R.color.woo_purple_10))
            .padding(dimensionResource(id = R.dimen.major_100)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_tintable_info_outline_24dp),
            contentDescription = "",
            colorFilter = ColorFilter.tint(color = colorResource(id = R.color.color_icon))
        )
        Text(
            text = annotatedStringRes(R.string.store_onboarding_upgrade_plan_to_launch_store_banner_text),
            style = MaterialTheme.typography.body1,
        )
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun LaunchStoreScreenPreview() {

}
