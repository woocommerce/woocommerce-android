package com.woocommerce.android.ui.blaze.creation.payment

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.CreditCardType
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.creation.payment.BlazeCampaignPaymentSummaryViewModel.CampaignCreationState
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils

@Composable
fun BlazeCampaignPaymentSummaryScreen(viewModel: BlazeCampaignPaymentSummaryViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        BlazeCampaignPaymentSummaryScreen(
            state = it,
            onBackClick = viewModel::onBackClicked,
            onSubmitCampaign = viewModel::onSubmitCampaign,
            onHelpClick = viewModel::onHelpClicked
        )
    }
}

@Composable
fun BlazeCampaignPaymentSummaryScreen(
    state: BlazeCampaignPaymentSummaryViewModel.ViewState,
    onBackClick: () -> Unit,
    onSubmitCampaign: () -> Unit,
    onHelpClick: () -> Unit
) {
    Scaffold(
        topBar = {
            if (state.campaignCreationState == null) {
                ToolbarWithHelpButton(
                    onNavigationButtonClick = onBackClick,
                    onHelpButtonClick = onHelpClick,
                )
            } else {
                Toolbar(onNavigationButtonClick = onBackClick)
            }
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        when (state.campaignCreationState) {
            is CampaignCreationState.Loading -> CampaignCreationLoadingUi(
                modifier = Modifier.padding(paddingValues)
            )

            is CampaignCreationState.Failed -> CampaignCreationErrorUi(
                errorMessage = state.campaignCreationState.errorMessage,
                onRetryClick = onSubmitCampaign,
                onHelpClick = onHelpClick,
                onCancelClick = onBackClick,
                modifier = Modifier.padding(paddingValues)
            )
            else -> PaymenSummaryContent(
                state = state,
                onSubmitCampaign = onSubmitCampaign,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun PaymenSummaryContent(
    state: BlazeCampaignPaymentSummaryViewModel.ViewState,
    onSubmitCampaign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = R.string.blaze_campaign_payment_summary_title),
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )

        PaymentTotals(
            displayBudget = state.displayBudget,
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

@Composable
private fun CampaignCreationLoadingUi(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = dimensionResource(id = R.dimen.major_150),
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_timer),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = R.dimen.image_major_72))
        )
        Text(text = stringResource(id = R.string.blaze_campaign_creation_loading))
        CircularProgressIndicator()
    }
}

@Composable
private fun CampaignCreationErrorUi(
    @StringRes errorMessage: Int,
    onRetryClick: () -> Unit,
    onHelpClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colors.error,
                modifier = Modifier.size(dimensionResource(id = R.dimen.image_major_64))
            )

            Text(
                text = stringResource(id = R.string.blaze_campaign_creation_error_title),
                style = MaterialTheme.typography.h5
            )

            Text(
                text = stringResource(errorMessage),
                style = MaterialTheme.typography.body2
            )

            Text(
                text = stringResource(id = R.string.blaze_campaign_creation_error_payment_hint),
                style = MaterialTheme.typography.subtitle1,
            )

            Text(
                text = stringResource(id = R.string.blaze_campaign_creation_error_help_hint),
                style = MaterialTheme.typography.body2,
            )

            WCTextButton(
                onClick = onHelpClick,
                text = stringResource(id = R.string.blaze_campaign_creation_error_get_support),
                icon = ImageVector.vectorResource(id = R.drawable.ic_help_24dp),
                allCaps = false,
                contentPadding = PaddingValues(vertical = dimensionResource(id = R.dimen.minor_100))
            )
        }

        WCColoredButton(
            onClick = onRetryClick,
            text = stringResource(id = R.string.try_again),
            modifier = Modifier.fillMaxWidth()
        )
        WCTextButton(
            onClick = onCancelClick,
            text = stringResource(id = R.string.blaze_campaign_creation_error_cancel),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PaymentTotals(
    displayBudget: String,
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
                text = displayBudget,
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
                text = displayBudget,
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

@LightDarkThemePreviews
@Composable
private fun BlazeCampaignPaymentSummaryScreenPreview() {
    WooThemeWithBackground {
        BlazeCampaignPaymentSummaryScreen(
            state = BlazeCampaignPaymentSummaryViewModel.ViewState(
                displayBudget = "100 USD",
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
                        )
                    ),
                    onClick = {}
                ),
                selectedPaymentMethodId = "1"
            ),
            onBackClick = {},
            onSubmitCampaign = {},
            onHelpClick = {}
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BlazeCampaignCreationLoadingPreview() {
    WooThemeWithBackground {
        CampaignCreationLoadingUi(modifier = Modifier.size(width = 360.dp, height = 640.dp))
    }
}

@LightDarkThemePreviews
@Composable
private fun BlazeCampaignCreationErrorPreview() {
    WooThemeWithBackground {
        CampaignCreationErrorUi(
            errorMessage = R.string.error_generic,
            onRetryClick = {},
            onHelpClick = {},
            onCancelClick = {},
            modifier = Modifier.size(width = 360.dp, height = 640.dp)
        )
    }
}
