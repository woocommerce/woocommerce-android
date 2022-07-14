package com.woocommerce.android.ui.orders.details.customfields

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight.Companion.W700
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity

@Composable
fun CustomOrderFieldsScreen(viewModel: OrderDetailViewModel) {
    CustomFieldsScreen(
        viewModel.getOrderMetadata()
    )
}

@Composable
fun CustomFieldsScreen(metadataList: List<OrderMetaDataEntity>) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.background(color = MaterialTheme.colors.surface)
    ) {
        itemsIndexed(
            items = metadataList,
            key = { _, metadata -> metadata.id }
        ) { _, metadata ->
            CustomFieldListItem(metadata)
            Divider(
                modifier = Modifier.offset(x = dimensionResource(id = R.dimen.major_100)),
                color = colorResource(id = R.color.divider_color),
                thickness = dimensionResource(id = R.dimen.minor_10)
            )
        }
    }
}

@Composable
private fun CustomFieldListItem(metadata: OrderMetaDataEntity) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            ),
    ) {
        Row {
            Column(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    .align(Alignment.CenterVertically)
            ) {
                SelectionContainer {
                    Text(
                        text = metadata.key,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = W700,
                        color = MaterialTheme.colors.onSurface
                    )
                }
                SelectionContainer {
                    Text(
                        text = metadata.value,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CustomFieldsPreview() {
    val metadataList = listOf(
        OrderMetaDataEntity(
            id = 0,
            localSiteId = LocalOrRemoteId.LocalId(0),
            orderId = 0,
            key = "key_zero",
            value = "value_zero",
            displayKey = null,
            displayValue = null
        ),
        OrderMetaDataEntity(
            id = 1,
            localSiteId = LocalOrRemoteId.LocalId(0),
            orderId = 0,
            key = "key_one",
            value = "value_one",
            displayKey = null,
            displayValue = null
        )
    )

    CustomFieldsScreen(metadataList)
}
