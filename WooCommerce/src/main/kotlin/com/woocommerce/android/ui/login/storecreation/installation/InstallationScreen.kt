package com.woocommerce.android.ui.login.storecreation.installation

import android.annotation.SuppressLint
import android.view.ViewGroup.LayoutParams
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.drawShadow
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.SuccessState

@Composable
fun InstallationScreen(viewModel: InstallationViewModel) {
    viewModel.viewState.observeAsState(LoadingState).value.let { viewState ->
        when (viewState) {
            is SuccessState -> InstallationSummary(viewState.url, viewModel)
            is ErrorState -> InstallationError(viewModel)
            LoadingState -> InstallationInProgress()
        }
    }
}

@Composable
private fun InstallationSummary(url: String, viewModel: InstallationViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(id = string.store_creation_installation_success),
            color = colorResource(id = color.color_on_surface),
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
                .background(color = colorResource(id = color.color_surface))
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
                        color = colorResource(id = color.color_on_surface),
                        backgroundColor = colorResource(id = color.color_surface),
                        borderRadius = dimensionResource(id = R.dimen.major_100)
                    )
            )
        }

        Divider(
            color = colorResource(id = color.divider_color),
            thickness = dimensionResource(id = dimen.minor_10),
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_100))
        )

        WCColoredButton(
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth(),
            onClick = viewModel::onManageStoreButtonClicked
        ) {
            Text(
                text = stringResource(id = string.store_creation_installation_manage_store_button)
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
            onClick = viewModel::onShowPreviewButtonClicked
        ) {
            Text(
                text = stringResource(id = string.store_creation_installation_show_preview_button)
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun PreviewWebView(url: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(dimensionResource(id = dimen.minor_100))
            .fillMaxSize()
            .clip(RoundedCornerShape(dimensionResource(id = dimen.minor_100)))
            .border(
                dimensionResource(id = dimen.minor_10),
                colorResource(id = color.gray_0),
                shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100)),
            )
    ) {
        var progress by remember { mutableStateOf(0) }

        CircularProgressIndicator(
            progress = (progress / 100f),
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(if (progress == 100) 0f else 1f)
        )
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )

                    this.settings.javaScriptEnabled = true
                    this.settings.useWideViewPort = true
                    this.settings.loadWithOverviewMode = true
                    this.setInitialScale(50)

                    this.webViewClient = WebViewClient()

                    this.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (progress == 100) {
                                view?.settings?.javaScriptEnabled = false
                                view?.stopLoading()
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
                .clip(RoundedCornerShape(dimensionResource(id = dimen.minor_75)))
        )
    }
}

@Composable
private fun InstallationError(viewModel: InstallationViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(id = string.store_creation_installation_error),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
            textAlign = TextAlign.Center
        )

        WCColoredButton(
            onClick = viewModel::onRetryButtonClicked,
            text = stringResource(id = string.retry),
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        )
    }
}

@Composable
private fun InstallationInProgress() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))

        Text(
            text = stringResource(id = string.store_creation_in_progress),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp)
        )
    }
}
