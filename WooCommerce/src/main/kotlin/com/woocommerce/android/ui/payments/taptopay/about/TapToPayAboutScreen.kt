package com.woocommerce.android.ui.payments.taptopay.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.LearnMoreAboutSection
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun TapToPayAboutScreen(viewModel: TapToPayAboutViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        TapToPayAboutScreen(
            onBackClick = viewModel::onBackClicked,
            state
        )
    }
}

@Composable
fun TapToPayAboutScreen(
    onBackClick: () -> Unit,
    state: TapToPayAboutViewModel.UiState,
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(R.string.card_reader_tap_to_pay_about_title),
                onNavigationButtonClick = onBackClick,
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_header),
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.size(dimensionResource(id = R.dimen.major_100)))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_description),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                if (state.importantInfo != null) {
                    TapToPayAboutScreenImportantInfo(state.importantInfo)
                } else {
                    Spacer(Modifier.size(dimensionResource(id = R.dimen.major_100)))
                }

                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_how_works_title),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_how_works_1_updated),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_how_works_2),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_how_works_3),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_how_works_4),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_how_works_5),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.size(16.dp))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_about_copyright),
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = R.color.color_on_surface_disabled),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.size(16.dp))
                Divider()

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 8.dp)
                )

                LearnMoreAboutSection(
                    textWithUrl = R.string.card_reader_tap_to_pay_learn_more,
                    onClick = state.onLearnMoreAboutTapToPay,
                )

                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    )
}

@Composable
fun TapToPayAboutScreenImportantInfo(importantInfo: TapToPayAboutViewModel.UiState.ImportantInfo) {
    Spacer(Modifier.size(dimensionResource(id = R.dimen.major_200)))
    Column(
        modifier = Modifier
            .background(
                color = colorResource(id = R.color.tap_to_pay_about_important_info_background),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tintable_info_outline_24dp),
                contentDescription = stringResource(
                    id = R.string.card_reader_upsell_card_reader_banner_dismiss
                ),
                tint = colorResource(id = R.color.color_on_surface),
            )
            Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
            Text(
                text = stringResource(id = R.string.card_reader_tap_to_pay_about_important_info_title),
                style = MaterialTheme.typography.h6,
                color = colorResource(id = R.color.color_on_surface)
            )
        }
        Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = importantInfo.pinDescription,
            style = MaterialTheme.typography.body1,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = stringResource(id = R.string.card_reader_tap_to_pay_about_important_info_description_2),
            style = MaterialTheme.typography.body1,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = stringResource(id = R.string.card_reader_tap_to_pay_about_important_info_description_3),
            style = MaterialTheme.typography.body1,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = stringResource(R.string.card_reader_tap_to_pay_about_important_info_button),
            fontWeight = FontWeight.W600,
            color = colorResource(id = R.color.color_primary),
            modifier = Modifier.clickable { importantInfo.onLearnMoreAboutCardReaders() }
        )
    }
    Spacer(Modifier.size(dimensionResource(id = R.dimen.major_200)))
}

@PreviewLightDark
@Composable
fun TapToPaySummaryAboutPreview() {
    WooThemeWithBackground {
        TapToPayAboutScreen(
            onBackClick = {},
            state = TapToPayAboutViewModel.UiState(
                importantInfo = TapToPayAboutViewModel.UiState.ImportantInfo(
                    pinDescription =
                    "In Australia, some cards require a PIN for contactless transactions above \$200. ",
                    onLearnMoreAboutCardReaders = {},
                ),
                onLearnMoreAboutTapToPay = {}
            ),
        )
    }
}

@Preview
@Composable
fun TapToPaySummaryAboutWithoutImportantInfoPreview() {
    WooThemeWithBackground {
        TapToPayAboutScreen(
            onBackClick = {},
            state = TapToPayAboutViewModel.UiState(
                importantInfo = null,
                onLearnMoreAboutTapToPay = {},
            )
        )
    }
}
