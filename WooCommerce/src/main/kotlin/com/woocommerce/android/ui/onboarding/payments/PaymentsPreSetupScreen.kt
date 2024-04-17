package com.woocommerce.android.ui.onboarding.payments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun PaymentsPreSetupScreen(
    isWooPaymentsTask: Boolean,
    backButtonClick: () -> Unit = {},
    onTermsOfServiceClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onContinueButtonClick: () -> Unit = {},
    onLearnMoreButtonClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            Toolbar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationButtonClick = backButtonClick,
            )
        },
        bottomBar = {
            PaymentsPreSetupFooter(
                onContinueButtonClick,
                onLearnMoreButtonClick
            )
        },
        modifier = Modifier.background(color = colorResource(id = R.color.color_surface))
    ) { paddingValues ->
        PaymentsPreSetupContent(
            isWooPaymentsTask = isWooPaymentsTask,
            onTermsOfServiceClick = onTermsOfServiceClick,
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}

@Composable
private fun PaymentsPreSetupContent(
    isWooPaymentsTask: Boolean,
    onTermsOfServiceClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_300)))
        Image(
            painter = painterResource(R.drawable.img_woo_payments_store_onboarding_setup_dialog),
            contentDescription = "WooPayments setup dialog image",
            alignment = Alignment.TopStart,
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = stringResource(id = R.string.store_onboarding_payments_pre_setup_title_one),
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = stringResource(
                id = if (isWooPaymentsTask) {
                    R.string.store_onboarding_payments_pre_setup_title_wcpay
                } else {
                    R.string.store_onboarding_payments_pre_setup_title_generic
                }
            ),
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_100)),
            color = colorResource(id = R.color.woo_purple_50)
        )

        val bodyText = annotatedStringRes(
            stringResId = if (isWooPaymentsTask) {
                R.string.store_onboarding_wcpay_setup_description
            } else {
                R.string.store_onboarding_payments_setup_description
            }
        )
        ClickableText(
            text = bodyText,
            style = MaterialTheme.typography.body1.copy(color = colorResource(id = R.color.color_on_surface_medium))
        ) {
            bodyText.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
                .firstOrNull()
                ?.let { annotation ->
                    when (annotation.item) {
                        "termsOfService" -> onTermsOfServiceClick()
                        "privacyPolicy" -> onPrivacyPolicyClick()
                    }
                }
        }
    }
}

@Composable
private fun PaymentsPreSetupFooter(
    onContinueButtonClick: () -> Unit = {},
    onLearnMoreButtonClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(vertical = dimensionResource(id = R.dimen.major_100))
    ) {
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10),
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100))
        )
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            onClick = onContinueButtonClick,
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }

        WCOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            onClick = onLearnMoreButtonClick,
        ) {
            Text(text = stringResource(id = R.string.learn_more))
        }
    }
}

@Preview
@Composable
private fun WooPaymentsPreSetupPreview() {
    WooThemeWithBackground {
        PaymentsPreSetupScreen(isWooPaymentsTask = true)
    }
}

@Preview
@Composable
private fun PaymentsPreSetupPreview() {
    WooThemeWithBackground {
        PaymentsPreSetupScreen(isWooPaymentsTask = false)
    }
}
