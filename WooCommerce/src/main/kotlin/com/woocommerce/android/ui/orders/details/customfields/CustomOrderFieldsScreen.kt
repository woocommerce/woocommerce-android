package com.woocommerce.android.ui.orders.details.customfields

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.W700
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.HtmlCompat
import com.woocommerce.android.R
import com.woocommerce.android.compose.utils.toAnnotatedString
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.HtmlHelper
import com.woocommerce.android.util.StringUtils
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
                    if (HtmlHelper.isHtml(metadata.value)) {
                        htmlTextValueItem(metadata.value)
                    } else if (URLUtil.isValidUrl(metadata.value)) {
                        urlTextValueItem(metadata.value)
                    } else if (StringUtils.isValidEmail(metadata.value)) {
                        emailTextValueItem(metadata.value)
                    } else {
                        textValueItem(metadata.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun textValueItem(value: String) {
    Text(
        text = HtmlCompat.fromHtml(
            value,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toAnnotatedString(),
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSurface
    )
}

@Composable
private fun htmlTextValueItem(value: String) {
    val context = LocalContext.current
    val annotatedText = HtmlCompat.fromHtml(
        value,
        HtmlCompat.FROM_HTML_MODE_LEGACY
    ).toAnnotatedString()

    ClickableText(
        text = annotatedText,
        style = MaterialTheme.typography.body2,
        onClick = { offset ->
            annotatedText.getStringAnnotations(
                tag = "policy",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                val url = it.item
                ChromeCustomTabUtils.launchUrl(context, url)
            }
        }
    )
}

@Composable
private fun urlTextValueItem(value: String) {
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
    val context = LocalContext.current

    ClickableText(
        text = text,
        style = MaterialTheme.typography.body2,
        onClick = {
            ChromeCustomTabUtils.launchUrl(context, value)
        }
    )
}

@Composable
private fun emailTextValueItem(value: String) {
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
    val context = LocalContext.current

    ClickableText(
        text = text,
        style = MaterialTheme.typography.body2,
        onClick = {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:$value")
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // TODO nbradbury
            }
        }
    )
}

@Preview
@Composable
private fun CustomFieldsPreview() {
    CustomFieldsScreen(
        listOf(
            OrderMetaDataEntity(
                id = 0,
                localSiteId = LocalOrRemoteId.LocalId(0),
                orderId = 0,
                key = "text key",
                value = "text value"
            ),
            OrderMetaDataEntity(
                id = 1,
                localSiteId = LocalOrRemoteId.LocalId(0),
                orderId = 0,
                key = "html key",
                value = "<strong>This</strong> is an <em>html</em> value"
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
            )
        )
    )
}
