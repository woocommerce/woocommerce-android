package com.woocommerce.android.ui.customer

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.CustomerWithAnalytics
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun CustomerDetailsScreen(viewModel: CustomerDetailsViewModel) {
    val state by viewModel.viewState.observeAsState()
    state?.let { currentState ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentState.customerWithAnalytics.getFullName()) },
                    navigationIcon = {
                        IconButton(viewModel::onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    },
                    backgroundColor = colorResource(id = R.color.color_toolbar),
                    elevation = 0.dp,
                )
            }
        ) { padding ->
            CustomerDetailsScreen(state = currentState, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
fun CustomerDetailsScreen(
    state: CustomerViewState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        CustomerSection(
            customer = state.customerWithAnalytics,
            isLoadingAnalytics = state.isLoadingAnalytics
        )
        OrdersSection(
            customer = state.customerWithAnalytics,
            isLoadingAnalytics = state.isLoadingAnalytics
        )
        RegistrationSection(customer = state.customerWithAnalytics)

        val hasBillingAddress = state.customerWithAnalytics.billingAddress.address1.isNotEmpty()

        if (hasBillingAddress && state.isLoadingAnalytics.not()) {
            BillingAddressSection(customer = state.customerWithAnalytics)
            ShippingAddressSection(customer = state.customerWithAnalytics)
        } else {
            LocationSection(
                customer = state.customerWithAnalytics,
                isLoadingAnalytics = state.isLoadingAnalytics
            )
        }
    }
}

@Composable
fun CustomerSection(
    customer: CustomerWithAnalytics,
    isLoadingAnalytics: Boolean
) {
    SectionTitle(stringResource(id = R.string.customers_details_customer_section))
    SectionValue(title = customer.getFullName(), value = null)
    Divider()
    SectionValue(title = customer.email) {
        Icon(
            imageVector = Icons.Filled.Email,
            contentDescription = "",
            tint = MaterialTheme.colors.primary
        )
    }
    Divider()
    SectionValue(
        title = customer.phone.ifEmpty { stringResource(id = R.string.customers_details_no_phone_hint) },
        value = null,
        modifier = if (customer.phone.isEmpty()) Modifier.alpha(.65f) else Modifier
    )
    Divider()
    if (isLoadingAnalytics) {
        SectionValue(
            title = stringResource(id = R.string.customers_details_last_active_value_title),
        ) {
            SkeletonView(
                width = 120.dp,
                height = 20.dp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    } else {
        SectionValue(
            title = stringResource(id = R.string.customers_details_last_active_value_title),
            value = customer.lastActive
        )
    }
}

@Composable
fun OrdersSection(
    customer: CustomerWithAnalytics,
    isLoadingAnalytics: Boolean
) {
    SectionTitle(stringResource(id = R.string.customers_details_orders_section))
    if (isLoadingAnalytics) {
        SectionValue(
            title = stringResource(id = R.string.customers_details_orders_value_title),
        ) {
            SkeletonView(
                width = 120.dp,
                height = 20.dp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Divider()
        SectionValue(
            title = stringResource(id = R.string.customers_details_total_spend_value_title),
        ) {
            SkeletonView(
                width = 170.dp,
                height = 20.dp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Divider()
        SectionValue(
            title = stringResource(id = R.string.customers_details_average_order_value_title),
        ) {
            SkeletonView(
                width = 150.dp,
                height = 20.dp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    } else {
        SectionValue(
            title = stringResource(id = R.string.customers_details_orders_value_title),
            value = customer.ordersCount.toString()
        )
        SectionValue(
            title = stringResource(id = R.string.customers_details_total_spend_value_title),
            value = customer.totalSpend.toString()
        )
        SectionValue(
            title = stringResource(id = R.string.customers_details_average_order_value_title),
            value = customer.averageOrderValue.toString()
        )
    }
}

@Composable
fun LocationSection(
    customer: CustomerWithAnalytics,
    isLoadingAnalytics: Boolean
) {
    SectionTitle(stringResource(id = R.string.customers_details_shipping_location_section))
    if (isLoadingAnalytics) {
        SectionValue(
            title = stringResource(id = R.string.customers_details_country_value_title),
        ) {
            SkeletonView(
                width = 110.dp,
                height = 20.dp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Divider()
        SectionValue(
            title = stringResource(id = R.string.customers_details_region_value_title),
        ) {
            SkeletonView(
                width = 180.dp,
                height = 20.dp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Divider()
        SectionValue(
            title = stringResource(id = R.string.customers_details_city_value_title),
        ) {
            SkeletonView(
                width = 150.dp,
                height = 20.dp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Divider()
        SectionValue(
            title = stringResource(id = R.string.customers_details_postal_code_value_title),
        ) {
            SkeletonView(
                width = 160.dp,
                height = 20.dp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    } else {
        SectionValue(
            title = stringResource(id = R.string.customers_details_country_value_title),
            value = customer.billingAddress.country.name.ifEmpty {
                stringResource(id = R.string.customers_details_none_hint)
            }
        )
        Divider()
        SectionValue(
            title = stringResource(id = R.string.customers_details_region_value_title),
            value = when (val state = customer.billingAddress.state) {
                is AmbiguousLocation.Defined -> state.value.name
                is AmbiguousLocation.Raw -> state.value
            }.ifEmpty { stringResource(id = R.string.customers_details_none_hint) }
        )
        Divider()
        SectionValue(
            title = stringResource(id = R.string.customers_details_city_value_title),
            value = customer.billingAddress.city.ifEmpty {
                stringResource(id = R.string.customers_details_none_hint)
            }
        )
        Divider()
        SectionValue(
            title = stringResource(id = R.string.customers_details_postal_code_value_title),
            value = customer.billingAddress.postcode.ifEmpty {
                stringResource(id = R.string.customers_details_none_hint)
            }
        )
    }
}

@Composable
fun RegistrationSection(
    customer: CustomerWithAnalytics
) {
    SectionTitle(stringResource(id = R.string.customers_details_registration_section))
    SectionValue(
        title = stringResource(id = R.string.customers_details_username_value_title),
        value = customer.username.ifEmpty {
            stringResource(id = R.string.customers_details_none_hint)
        }
    )
    Divider()
    SectionValue(
        title = stringResource(id = R.string.customers_details_date_registered_value_title),
        value = customer.registeredDate.ifEmpty {
            stringResource(id = R.string.customers_details_none_hint)
        }
    )
}

@Composable
fun BillingAddressSection(customer: CustomerWithAnalytics) {
    SectionTitle(stringResource(id = R.string.customers_details_billing_address_section))
    SectionValue(
        title = customer.getBillingAddress(),
        value = null
    )
}

@Composable
fun ShippingAddressSection(customer: CustomerWithAnalytics) {
    SectionTitle(stringResource(id = R.string.customers_details_shipping_address_section))
    SectionValue(
        title = customer.getShippingAddress(),
        value = null
    )
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        fontSize = 18.sp,
        color = if (MaterialTheme.colors.isLight) Color.DarkGray else Color.LightGray,
        modifier = modifier
            .fillMaxWidth()
            .background(color = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SectionTitlePreview() {
    WooTheme {
        SectionTitle("This is a section title")
    }
}

@Composable
fun SectionValue(
    title: String,
    modifier: Modifier = Modifier,
    action: (() -> Unit)? = null,
    value: @Composable RowScope. () -> Unit
) {
    val rowModifier = action?.let {
        modifier.clickable { it() }
    } ?: modifier
    Row(modifier = rowModifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f, true)
        )
        value()
    }
}

@Composable
fun SectionValue(
    title: String,
    value: String?,
    modifier: Modifier = Modifier,
    action: (() -> Unit)? = null,
) {
    SectionValue(
        title = title,
        modifier = modifier,
        action = action
    ) {
        value?.let { Text(text = it, fontSize = 18.sp) }
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SectionValuePreview() {
    WooThemeWithBackground {
        SectionValue(
            title = "Name",
            value = "John"
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SectionValueComposablePreview() {
    WooThemeWithBackground {
        SectionValue(
            title = "Name"
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = "",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SectionValueNoValuePreview() {
    WooThemeWithBackground {
        SectionValue(title = "Name", value = null)
    }
}


