package com.woocommerce.android.ui.orders.connectivitytool

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCTextButton
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
    val viewState by viewModel.viewState.observeAsState()

    OrderConnectivityToolScreen(
        shouldEnableContactSupportButton = isCheckFinished ?: false,
        shouldDisplaySummarySection = viewState?.shouldDisplaySummary ?: false,
        internetConnectionCheckData = viewState?.internetCheckData,
        wordpressConnectionCheckData = viewState?.wordPressCheckData,
        storeConnectionCheckData = viewState?.storeCheckData,
        storeOrdersCheckData = viewState?.ordersCheckData,
        onContactSupportClicked = viewModel::onContactSupportClicked,
        onReturnClick = viewModel::onReturnClicked
    )
}

@Composable
fun OrderConnectivityToolScreen(
    shouldEnableContactSupportButton: Boolean,
    shouldDisplaySummarySection: Boolean,
    internetConnectionCheckData: InternetConnectivityCheckData?,
    wordpressConnectionCheckData: WordPressConnectivityCheckData?,
    storeConnectionCheckData: StoreConnectivityCheckData?,
    storeOrdersCheckData: StoreOrdersConnectivityCheckData?,
    onContactSupportClicked: () -> Unit,
    onReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(colorResource(id = R.color.color_surface))
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
            modifier = modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.orderlist_connectivity_tool_title),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(id = R.string.orderlist_connectivity_tool_subtitle))
        }

        ConnectivityCheckCard(internetConnectionCheckData)
        ConnectivityCheckCard(wordpressConnectionCheckData)
        ConnectivityCheckCard(storeConnectionCheckData)
        ConnectivityCheckCard(storeOrdersCheckData)

        ConnectivitySummary(
            shouldDisplaySummarySection = shouldDisplaySummarySection,
            onReturnClick = onReturnClick,
            modifier = modifier
        )

        Spacer(modifier = modifier.weight(1f))
        WCOutlinedButton(
            enabled = shouldEnableContactSupportButton,
            onClick = { onContactSupportClicked() },
            modifier = modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.orderlist_connectivity_tool_contact_support_action))
        }
    }
}

@Composable
fun ConnectivityCheckCard(
    cardData: ConnectivityCheckCardData?
) {
    cardData
        ?.takeUnless { it.connectivityCheckStatus is NotStarted }
        ?.let {
            ConnectivityCheckCard(
                checkTitle = it.title,
                iconDrawable = it.icon,
                suggestion = it.suggestion,
                checkStatus = it.connectivityCheckStatus,
                onReadMoreClicked = it.readMoreAction ?: {},
                onRetryConnectionClicked = it.retryConnectionAction ?: {},
                shouldDisplayReadMoreButton = it.readMoreAction != null
            )
            Divider(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
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
    Column(
        modifier = modifier.padding(PaddingValues(dimensionResource(id = R.dimen.major_100)))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                colorFilter = ColorFilter.tint(colorResource(id = R.color.woo_black)),
                painter = painterResource(id = iconDrawable),
                contentDescription = stringResource(id = checkTitle),
                modifier = modifier
                    .size(dimensionResource(id = R.dimen.major_100))
            )

            Text(
                text = stringResource(id = checkTitle),
                fontWeight = FontWeight.Bold,
                modifier = modifier
                    .padding(start = dimensionResource(id = R.dimen.minor_100))
                    .fillMaxHeight()
            )

            Spacer(modifier = modifier.weight(1f))
            when (checkStatus) {
                is InProgress -> CircularProgressIndicator(
                    modifier = modifier.size(dimensionResource(id = R.dimen.major_150))
                )
                is Success -> ResultIcon(
                    icon = Icons.Default.CheckCircle,
                    color = R.color.woo_green_50
                )
                is Failure -> ResultIcon(
                    icon = Icons.Default.Error,
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
                    style = MaterialTheme.typography.body2,
                    modifier = modifier.align(Alignment.Start)
                )

                WCTextButton(
                    allCaps = false,
                    icon = Icons.Default.Repeat,
                    onClick = onRetryConnectionClicked,
                    modifier = modifier.align(Alignment.Start),
                    text = stringResource(id = R.string.orderlist_connectivity_tool_retry_action),
                    contentPadding = PaddingValues(
                        vertical = dimensionResource(id = R.dimen.minor_100),
                        horizontal = dimensionResource(id = R.dimen.minor_00)
                    )
                )

                if (shouldDisplayReadMoreButton) {
                    WCTextButton(
                        allCaps = false,
                        onClick = onReadMoreClicked,
                        icon = Icons.Default.ArrowOutward,
                        modifier = modifier.align(Alignment.Start),
                        text = stringResource(id = R.string.orderlist_connectivity_tool_read_more_action),
                        contentPadding = PaddingValues(
                            vertical = dimensionResource(id = R.dimen.minor_100),
                            horizontal = dimensionResource(id = R.dimen.minor_00)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectivitySummary(
    shouldDisplaySummarySection: Boolean,
    onReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (shouldDisplaySummarySection) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
            modifier = modifier
                .padding(vertical = dimensionResource(id = R.dimen.major_100))
                .padding(start = dimensionResource(id = R.dimen.major_100))
        ) {
            Column(
                modifier = modifier
                    .padding(end = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = stringResource(id = R.string.orderlist_connectivity_tool_summary_title),
                    fontWeight = FontWeight.Bold,
                    modifier = modifier
                        .fillMaxHeight()
                )

                Text(
                    text = stringResource(id = R.string.orderlist_connectivity_tool_summary_suggestion),
                    style = MaterialTheme.typography.body2
                )

                WCTextButton(
                    allCaps = false,
                    onClick = onReturnClick,
                    text = stringResource(id = R.string.orderlist_connectivity_tool_return_action),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentPadding = PaddingValues(
                        vertical = dimensionResource(id = R.dimen.minor_100),
                        horizontal = dimensionResource(id = R.dimen.minor_00)
                    )
                )
            }

            Divider()
        }
    }
}

@Composable
fun ResultIcon(
    icon: ImageVector,
    @ColorRes color: Int
) {
    Image(
        imageVector = icon,
        colorFilter = ColorFilter.tint(colorResource(id = color)),
        contentDescription = null
    )
}

@Preview
@Composable
fun OrderConnectivityToolScreenPreview() {
    WooThemeWithBackground {
        OrderConnectivityToolScreen(
            shouldEnableContactSupportButton = true,
            shouldDisplaySummarySection = true,
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
            onContactSupportClicked = {},
            onReturnClick = {}
        )
    }
}
