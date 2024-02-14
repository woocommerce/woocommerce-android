package com.woocommerce.android.ui.blaze.creation.payment

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.CreditCardType
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils
import java.util.Date

@Composable
fun BlazeCampaignPaymentSummaryScreen(viewModel: BlazeCampaignPaymentSummaryViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        BlazeCampaignPaymentSummaryScreen(
            state = it,
            onBackClick = viewModel::onBackClicked,
            onSubmitCampaign = viewModel::onSubmitCampaign
        )
    }
}

@Composable
fun BlazeCampaignPaymentSummaryScreen(
    state: BlazeCampaignPaymentSummaryViewModel.ViewState,
    onBackClick: () -> Unit,
    onSubmitCampaign: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Toolbar(onNavigationButtonClick = onBackClick)
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.blaze_campaign_payment_summary_title),
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )

            PaymentTotals(
                budget = state.budget,
                modifier = Modifier.fillMaxWidth()
            )

            PaymentMethod(
                paymentMethodsState = state.paymentMethodsState,
                selectedPaymentMethod = state.selectedPaymentMethod,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))
            Divider()

            WCColoredButton(
                onClick = onSubmitCampaign,
                text = stringResource(id = R.string.blaze_campaign_payment_summary_submit_campaign),
                enabled = state.isPaymentMethodSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )

            val termsOfServices = annotatedStringRes(
                stringResId = R.string.blaze_campaign_payment_summary_terms_and_conditions
            )
            ClickableText(
                text = termsOfServices,
                style = MaterialTheme.typography.caption.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                ),
                onClick = { offset ->
                    termsOfServices.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = offset, end = offset)
                        .firstOrNull()
                        ?.let { annotation ->
                            when (annotation.item) {
                                "termsOfService" ->
                                    ChromeCustomTabUtils.launchUrl(context, AppUrls.WORPRESS_COM_TERMS)

                                "advertisingPolicy" ->
                                    ChromeCustomTabUtils.launchUrl(context, AppUrls.ADVERTISING_POLICY)

                                "learnMore" ->
                                    ChromeCustomTabUtils.launchUrl(context, AppUrls.BLAZE_SUPPORT)
                            }
                        }
                },
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@Composable
private fun PaymentTotals(
    budget: BlazeRepository.Budget,
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_200)),
        modifier = modifier.padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = R.string.blaze_campaign_payment_summary_totals),
            style = MaterialTheme.typography.body2
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.blaze_campaign_payment_summary_campaign_budget),
                style = MaterialTheme.typography.body2
            )

            Text(
                // TODO format correctly
                text = "${budget.currencyCode}${budget.totalBudget}",
                style = MaterialTheme.typography.body2
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.blaze_campaign_payment_summary_total_budget),
                style = MaterialTheme.typography.subtitle2
            )

            Text(
                // TODO format correctly
                text = "${budget.currencyCode}${budget.totalBudget}",
                style = MaterialTheme.typography.subtitle2
            )
        }
    }
}

@Composable
private fun PaymentMethod(
    paymentMethodsState: BlazeCampaignPaymentSummaryViewModel.PaymentMethodsState,
    selectedPaymentMethod: BlazeRepository.PaymentMethod?,
    modifier: Modifier
) {
    Column(modifier) {
        Divider()

        when (paymentMethodsState) {
            BlazeCampaignPaymentSummaryViewModel.PaymentMethodsState.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
                ) {
                    Text(
                        text = stringResource(id = R.string.blaze_campaign_payment_summary_loading_payment_methods),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                        modifier = Modifier.weight(1f)
                    )
                    CircularProgressIndicator()
                }
            }

            is BlazeCampaignPaymentSummaryViewModel.PaymentMethodsState.Success -> {
                PaymentMethodInfo(
                    paymentMethod = selectedPaymentMethod,
                    onClick = paymentMethodsState.onClick,
                    modifier = Modifier
                )
            }

            is BlazeCampaignPaymentSummaryViewModel.PaymentMethodsState.Error -> {
                Text(
                    text = stringResource(id = R.string.blaze_campaign_payment_summary_error_loading_payment_methods),
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .padding(dimensionResource(id = R.dimen.major_100))
                        .clickable(onClick = paymentMethodsState.onRetry)
                )
            }
        }

        Divider()
    }
}

@Composable
private fun PaymentMethodInfo(
    paymentMethod: BlazeRepository.PaymentMethod?,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        if (paymentMethod == null) {
            Text(
                text = stringResource(id = R.string.blaze_campaign_payment_summary_add_payment_method),
                modifier = Modifier.weight(1f),
            )
        } else {
            if (paymentMethod.info is BlazeRepository.PaymentMethod.PaymentMethodInfo.CreditCard) {
                Image(
                    painter = painterResource(id = paymentMethod.info.creditCardType.icon),
                    contentDescription = null
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = paymentMethod.name,
                    style = MaterialTheme.typography.subtitle1
                )

                Text(
                    text = paymentMethod.subtitle ?: "",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                )
            }
        }
        Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_right), contentDescription = null)
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun BlazeCampaignPaymentSummaryScreenPreview() {
    WooThemeWithBackground {
        BlazeCampaignPaymentSummaryScreen(
            state = BlazeCampaignPaymentSummaryViewModel.ViewState(
                budget = BlazeRepository.Budget(
                    100f,
                    spentBudget = 0f,
                    durationInDays = 7,
                    startDate = Date(),
                    currencyCode = "$"
                ),
                paymentMethodsState = BlazeCampaignPaymentSummaryViewModel.PaymentMethodsState.Success(
                    BlazeRepository.PaymentMethodsData(
                        listOf(
                            BlazeRepository.PaymentMethod(
                                id = "1",
                                name = "Visa **** 1234",
                                info = BlazeRepository.PaymentMethod.PaymentMethodInfo.CreditCard(
                                    creditCardType = CreditCardType.VISA,
                                    cardHolderName = "John Doe"
                                )
                            )
                        ),
                        BlazeRepository.PaymentMethodUrls(
                            formUrl = "https://example.com/form",
                            successUrl = "https://example.com/success",
                            idUrlParameter = "id"
                        )
                    ),
                    onClick = {}
                ),
                selectedPaymentMethodId = "1"
            ),
            onBackClick = {},
            onSubmitCampaign = {}
        )
    }
}
