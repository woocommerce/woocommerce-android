package com.woocommerce.android.ui.orders.connectivitytool

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
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
        ConnectivityCheckCard(
            checkTitle = R.string.orderlist_connectivity_tool_internet_check_title,
            iconDrawable = R.drawable.ic_cloud,
            errorMessage = "No internet connection",
            testStatus = internetConnectionTestStatus
        )
        ConnectivityCheckCard(
            checkTitle = R.string.orderlist_connectivity_tool_wordpress_check_title,
            iconDrawable = R.drawable.ic_cloud,
            errorMessage = "WordPress connection failed",
            testStatus = wordpressConnectionTestStatus
        )
        ConnectivityCheckCard(
            checkTitle = R.string.orderlist_connectivity_tool_store_check_title,
            iconDrawable = R.drawable.ic_cloud,
            errorMessage = "Store connection failed",
            testStatus = storeConnectionTestStatus
        )
        ConnectivityCheckCard(
            checkTitle = R.string.orderlist_connectivity_tool_store_orders_check_title,
            iconDrawable = R.drawable.ic_cloud,
            errorMessage = "Store orders failed",
            testStatus = storeOrdersTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        Button(
            enabled = isContactSupportButtonEnabled,
            onClick = { onContactSupportClicked() },
            modifier = modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.orderlist_connectivity_tool_contact_support_action))
        }
    }
}

@Composable
fun ConnectivityCheckCard(
    @StringRes checkTitle: Int,
    @DrawableRes iconDrawable: Int,
    errorMessage: String,
    testStatus: ConnectivityCheckStatus,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.major_75)),
        modifier = modifier.padding(PaddingValues(dimensionResource(id = R.dimen.major_75)))
    ) {
        Column(
            modifier = modifier.padding(PaddingValues(dimensionResource(id = R.dimen.major_75)))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = modifier
                        .size(dimensionResource(id = R.dimen.major_250))
                        .clip(CircleShape)
                        .background(colorResource(id = R.color.more_menu_button_icon_background))
                ) {
                    Image(
                        painter = painterResource(id = iconDrawable),
                        contentDescription = stringResource(id = checkTitle),
                        modifier = modifier
                            .size(dimensionResource(id = R.dimen.major_150))
                            .align(Alignment.Center)
                    )
                }
                Text(
                    text = stringResource(id = checkTitle),
                    modifier = modifier.padding(start = dimensionResource(id = R.dimen.major_100))
                )
                Spacer(modifier = modifier.weight(1f))
                when (testStatus) {
                    NotStarted -> {}
                    InProgress -> {}
                    Success -> ResultIcon(
                        icon = R.drawable.ic_rounded_chcekbox_checked,
                        color = R.color.woo_green_50
                    )
                    Failure -> ResultIcon(
                        icon = R.drawable.ic_rounded_chcekbox_partially_checked,
                        color = R.color.woo_red_50
                    )
                }
            }

            if (testStatus == Failure) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = modifier
                        .padding(top = dimensionResource(id = R.dimen.major_100))
                        .fillMaxWidth()
                ) {
                    Text(errorMessage)
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.orderlist_connectivity_tool_read_more_action))
                    }
                }
            }
        }
    }
}

@Composable
fun ResultIcon(
    @DrawableRes icon: Int,
    @ColorRes color: Int
) {
    Image(
        painter = painterResource(id = icon),
        colorFilter = ColorFilter.tint(colorResource(id = color)),
        contentDescription = null
    )
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
