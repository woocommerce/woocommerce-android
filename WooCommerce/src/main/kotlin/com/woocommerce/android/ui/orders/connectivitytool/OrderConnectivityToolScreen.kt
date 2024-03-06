package com.woocommerce.android.ui.orders.connectivitytool

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.NotStarted

@Composable
fun OrderConnectivityToolScreen(viewModel: OrderConnectivityToolViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    OrderConnectivityToolScreen(
        isContactSupportButtonEnabled = viewState?.isContactSupportEnabled ?: false,
        internetConnectionTestStatus = viewState?.internetConnectionCheckStatus ?: NotStarted,
        wordpressConnectionTestStatus = viewState?.wordpressConnectionCheckStatus ?: NotStarted,
        storeConnectionTestStatus = viewState?.storeConnectionCheckStatus ?: NotStarted,
        storeOrdersTestStatus = viewState?.storeOrdersCheckStatus ?: NotStarted
    )
}

@Composable
fun OrderConnectivityToolScreen(
    isContactSupportButtonEnabled: Boolean,
    internetConnectionTestStatus: ConnectivityTestStatus,
    wordpressConnectionTestStatus: ConnectivityTestStatus,
    storeConnectionTestStatus: ConnectivityTestStatus,
    storeOrdersTestStatus: ConnectivityTestStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        ConnectivityTestRow(
            testTitle = "Internet Connection",
            errorMessage = "No internet connection",
            testStatus = internetConnectionTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        ConnectivityTestRow(
            testTitle = "WordPress Connection",
            errorMessage = "WordPress connection failed",
            testStatus = wordpressConnectionTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        ConnectivityTestRow(
            testTitle = "Store Connection",
            errorMessage = "Store connection failed",
            testStatus = storeConnectionTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        ConnectivityTestRow(
            testTitle = "Store Orders",
            errorMessage = "Store orders failed",
            testStatus = storeOrdersTestStatus
        )
        Spacer(modifier = modifier.weight(1f))
        Button(
            modifier = modifier.fillMaxWidth(),
            enabled = isContactSupportButtonEnabled,
            onClick = {},
        ) {
            Text("Contact Support")
        }
    }
}

@Composable
fun ConnectivityTestRow(
    testTitle: String,
    errorMessage: String,
    testStatus: ConnectivityTestStatus
) {
    Column {
        Row {
            Text(testTitle)
            Spacer(modifier = Modifier.weight(1f))
            val status = when (testStatus) {
                NotStarted -> "Not Started"
                InProgress -> "In Progress"
                Failure -> "Failed"
                else -> "Success"
            }
            Text(status)
        }

        if (testStatus == Failure) {
            Row {
                Text(errorMessage)
            }
        }
    }
}
