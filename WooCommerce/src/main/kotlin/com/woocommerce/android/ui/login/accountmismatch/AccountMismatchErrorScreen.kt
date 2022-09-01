package com.woocommerce.android.ui.login.accountmismatch

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun AccountMismatchErrorScreen(viewModel: AccountMismatchErrorViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        Scaffold(topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                title = { },
                actions = {
                    IconButton(onClick = viewModel::onHelpButtonClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_help_24dp),
                            contentDescription = stringResource(id = R.string.help)
                        )
                    }
                },
                elevation = 0.dp
            )
        }) { paddingValues ->
            AccountMismatchErrorScreen(viewState = viewState, modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun AccountMismatchErrorScreen(viewState: AccountMismatchErrorViewModel.ViewState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        MainContent(
            viewState = viewState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        )

        ButtonBar(
            primaryButtonText = viewState.primaryButtonText?.let { stringResource(id = it) },
            primaryButtonClick = viewState.primaryButtonAction,
            secondaryButtonText = stringResource(id = viewState.secondaryButtonText),
            secondaryButtonClick = viewState.secondaryButtonAction,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MainContent(
    viewState: AccountMismatchErrorViewModel.ViewState,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        UserInfo(
            avatarUrl = viewState.avatarUrl,
            displayName = viewState.displayName,
            username = viewState.username
        )

        Image(
            painter = painterResource(id = R.drawable.img_woo_no_stores),
            contentDescription = null,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .weight(1f, fill = false)
        )
        Text(
            text = viewState.message,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        WCTextButton(onClick = viewState.inlineButtonAction) {
            Text(text = stringResource(id = viewState.inlineButtonText))
        }
    }
}

@Composable
private fun UserInfo(avatarUrl: String, displayName: String, username: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.img_gravatar_placeholder),
            error = painterResource(R.drawable.img_gravatar_placeholder),
            contentDescription = stringResource(R.string.login_avatar_content_description),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .size(dimensionResource(R.dimen.image_major_72))
                .clip(CircleShape)
        )

        Text(
            text = displayName,
            style = MaterialTheme.typography.h5,
            color = colorResource(id = R.color.color_on_surface_high)
        )
        Text(
            text = username,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun ButtonBar(
    primaryButtonText: String?,
    primaryButtonClick: () -> Unit,
    secondaryButtonText: String,
    secondaryButtonClick: () -> Unit,
    modifier: Modifier
) {
    @Composable
    fun Buttons(modifier: Modifier) {
        primaryButtonText?.let {
            WCColoredButton(onClick = primaryButtonClick, modifier = modifier) {
                Text(text = primaryButtonText)
            }
        }

        WCOutlinedButton(onClick = secondaryButtonClick, modifier = modifier) {
            Text(text = secondaryButtonText)
        }
    }

    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
        ) {
            Buttons(modifier = Modifier.weight(1f))
        }
        else -> Column(modifier = modifier) {
            Buttons(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview
@Composable
private fun AccountMismatchPreview() {
    WooThemeWithBackground {
        AccountMismatchErrorScreen(
            viewState = AccountMismatchErrorViewModel.ViewState(
                displayName = "displayname",
                username = "username",
                avatarUrl = "",
                message = stringResource(id = R.string.login_wpcom_account_mismatch, "url"),
                primaryButtonText = R.string.continue_button,
                primaryButtonAction = {},
                secondaryButtonText = R.string.continue_button,
                secondaryButtonAction = {},
                inlineButtonText = R.string.continue_button,
                inlineButtonAction = {}
            )
        )
    }
}
