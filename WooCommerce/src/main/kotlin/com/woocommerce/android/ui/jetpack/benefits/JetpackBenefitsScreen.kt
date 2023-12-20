package com.woocommerce.android.ui.jetpack.benefits

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun JetpackBenefitsScreen(viewModel: JetpackBenefitsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackBenefitsScreen(
            viewState = it,
            onInstallClick = viewModel::onInstallClick,
            onDismissClick = viewModel::onDismiss
        )
    }
}

@Composable
fun JetpackBenefitsScreen(
    viewState: JetpackBenefitsViewModel.ViewState,
    onInstallClick: () -> Unit = {},
    onDismissClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(top = dimensionResource(id = R.dimen.major_350))
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.jetpack_benefits_modal_title),
                style = MaterialTheme.typography.h4,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            Text(
                text = stringResource(id = R.string.jetpack_benefits_modal_subtitle),
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

            // Push notifications
            BenefitEntry(
                icon = R.drawable.ic_alarm_bell_ring,
                title = R.string.jetpack_benefits_modal_push_notifications_title,
                subtitle = R.string.jetpack_benefits_modal_push_notifications_subtitle,
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

            // Analytics
            BenefitEntry(
                icon = R.drawable.ic_phone_analytics,
                title = R.string.jetpack_benefits_modal_analytics_title,
                subtitle = R.string.jetpack_benefits_modal_analytics_subtitle,
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

            if (viewState.isUsingJetpackCP) {
                // User profiles
                BenefitEntry(
                    icon = R.drawable.ic_users,
                    title = R.string.jetpack_benefits_modal_user_profiles_title,
                    subtitle = R.string.jetpack_benefits_modal_user_profiles_subtitle,
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
                )
            } else {
                // Multiple stores
                BenefitEntry(
                    icon = R.drawable.ic_users, // TODO update this icon
                    title = R.string.jetpack_benefits_modal_multiple_stores_title,
                    subtitle = R.string.jetpack_benefits_modal_multiple_stores_subtitle,
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
                )
            }
        }

        WCColoredButton(onClick = onInstallClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    id = if (viewState.isUsingJetpackCP) R.string.jetpack_benefits_modal_install_jetpack
                    else R.string.jetpack_benefits_modal_login
                )
            )
        }

        WCOutlinedButton(onClick = onDismissClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.jetpack_benefits_modal_dismiss))
        }
    }

    if (viewState.isLoadingDialogShown) {
        ProgressDialog(
            title = stringResource(id = R.string.jetpack_benefits_fetching_status),
            subtitle = stringResource(id = R.string.please_wait)
        )
    }
}

@Composable
private fun BenefitEntry(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.image_minor_100))
                .background(color = colorResource(id = R.color.woo_gray_0), shape = CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(id = title), style = MaterialTheme.typography.subtitle1)
            Text(text = stringResource(id = subtitle), style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
@Preview
private fun JetpackBenefitsScreenPreview() {
    WooThemeWithBackground {
        JetpackBenefitsScreen(
            viewState = JetpackBenefitsViewModel.ViewState(
                isUsingJetpackCP = false,
                isLoadingDialogShown = false,
            )
        )
    }
}

@Composable
@Preview
private fun JetpackBenefitsScreenWithoutNativeInstallPreview() {
    WooThemeWithBackground {
        JetpackBenefitsScreen(
            viewState = JetpackBenefitsViewModel.ViewState(
                isUsingJetpackCP = false,
                isLoadingDialogShown = false,
            )
        )
    }
}
