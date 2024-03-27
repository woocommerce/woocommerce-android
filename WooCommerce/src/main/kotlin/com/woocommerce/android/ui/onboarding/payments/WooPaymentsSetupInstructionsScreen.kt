package com.woocommerce.android.ui.onboarding.payments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WooPaymentsSetupInstructionsScreen(
    onCloseButtonClick: () -> Unit = {},
    onWPComAccountMoreDetailsClick: () -> Unit = {},
    onBeginButtonClick: () -> Unit = {},
    onLearnMoreClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            Toolbar(
                navigationIcon = Icons.Filled.Close,
                onNavigationButtonClick = onCloseButtonClick,
            )
        },
        bottomBar = {
            WooPaymentsSetupInstructionsFooter(
                onBeginButtonClick,
                onLearnMoreClick
            )
        }
    ) { paddingValues ->
        WooPaymentsSetupInstructionsContent(
            onWPComAccountMoreDetailsClick,
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}

@Composable
private fun WooPaymentsSetupInstructionsContent(
    onWPComAccountMoreDetailsClick: () -> Unit = {},
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        Image(
            painter = painterResource(R.drawable.img_woo_payments_logo),
            contentDescription = "WooPayments",
            alignment = Alignment.TopStart,
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = stringResource(id = R.string.store_onboarding_wcpay_instructions_title),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = stringResource(id = R.string.store_onboarding_wcpay_instructions_estimate_title),
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.woo_gray_40),
        )
        Text(
            text = stringResource(id = R.string.store_onboarding_wcpay_instructions_estimate_time),
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold
        )

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10),
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_150))
        )

        Text(
            text = stringResource(id = R.string.store_onboarding_wcpay_instructions_content_title),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_100))
        )

        val text = annotatedStringRes(stringResId = R.string.store_onboarding_wcpay_instructions_content_step_1_content)
        WooPaymentsSetupInstructionsStep(stepNumber = 1) {
            ClickableText(
                text = text,
                style = MaterialTheme.typography.subtitle1
                    .copy(color = colorResource(id = color.color_on_surface)),
            ) {
                text.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
                    .firstOrNull()
                    ?.let { onWPComAccountMoreDetailsClick() }
            }
        }

        WooPaymentsSetupInstructionsStep(
            stepNumber = 2
        ) {
            Text(text = stringResource(id = string.store_onboarding_wcpay_instructions_content_step_2_content))
        }
    }
}

@Composable
private fun WooPaymentsSetupInstructionsStep(stepNumber: Int, formattedText: @Composable () -> Unit) {
    val format = NumberFormat.getInstance(Locale.getDefault())
    val formattedNumber = format.format(stepNumber)

    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.major_200))
                    .background(
                        color = colorResource(R.color.woo_payments_setup_bullet_background),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedNumber,
                    color = colorResource(id = R.color.color_on_surface)
                )
            }
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))
            formattedText()
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    }
}

@Composable
private fun WooPaymentsSetupInstructionsFooter(
    onBeginButtonClick: () -> Unit = {},
    onLearnMoreClick: () -> Unit = {}
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
            onClick = onBeginButtonClick,
        ) {
            Text(text = stringResource(id = R.string.store_onboarding_wcpay_instructions_content_button))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp)
            )
            val text = annotatedStringRes(stringResId = R.string.store_onboarding_wcpay_instructions_content_learn_more)
            ClickableText(
                text = text,
                style = MaterialTheme.typography.subtitle2
                    .copy(color = colorResource(id = R.color.color_on_surface_medium)),
            ) {
                text.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
                    .firstOrNull()
                    ?.let { onLearnMoreClick() }
            }
        }
    }
}

@Preview
@Composable
private fun WooPaymentsSetupInstructionsScreenPreview() {
    WooThemeWithBackground {
        WooPaymentsSetupInstructionsScreen()
    }
}
