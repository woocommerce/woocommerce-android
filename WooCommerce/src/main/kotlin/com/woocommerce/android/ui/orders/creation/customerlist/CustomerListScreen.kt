package com.woocommerce.android.ui.orders.creation.customerlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewModel.CustomerListItem
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewModel.CustomerListViewState

@Composable
fun CustomerListScreen(
    viewModel: CustomerListViewModel
) {
    val state by viewModel.viewState.observeAsState(CustomerListViewState())
    CustomerListScreen(
        state,
        viewModel::onCustomerClick
    )
}

@Composable
fun CustomerListScreen(
    state: CustomerListViewState,
    onCustomerClick: ((Long) -> Unit?)? = null
) {
    if (state.isSkeletonShown) {
        CustomerListSkeleton()
    } else if (state.searchQuery.isEmpty()) {
        // show nothing
    } else if (state.customers.isEmpty()) {
        EmptyCustomerList()
    } else {
        CustomerList(state.customers, onCustomerClick)
    }
}

@Composable
private fun CustomerList(
    customers: List<CustomerListItem>,
    onCustomerClick: ((Long) -> Unit?)? = null
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.background(color = MaterialTheme.colors.surface)
    ) {
        itemsIndexed(
            items = customers,
            key = { _, customer -> customer.remoteId }
        ) { _, customer ->
            CustomerListViewItem(customer, onCustomerClick)
            Divider(
                modifier = Modifier.offset(x = dimensionResource(id = R.dimen.major_100)),
                color = colorResource(id = R.color.divider_color),
                thickness = dimensionResource(id = R.dimen.minor_10)
            )
        }
    }
}

@Composable
private fun CustomerListViewItem(
    customer: CustomerListItem,
    onCustomerClick: ((Long) -> Unit?)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                role = Role.Button,
                onClick = {
                    onCustomerClick?.let {
                        it(customer.remoteId)
                    }
                }
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            ),
    ) {
        Row {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(customer.avatarUrl)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.ic_photos_grey_c_24dp),
                error = painterResource(R.drawable.ic_photos_grey_c_24dp),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.major_300))
                    .clip(RoundedCornerShape(3.dp))
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = "${customer.firstName} ${customer.lastName}",
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = customer.email,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

@Composable
private fun EmptyCustomerList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.order_creation_customer_search_empty),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        Image(
            painter = painterResource(id = R.drawable.img_empty_search),
            contentDescription = null,
        )
    }
}

@Composable
private fun CustomerListSkeleton() {
    val numberOfSkeletonRows = 10
    LazyColumn(
        Modifier
            .background(color = MaterialTheme.colors.surface)
    ) {
        repeat(numberOfSkeletonRows) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(
                            horizontal = dimensionResource(id = R.dimen.major_100),
                            vertical = dimensionResource(id = R.dimen.minor_100)
                        )
                ) {
                    SkeletonView(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.major_300))
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Column(
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                            .align(Alignment.CenterVertically)
                    ) {
                        SkeletonView(
                            modifier = Modifier
                                .width(dimensionResource(id = R.dimen.skeleton_text_large_width))
                                .height(dimensionResource(id = R.dimen.skeleton_text_height_100))
                        )
                        SkeletonView(
                            modifier = Modifier
                                .padding(
                                    top = dimensionResource(id = R.dimen.minor_50)
                                )
                                .width(dimensionResource(id = R.dimen.skeleton_text_extra_large_width))
                                .height(dimensionResource(id = R.dimen.skeleton_text_height_75))
                        )
                    }
                }

                Divider(
                    modifier = Modifier
                        .offset(x = dimensionResource(id = R.dimen.major_100)),
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CustomerListPreview() {
    val customers = listOf(
        CustomerListItem(
            remoteId = 1,
            firstName = "George",
            lastName = "Carlin",
            email = "me@example.com",
            avatarUrl = ""
        )
    )

    CustomerList(customers)
}

@Preview
@Composable
private fun EmptyCustomerListPreview() {
    EmptyCustomerList()
}

@Preview
@Composable
private fun CustomerListSkeletonPreview() {
    CustomerListSkeleton()
}
