package com.woocommerce.android.wear.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.R
import com.woocommerce.android.wear.compose.theme.WooTheme
import com.woocommerce.android.wear.compose.theme.WooTypography
import com.woocommerce.android.wear.ui.NavRoutes
import com.woocommerce.android.wear.ui.NavRoutes.MY_STORE
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Logged
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Timeout
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Waiting

@Composable
fun LoginScreen(navController: NavController) {
    val viewModel: LoginViewModel = hiltViewModel()
    val viewState by viewModel.viewState.observeAsState()
    LoginScreen(
        loginState = viewState?.loginState ?: Waiting,
        navController = navController,
        onTryAgainClicked = viewModel::reloadData
    )
}

@Composable
fun LoginScreen(
    loginState: LoginState,
    navController: NavController,
    onTryAgainClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooTheme {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (loginState) {
                Logged -> {
                    navController.navigate(MY_STORE.route) {
                        popUpTo(NavRoutes.LOGIN.route) { inclusive = true }
                    }
                }
                Timeout -> {
                    LoginInstructionsScreen(onTryAgainClicked, modifier)
                }
                Waiting -> {
                    TimeText()
                    LoginLoadingScreen(modifier)
                }
            }
        }
    }
}

@Composable
private fun LoginInstructionsScreen(
    onTryAgainClicked: () -> Unit,
    modifier: Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(
                vertical = dimensionResource(id = R.dimen.activity_vertical_margin),
                horizontal = dimensionResource(id = R.dimen.activity_horizontal_margin)
            )
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.login_screen_error_caption),
                textAlign = TextAlign.Center,
                modifier = modifier
                    .padding(top = 8.dp)
                    .wrapContentHeight()
            )
            Button(
                onClick = onTryAgainClicked,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.DarkGray
                ),
                modifier = modifier
                    .weight(1f)
                    .padding(top = 8.dp)
                    .requiredHeight(46.dp)
                    .fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.login_screen_action_button))
            }
        }
    }
}

@Composable
private fun LoginLoadingScreen(
    modifier: Modifier
) {
    TimeText()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_woo_bubble_white_background),
            contentDescription = null,
            contentScale = ContentScale.FillWidth
        )
        Text(
            text = stringResource(id = R.string.login_screen_loading_text),
            textAlign = TextAlign.Center,
            style = WooTypography.title3,
            modifier = modifier
                .padding(top = 8.dp)
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun PreviewError() {
    LoginScreen(
        loginState = Timeout,
        navController = rememberNavController(),
        onTryAgainClicked = {}
    )
}

@Preview(name = "Loading Round", device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(name = "Loading Square", device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun PreviewLoading() {
    LoginScreen(
        loginState = Waiting,
        navController = rememberNavController(),
        onTryAgainClicked = {}
    )
}
