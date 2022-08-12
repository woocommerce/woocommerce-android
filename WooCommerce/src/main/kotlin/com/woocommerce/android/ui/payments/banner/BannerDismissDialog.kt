package com.woocommerce.android.ui.payments.banner

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel

@Composable
fun PaymentsScreenBannerDismissDialog(viewModel: SelectPaymentMethodViewModel) {
    val showDialog by viewModel.shouldShowUpsellCardReaderDismissDialog.observeAsState(true)
    BannerDismissDialog(
        onRemindLaterClick = viewModel::onRemindLaterClicked,
        onDontShowAgainClick = viewModel::onDontShowAgainClicked,
        onDismissClick = viewModel::onBannerAlertDismiss,
        showDialog,
        AnalyticsTracker.KEY_BANNER_PAYMENTS
    )
}

@Composable
fun OrderListBannerDismissDialog(viewModel: OrderListViewModel) {
    val showDialog by viewModel.shouldShowUpsellCardReaderDismissDialog.observeAsState(true)
    BannerDismissDialog(
        onRemindLaterClick = viewModel::onRemindLaterClicked,
        onDontShowAgainClick = viewModel::onDontShowAgainClicked,
        onDismissClick = viewModel::onBannerAlertDismiss,
        showDialog,
        AnalyticsTracker.KEY_BANNER_ORDER_LIST
    )
}

@Composable
fun BannerDismissDialog(
    onRemindLaterClick: (Long, String) -> Unit,
    onDontShowAgainClick: (String) -> Unit,
    onDismissClick: () -> Unit,
    showDialog: Boolean,
    source: String,
    title: String = stringResource(
        id = R.string.card_reader_upsell_card_reader_banner_payments
    ),
    description: String = stringResource(
        id = R.string.card_reader_upsell_card_reader_banner_dismiss_dialog_description
    )
) {
    if (showDialog) {
        AlertDialog(
            backgroundColor = colorResource(id = R.color.color_surface_elevated),
            onDismissRequest = onDismissClick,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    color = colorResource(id = R.color.color_on_surface)
                )
            },
            text = {
                Text(
                    text = description,
                    style = MaterialTheme.typography.subtitle1,
                    color = colorResource(id = R.color.color_on_surface)
                )
            },
            buttons = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = dimensionResource(id = R.dimen.major_100)),
                    horizontalAlignment = Alignment.End
                ) {
                    TextButton(
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(id = R.dimen.minor_100),
                            ),
                        onClick = {
                            onRemindLaterClick(System.currentTimeMillis(), source)
                        }

                    ) {
                        Text(
                            text = stringResource(id = R.string.card_reader_upsell_card_reader_banner_remind_me_later),
                            color = colorResource(id = R.color.color_secondary),
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(
                                bottom = dimensionResource(id = R.dimen.minor_100)
                            ),
                        onClick = {
                            onDontShowAgainClick(source)
                        }

                    ) {
                        Text(
                            text = stringResource(id = R.string.card_reader_upsell_card_reader_banner_dont_show_again),
                            color = colorResource(id = R.color.color_secondary),
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BannerDismissDialogPreview() {
    BannerDismissDialog(
        onRemindLaterClick = { _, _ -> },
        onDontShowAgainClick = {},
        onDismissClick = {},
        true,
        AnalyticsTracker.KEY_BANNER_PAYMENTS,
        title = stringResource(
            id = R.string.card_reader_upsell_card_reader_banner_payments
        ),
        description = stringResource(
            id = R.string.card_reader_upsell_card_reader_banner_dismiss_dialog_description
        )
    )
}
