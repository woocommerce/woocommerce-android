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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
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
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.InternetConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreOrdersConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.WordPressConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success

@Composable
fun OrderConnectivityToolScreen(viewModel: OrderConnectivityToolViewModel) {
    val isCheckFinished by viewModel.isCheckFinished.observeAsState()
    val internetConnectionCheckData by viewModel.internetCheckData.observeAsState()
    val wordpressConnectionCheckData by viewModel.wordpressCheckData.observeAsState()
    val storeConnectionCheckData by viewModel.storeCheckData.observeAsState()
    val storeOrdersCheckData by viewModel.storeOrdersCheckData.observeAsState()

    OrderConnectivityToolScreen(
        isContactSupportButtonEnabled = isCheckFinished ?: false,
        internetConnectionCheckData = internetConnectionCheckData,
        wordpressConnectionCheckData = wordpressConnectionCheckData,
        storeConnectionCheckData = storeConnectionCheckData,
        storeOrdersCheckData = storeOrdersCheckData,
        onContactSupportClicked = viewModel::onContactSupportClicked
    )
}

@Composable
fun OrderConnectivityToolScreen(
    isContactSupportButtonEnabled: Boolean,
    internetConnectionCheckData: InternetConnectivityCheckData?,
    wordpressConnectionCheckData: WordPressConnectivityCheckData?,
    storeConnectionCheckData: StoreConnectivityCheckData?,
    storeOrdersCheckData: StoreOrdersConnectivityCheckData?,
    onContactSupportClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        ConnectivityCheckCard(internetConnectionCheckData)
        ConnectivityCheckCard(wordpressConnectionCheckData)
        ConnectivityCheckCard(storeConnectionCheckData)
        ConnectivityCheckCard(storeOrdersCheckData)
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
    cardData: ConnectivityCheckCardData?
) {
    cardData?.let {
        ConnectivityCheckCard(
            checkTitle = it.title,
            iconDrawable = it.icon,
            suggestion = it.suggestion,
            checkStatus = it.connectivityCheckStatus,
            onReadMoreClicked = it.readMoreAction ?: {},
            onRetryConnectionClicked = it.retryConnectionAction ?: {},
            shouldDisplayReadMoreButton = it.readMoreAction != null
        )
    }
}

@Composable
fun ConnectivityCheckCard(
    modifier: Modifier = Modifier,
    @StringRes checkTitle: Int,
    @DrawableRes iconDrawable: Int,
    @StringRes suggestion: Int,
    checkStatus: ConnectivityCheckStatus,
    onReadMoreClicked: () -> Unit,
    onRetryConnectionClicked: () -> Unit,
    shouldDisplayReadMoreButton: Boolean = false
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
                            .size(dimensionResource(id = R.dimen.major_125))
                            .align(Alignment.Center)
                    )
                }
                Text(
                    text = stringResource(id = checkTitle),
                    modifier = modifier.padding(start = dimensionResource(id = R.dimen.major_100))
                )
                Spacer(modifier = modifier.weight(1f))
                when (checkStatus) {
                    is InProgress -> CircularProgressIndicator(
                        modifier = modifier.size(dimensionResource(id = R.dimen.major_200))
                    )
                    is Success -> ResultIcon(
                        icon = R.drawable.ic_rounded_chcekbox_checked,
                        color = R.color.woo_green_50
                    )
                    is Failure -> ResultIcon(
                        icon = R.drawable.ic_rounded_chcekbox_partially_checked,
                        color = R.color.woo_red_50
                    )
                    is NotStarted -> { /* Do nothing */ }
                }
            }

            if (checkStatus is Failure) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = modifier
                        .padding(top = dimensionResource(id = R.dimen.major_100))
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = checkStatus.error?.message ?: suggestion),
                        color = colorResource(id = R.color.woo_red_50)
                    )

                    OutlinedButton(
                        onClick = { /*TODO*/ },
                        modifier = modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.orderlist_connectivity_tool_retry_action))
                    }

                    if (shouldDisplayReadMoreButton) {
                        OutlinedButton(
                            onClick = onReadMoreClicked,
                            modifier = modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.orderlist_connectivity_tool_read_more_action))
                        }
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
            internetConnectionCheckData = InternetConnectivityCheckData(
                connectivityCheckStatus = NotStarted
            ),
            wordpressConnectionCheckData = WordPressConnectivityCheckData(
                connectivityCheckStatus = Success
            ),
            storeConnectionCheckData = StoreConnectivityCheckData(
                connectivityCheckStatus = Failure(),
                readMoreAction = {}
            ),
            storeOrdersCheckData = StoreOrdersConnectivityCheckData(
                connectivityCheckStatus = InProgress
            ),
            onContactSupportClicked = {}
        )
    }
}
