package com.woocommerce.android.ui.dashboard.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCTag
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.NavigateToOrders
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.Content
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.Error
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.Loading
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.OrderItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

@Composable
fun DashboardOrdersCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardOrdersViewModel = viewModelWithFactory { factory: DashboardOrdersViewModel.Factory ->
        factory.create(parentViewModel)
    }
) {
    viewModel.viewState.observeAsState().value?.let { state ->
        WidgetCard(
            titleResource = state.title,
            menu = viewModel.menu,
            button = viewModel.button,
            modifier = modifier,
            isError = state is Error
        ) {
            when (state) {
                is Content -> {
                    TopOrders(
                        orders = state.orders
                    )
                }
                is Error -> WidgetError(
                    onContactSupportClicked = parentViewModel::onContactSupportClicked,
                    onRetryClicked = viewModel::onRefresh
                )
                is Loading -> Loading()
            }
        }
    }

    HandleEvents(viewModel.event)
}



@Composable
private fun HandleEvents(
    event: LiveData<Event>
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is NavigateToOrders -> {
                    navController.navigateSafely(
                        resId = R.id.orders,
                        navOptions = navOptions {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    )
                }
            }
        }

        event.observe(lifecycleOwner, observer)

        onDispose {
            event.removeObserver(observer)
        }
    }
}

@Composable
fun TopOrders(
    orders: List<OrderItem>
) {
    Column {
        orders.forEach { order ->
            OrderListItem(order)

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun Loading() {
    Column {
        repeat(3) {
            LoadingItem()
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            )
        }
    }
}

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
private fun LoadingItem() {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val (number, date, name, status, total) = createRefs()
        SkeletonView(
            modifier = Modifier
                .height(22.dp)
                .width(50.dp)
                .constrainAs(number) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
        )

        SkeletonView(
            modifier = Modifier
                .padding(start = 16.dp)
                .height(22.dp)
                .width(70.dp)
                .constrainAs(date) {
                    top.linkTo(parent.top)
                    start.linkTo(number.end)
                }
        )

        SkeletonView(
            modifier = Modifier
                .padding(top = 8.dp)
                .height(20.dp)
                .width(120.dp)
                .constrainAs(name) {
                    top.linkTo(number.bottom)
                    start.linkTo(parent.start)
                }
        )

        SkeletonView(
            modifier = Modifier
                .height(22.dp)
                .width(100.dp)
                .constrainAs(status) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                },
        )

        SkeletonView(
            modifier = Modifier
                .padding(top = 8.dp)
                .height(20.dp)
                .width(70.dp)
                .constrainAs(total) {
                    top.linkTo(status.bottom)
                    end.linkTo(parent.end)
                }
        )
    }
}

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
private fun OrderListItem(order: OrderItem) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val (number, date, name, status, total) = createRefs()

        Text(
            text = order.number,
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier.constrainAs(number) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        )

        Text(
            text = order.date,
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier
                .padding(start = 16.dp)
                .constrainAs(date) {
                    top.linkTo(parent.top)
                    start.linkTo(number.end)
                }
        )

        Text(
            text = order.customerName,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(top = 8.dp)
                .constrainAs(name) {
                    top.linkTo(number.bottom)
                    start.linkTo(parent.start)
                }
        )

        WCTag(
            modifier = Modifier
                .constrainAs(status) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                },
            text = order.status,
            textColor = MaterialTheme.colors.onSurface,
            backgroundColor = colorResource(id = order.statusColor),
            fontWeight = FontWeight.Normal
        )

        Text(
            text = order.totalPrice,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(top = 8.dp)
                .constrainAs(total) {
                    top.linkTo(status.bottom)
                    end.linkTo(parent.end)
                }
        )
    }
}

@Composable
@Preview
fun PreviewTopOrders() {
    TopOrders(
        orders = listOf(
            OrderItem(
                number = "123",
                date = "2021-09-01",
                customerName = "John Doe",
                status = "Processing",
                statusColor = R.color.tag_bg_processing,
                totalPrice = "$100.00"
            ),
            OrderItem(
                number = "124",
                date = "2021-09-02",
                customerName = "Jane Doe",
                status = "Completed",
                statusColor = R.color.tag_bg_completed,
                totalPrice = "$200.00"
            )
        ),
    )
}

@Composable
@Preview
fun PreviewLoadingCard() {
    Loading()
}
