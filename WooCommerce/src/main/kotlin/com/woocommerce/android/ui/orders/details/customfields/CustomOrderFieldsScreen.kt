package com.woocommerce.android.ui.orders.details.customfields

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.W700
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.orders.details.customfields.CustomOrderFieldsHelper.CustomOrderFieldClickListener
import com.woocommerce.android.ui.orders.details.customfields.CustomOrderFieldsHelper.CustomOrderFieldType
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity

private var clickListener: CustomOrderFieldClickListener? = null

@Composable
fun CustomOrderFieldsScreen(viewModel: OrderDetailViewModel, listener: CustomOrderFieldClickListener? = null) {
    clickListener = listener
    CustomFieldsScreen(
        viewModel.getOrderMetadata(),
        viewModel::onBackPressed
    )
}

@Composable
fun CustomFieldsScreen(
    metadataList: List<OrderMetaDataEntity>,
    onBackPressed: () -> Unit
) {
    Box(
        modifier = Modifier.height(600.dp)
    ) {
        Column {
            Toolbar(
                title = stringResource(id = R.string.orderdetail_custom_fields),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationButtonClick = {
                    onBackPressed()
                }
            )
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
                    if (CustomOrderFieldType.fromMetadataValue(metadata.value) == CustomOrderFieldType.TEXT) {
                        textValueItem(metadata.value)
                    } else {
                        clickableTextValueItem(metadata.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun textValueItem(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSurface
    )
}

@Composable
private fun clickableTextValueItem(value: String) {
    val text = with(AnnotatedString.Builder()) {
        append(value)
        addStringAnnotation(
            tag = value,
            annotation = value,
            start = 0,
            end = value.length - 1
        )
        toAnnotatedString()
    }

    Text(
        text = text,
        style = MaterialTheme.typography.body2.copy(
            color = colorResource(R.color.color_text_link)
        ),
        modifier = Modifier.clickable(
            indication = rememberRipple(
                bounded = true,
                color = colorResource(id = R.color.color_ripple_overlay)
            ),
            interactionSource = remember { MutableInteractionSource() },
            onClick = {
                clickListener?.onCustomOrderFieldClicked(value)
            }
        )
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CustomFieldsPreview() {
    WooThemeWithBackground {
        CustomFieldsScreen(
            listOf(
                OrderMetaDataEntity(
                    id = 1,
                    localSiteId = LocalOrRemoteId.LocalId(0),
                    orderId = 0,
                    key = "text key",
                    value = "text value"
                ),
                OrderMetaDataEntity(
                    id = 2,
                    localSiteId = LocalOrRemoteId.LocalId(0),
                    orderId = 0,
                    key = "url key",
                    value = "https://automattic.com/"
                ),
                OrderMetaDataEntity(
                    id = 3,
                    localSiteId = LocalOrRemoteId.LocalId(0),
                    orderId = 0,
                    key = "email key",
                    value = "example@example.com"
                ),
                OrderMetaDataEntity(
                    id = 4,
                    localSiteId = LocalOrRemoteId.LocalId(0),
                    orderId = 0,
                    key = "phone key",
                    value = "tel://1234567890"
                )
            ),
            onBackPressed = {}
        )
    }
}
