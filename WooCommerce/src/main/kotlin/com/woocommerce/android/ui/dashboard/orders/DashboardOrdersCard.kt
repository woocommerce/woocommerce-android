package com.woocommerce.android.ui.dashboard.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCTag
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardFilterableCardHeader
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.NavigateToOrderDetails
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.NavigateToOrders
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.Content
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.Error
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.Loading
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.OrderItem
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.ui.orders.list.OrderListFragmentDirections
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
                        selectedFilter = state.selectedFilter,
                        filterOptions = state.filterOptions,
                        onFilterSelected = viewModel::onFilterSelected,
                        orders = state.orders,
                        onOrderClicked = { order -> viewModel.onOrderClicked(order.id) }
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
    fun NavController.navigateToOrders() {
        navigateSafely(
            resId = R.id.orders,
            navOptions = navOptions {
                popUpTo(graph.findStartDestination().id) {
                    saveState = true
                }
            }
        )
    }

    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is NavigateToOrders -> {
                    navController.navigateToOrders()
                }
                is NavigateToOrderDetails -> {
                    navController.navigateToOrders()
                    navController.navigateSafely(
                        directions = OrderListFragmentDirections
                            .actionOrderListFragmentToOrderDetailFragment(event.orderId, longArrayOf()),
                        skipThrottling = true
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
private fun Header(
    selectedFilter: OrderStatusOption,
    filterOptions: List<OrderStatusOption>,
    onFilterSelected: (OrderStatusOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        DashboardFilterableCardHeader(
            title = stringResource(id = R.string.dashboard_reviews_card_header_title),
            currentFilter = selectedFilter,
            filterList = filterOptions,
            onFilterSelected = onFilterSelected,
            mapper = { it.label }
        )

        Divider()
    }
}

@Composable
fun TopOrders(
    selectedFilter: OrderStatusOption,
    filterOptions: List<OrderStatusOption>,
    onFilterSelected: (OrderStatusOption) -> Unit,
    orders: List<OrderItem>,
    onOrderClicked: (OrderItem) -> Unit
) {
    Column {
        Header(
            selectedFilter = selectedFilter,
            filterOptions = filterOptions,
            onFilterSelected = onFilterSelected
        )
        if (orders.isEmpty()) {
            EmptyView()
        } else {
            orders.forEach { order ->
                OrderListItem(order, onOrderClicked)

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                )
            }
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
private fun OrderListItem(order: OrderItem, onOrderClicked: (OrderItem) -> Unit) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .focusable(true)
            .clickable(onClick = { onOrderClicked(order) })
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
            textColor = colorResource(id = R.color.color_on_secondary),
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
fun EmptyView(
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_orders_no_orders),
            contentDescription = null,
            modifier = Modifier.sizeIn(maxWidth = 160.dp, maxHeight = 160.dp)
        )

        Text(
            text = stringResource(
                R.string.orders_empty_message_for_filtered_orders
            ),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Preview
fun PreviewEmptyView() {
    EmptyView()
}

@Composable
@Preview
fun PreviewTopOrders() {
    TopOrders(
        orders = listOf(
            OrderItem(
                id = 0L,
                number = "123",
                date = "2021-09-01",
                customerName = "John Doe",
                status = "Processing",
                statusColor = R.color.tag_bg_processing,
                totalPrice = "$100.00"
            ),
            OrderItem(
                id = 0L,
                number = "124",
                date = "2021-09-02",
                customerName = "Jane Doe",
                status = "Completed",
                statusColor = R.color.tag_bg_completed,
                totalPrice = "$200.00"
            )
        ),
        selectedFilter = OrderStatusOption(
            key = "processing",
            label = "Processing",
            statusCount = 1,
            isSelected = true
        ),
        filterOptions = listOf(
            OrderStatusOption(
                key = "processing",
                label = "Processing",
                statusCount = 1,
                isSelected = true
            ),
            OrderStatusOption(
                key = "completed",
                label = "Completed",
                statusCount = 1,
                isSelected = false
            )
        ),
        onFilterSelected = {},
        onOrderClicked = {}
    )
}

@Composable
@Preview
fun PreviewLoadingCard() {
    Loading()
}
