package com.woocommerce.android.ui.blaze

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeCampaignCreationViewModel.BlazeCreationViewState
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun BlazeCampaignCreationScreen(
    viewModel: BlazeCampaignCreationViewModel,
    userAgent: UserAgent,
    wpcomWebViewAuthenticator: WPComWebViewAuthenticator,
    activityRegistry: ActivityResultRegistry,
    onClose: () -> Unit
) {
    viewModel.viewState.observeAsState().value?.let {
        BlazeCampaignCreationScreen(
            viewState = it,
            userAgent = userAgent,
            wpcomWebViewAuthenticator = wpcomWebViewAuthenticator,
            activityRegistry = activityRegistry,
            onClose = onClose
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BlazeCampaignCreationScreen(
    viewState: BlazeCreationViewState,
    userAgent: UserAgent,
    wpcomWebViewAuthenticator: WPComWebViewAuthenticator,
    activityRegistry: ActivityResultRegistry,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.more_menu_button_blaze),
                onNavigationButtonClick = onClose,
                navigationIcon = Filled.ArrowBack
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        AnimatedContent(
            targetState = viewState,
            transitionSpec = { fadeIn() with fadeOut() }
        ) { targetState ->
            when (targetState) {
                is BlazeCreationViewState.Intro -> BlazeCreationIntroScreen(
                    onCreateCampaignClick = targetState.onCreateCampaignClick,
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                )

                is BlazeCreationViewState.BlazeWebViewState -> WCWebView(
                    url = targetState.urlToLoad,
                    userAgent = userAgent,
                    wpComAuthenticator = wpcomWebViewAuthenticator,
                    onPageFinished = targetState.onPageFinished,
                    activityRegistry = activityRegistry,
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun BlazeCreationIntroScreen(
    onCreateCampaignClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Column(
            Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .verticalScroll(rememberScrollState())
                .weight(1f)
        ) {
            Spacer(modifier = Modifier.weight(0.8f))

            Image(
                painter = painterResource(id = R.drawable.ic_blaze),
                contentDescription = null,
                modifier = Modifier
                    .background(
                        color = colorResource(id = R.color.woo_orange_5).copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .size(dimensionResource(id = R.dimen.image_major_120))
                    .padding(dimensionResource(id = R.dimen.major_250))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
            Text(
                text = stringResource(id = R.string.blaze_campaign_creation_intro_title),
                style = MaterialTheme.typography.h4
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
            Text(
                text = "• ${stringResource(id = R.string.blaze_campaign_creation_intro_benefit_1)}",
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = "• ${stringResource(id = R.string.blaze_campaign_creation_intro_benefit_2)}",
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = "• ${stringResource(id = R.string.blaze_campaign_creation_intro_benefit_3)}",
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = "• ${stringResource(id = R.string.blaze_campaign_creation_intro_benefit_4)}",
                style = MaterialTheme.typography.subtitle1
            )

            Spacer(modifier = Modifier.weight(1.3f))
        }

        Divider()

        Button(
            onClick = onCreateCampaignClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(text = stringResource(id = R.string.blaze_campaign_creation_intro_button))
        }
    }
}

@Composable
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun BlazeCreationIntroPreview() {
    WooThemeWithBackground {
        BlazeCreationIntroScreen(onCreateCampaignClick = {})
    }
}
