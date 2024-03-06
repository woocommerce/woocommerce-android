package com.woocommerce.android.ui.orders.connectivitytool

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStatus.Success

@Composable
fun OrderConnectivityToolScreen(viewModel: OrderConnectivityToolViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    OrderConnectivityToolScreen(
        isContactSupportButtonEnabled = viewState?.isCheckFinished ?: false,
        internetConnectionTestStatus = viewState?.internetConnectionCheckStatus ?: NotStarted,
        wordpressConnectionTestStatus = viewState?.wordpressConnectionCheckStatus ?: NotStarted,
        storeConnectionTestStatus = viewState?.storeConnectionCheckStatus ?: NotStarted,
        onContactSupportClicked = viewModel::onContactSupportClicked,
        storeOrdersTestStatus = viewState?.storeOrdersCheckStatus ?: NotStarted
    )
}

@Composable
fun OrderConnectivityToolScreen(
    isContactSupportButtonEnabled: Boolean,
    internetConnectionTestStatus: ConnectivityCheckStatus,
    wordpressConnectionTestStatus: ConnectivityCheckStatus,
    storeConnectionTestStatus: ConnectivityCheckStatus,
    storeOrdersTestStatus: ConnectivityCheckStatus,
    onContactSupportClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        ConnectivityTestRow(
            checkTitle = R.string.orderlist_connectivity_tool_internet_check_title,
            errorMessage = "No internet connection",
            testStatus = internetConnectionTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        ConnectivityTestRow(
            checkTitle = R.string.orderlist_connectivity_tool_wordpress_check_title,
            errorMessage = "WordPress connection failed",
            testStatus = wordpressConnectionTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        ConnectivityTestRow(
            checkTitle = R.string.orderlist_connectivity_tool_store_check_title,
            errorMessage = "Store connection failed",
            testStatus = storeConnectionTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        ConnectivityTestRow(
            checkTitle = R.string.orderlist_connectivity_tool_store_orders_check_title,
            errorMessage = "Store orders failed",
            testStatus = storeOrdersTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        Button(
            modifier = modifier.fillMaxWidth(),
            enabled = isContactSupportButtonEnabled,
            onClick = { onContactSupportClicked() },
        ) {
            Text("Contact Support")
        }
    }
}

@Composable
fun ConnectivityTestRow(
    @StringRes checkTitle: Int,
    @DrawableRes iconDrawable: Int,
    errorMessage: String,
    testStatus: ConnectivityCheckStatus
) {
    Column {
        Row {
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.major_250))
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.more_menu_button_icon_background))
            ) {
                Image(
                    painter = painterResource(id = iconDrawable),
                    contentDescription = stringResource(id = checkTitle),
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.major_125))
                        .align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(stringResource(id = checkTitle))
            Spacer(modifier = Modifier.weight(1f))
            when (testStatus) {
                NotStarted -> null
                InProgress -> null
                Failure -> R.drawable.ic_rounded_chcekbox_checked
                Success -> R.drawable.ic_rounded_chcekbox_partially_checked
            }?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = null
                )
            }
        }

        if (testStatus == Failure) {
            Row {
                Text(errorMessage)
            }
        }
    }
}

@Preview
@Composable
fun OrderConnectivityToolScreenPreview() {
    WooThemeWithBackground {
        OrderConnectivityToolScreen(
            isContactSupportButtonEnabled = true,
            internetConnectionTestStatus = NotStarted,
            wordpressConnectionTestStatus = InProgress,
            storeConnectionTestStatus = Failure,
            storeOrdersTestStatus = Success,
            onContactSupportClicked = {}
        )
    }
}
