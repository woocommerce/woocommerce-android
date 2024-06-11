package com.woocommerce.android.ui.jetpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun JetpackActivationEligibilityErrorScreen(viewModel: JetpackActivationEligibilityErrorViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationEligibilityErrorScreen(
            viewState = it,
            onBackButtonClick = viewModel::onBackButtonClick,
            onLearnMoreClick = viewModel::onLearnMoreButtonClicked,
            onRetryClick = viewModel::onRetryButtonClicked,
            onHelpButtonClick = viewModel::onHelpButtonClicked
        )
    }
}

@Composable
fun JetpackActivationEligibilityErrorScreen(
    viewState: JetpackActivationEligibilityErrorViewModel.ViewState,
    onBackButtonClick: () -> Unit = {},
    onLearnMoreClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onHelpButtonClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            ToolbarWithHelpButton(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onHelpButtonClick = onHelpButtonClick,
                onNavigationButtonClick = onBackButtonClick
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.surface)
                .padding(paddingValues)
                .padding(
                    vertical = dimensionResource(id = R.dimen.major_100),
                    horizontal = dimensionResource(id = R.dimen.major_100)
                )
        ) {
            MainContent(
                username = viewState.username,
                role = viewState.role,
                onLearnMoreClick = onLearnMoreClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )

            Spacer(Modifier.height(dimensionResource(id = R.dimen.major_100)))
            WCColoredButton(
                onClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(text = stringResource(id = R.string.retry))
            }

            if (viewState.isRetrying) {
                ProgressDialog(
                    title = stringResource(id = R.string.jetpack_benefits_fetching_status),
                    subtitle = stringResource(id = R.string.please_wait)
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    username: String,
    role: String,
    onLearnMoreClick: () -> Unit = {},
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = username,
            style = MaterialTheme.typography.h5,
            color = colorResource(id = R.color.color_on_surface_high)
        )
        Text(
            text = role,
            style = MaterialTheme.typography.caption
        )

        Image(
            painter = painterResource(id = R.drawable.img_user_access_error),
            contentDescription = null,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .weight(1f, fill = false)
        )
        Text(
            text = stringResource(id = R.string.jetpack_install_role_eligibility_error_message),
            textAlign = TextAlign.Center
        )

        WCTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLearnMoreClick
        ) {
            Text(text = stringResource(id = R.string.jetpack_install_role_eligibility_learn_more))
        }
    }
}

@Preview
@Composable
private fun JetpackActivationEligibilityErrorScreenPreview() {
    JetpackActivationEligibilityErrorScreen(
        JetpackActivationEligibilityErrorViewModel.ViewState(
            username = "username",
            role = "Shop Manager",
            isRetrying = false
        )
    )
}
