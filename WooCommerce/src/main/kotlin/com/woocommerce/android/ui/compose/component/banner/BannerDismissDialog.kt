package com.woocommerce.android.ui.compose.component.banner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel
import com.woocommerce.android.ui.prefs.MainSettingsContract

@Composable
fun PaymentsScreenBannerDismissDialog(viewModel: SelectPaymentMethodViewModel) {
    val showDialog by viewModel.shouldShowUpsellCardReaderDismissDialog.observeAsState(true)
    BannerDismissDialog(
        onRemindLaterClick = viewModel::onRemindLaterClicked,
        onDontShowAgainClick = viewModel::onDontShowAgainClicked,
        showDialog,
    )
}

@Composable
fun OrderListBannerDismissDialog(viewModel: OrderListViewModel) {
    val showDialog by viewModel.shouldShowUpsellCardReaderDismissDialog.observeAsState(true)
    BannerDismissDialog(
        onRemindLaterClick = viewModel::onRemindLaterClicked,
        onDontShowAgainClick = viewModel::onDontShowAgainClicked,
        showDialog,
    )
}

@Composable
fun SettingsBannerDismissDialog(presenter: MainSettingsContract.Presenter) {
    val showDialog by presenter.shouldShowUpsellCardReaderDismissDialog.observeAsState(true)
    BannerDismissDialog(
        onRemindLaterClick = presenter::onRemindLaterClicked,
        onDontShowAgainClick = presenter::onDontShowAgainClicked,
        showDialog,
    )
}

@Composable
fun BannerDismissDialog(
    onRemindLaterClick: (Long) -> Unit,
    onDontShowAgainClick: () -> Unit,
    showDialog: Boolean,
    title: String = stringResource(
        id = R.string.card_reader_upsell_card_reader_banner_payments
    ),
    description: String = stringResource(
        id = R.string.card_reader_upsell_card_reader_banner_dismiss_dialog_description
    )
) {
    if (showDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                )
            },
            text = {
                Text(
                    text = description,
                    style = MaterialTheme.typography.subtitle1,
                )
            },
            buttons = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = dimensionResource(id = R.dimen.major_100)),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(id = R.string.card_reader_upsell_card_reader_banner_remind_me_later),
                        color = colorResource(id = R.color.woo_purple_60),
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(id = R.dimen.minor_100),
                                bottom = dimensionResource(id = R.dimen.major_100)
                            )
                            .clickable(
                                onClick = {
                                    onRemindLaterClick(System.currentTimeMillis())
                                }
                            )
                    )

                    Text(
                        text = stringResource(id = R.string.card_reader_upsell_card_reader_banner_dont_show_again),
                        color = colorResource(id = R.color.woo_purple_60),
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(id = R.dimen.minor_100),
                                bottom = dimensionResource(id = R.dimen.major_100)
                            )
                            .clickable(
                                onClick = {
                                    onDontShowAgainClick()
                                }
                            )
                    )
                }
            }
        )
    }
}

@Preview
@Composable
fun BannerDismissDialogPreview() {
    BannerDismissDialog(
        onRemindLaterClick = {},
        onDontShowAgainClick = {},
        true,
        title = stringResource(
            id = R.string.card_reader_upsell_card_reader_banner_payments
        ),
        description = stringResource(
            id = R.string.card_reader_upsell_card_reader_banner_dismiss_dialog_description
        )
    )
}
