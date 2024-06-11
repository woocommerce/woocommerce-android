package com.woocommerce.android.ui.prefs.domain

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.DashboardState
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.ErrorState.ErrorType
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.ErrorState.ErrorType.ACCESS_ERROR
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.LoadingState

@Composable
fun DomainDashboardScreen(viewModel: DomainDashboardViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        Crossfade(targetState = state) { viewState ->
            Scaffold(topBar = {
                ToolbarWithHelpButton(
                    title = stringResource(id = string.domains),
                    onNavigationButtonClick = viewModel::onCancelPressed,
                    onHelpButtonClick = viewModel::onHelpPressed
                )
            }) { padding ->
                when (viewState) {
                    is DashboardState -> {
                        DomainDashboard(
                            dashboardState = viewState,
                            onFindDomainButtonTapped = viewModel::onFindDomainButtonTapped,
                            onLearnMoreButtonTapped = viewModel::onLearnMoreButtonTapped,
                            modifier = Modifier
                                .background(MaterialTheme.colors.surface)
                                .fillMaxSize()
                                .padding(padding)
                        )
                    }
                    is ErrorState -> ErrorScreen(viewState.errorType)
                    LoadingState -> ProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DomainDashboard(
    dashboardState: DashboardState,
    onFindDomainButtonTapped: () -> Unit,
    onLearnMoreButtonTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        WPComDomain(dashboardState.wpComDomain.url, dashboardState.wpComDomain.isPrimary)

        val isBannerVisible = remember { mutableStateOf(dashboardState.isDomainClaimBannerVisible) }
        AnimatedVisibility(visible = isBannerVisible.value, exit = slideOutVertically()) {
            ClaimDomainBanner(onClaimDomainButtonTapped = onFindDomainButtonTapped)
        }

        if (dashboardState.paidDomains.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
            )
            Divider()
            WCColoredButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = dimen.major_100),
                        end = dimensionResource(id = dimen.major_100),
                        top = dimensionResource(id = dimen.major_100),
                    ),
                onClick = onFindDomainButtonTapped
            ) {
                Text(text = stringResource(id = string.domains_search_for_domain_button_title))
            }
            Row(
                modifier = Modifier.padding(
                    start = dimensionResource(id = dimen.major_100),
                    end = dimensionResource(id = dimen.major_100),
                    top = dimensionResource(id = dimen.minor_50),
                    bottom = dimensionResource(id = dimen.major_100)
                )
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = drawable.ic_info_outline_20dp),
                    contentDescription = stringResource(string.domains_learn_more)
                )
                val text = annotatedStringRes(stringResId = string.domains_learn_more)
                ClickableText(
                    modifier = Modifier.padding(start = dimensionResource(id = dimen.minor_100)),
                    text = text,
                    style = MaterialTheme.typography.caption.copy(
                        color = colorResource(id = color.color_on_surface_medium)
                    ),
                ) {
                    text.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
                        .firstOrNull()
                        ?.let { onLearnMoreButtonTapped() }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = dimensionResource(id = dimen.major_100),
                        end = dimensionResource(id = dimen.major_100)
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_75))
            ) {
                stickyHeader {
                    Text(
                        text = stringResource(id = string.domains_your_domains).uppercase(),
                        style = MaterialTheme.typography.caption,
                        color = colorResource(id = color.color_on_surface_medium)
                    )
                }
                items(dashboardState.paidDomains) { domain ->
                    Domain(domain.url, domain.renewalDate, domain.isPrimary)
                }
                item {
                    WCTextButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = onFindDomainButtonTapped,
                        allCaps = true,
                        icon = Icons.Default.Add,
                        text = stringResource(id = string.domains_add_domain_button_title),
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClaimDomainBanner(onClaimDomainButtonTapped: () -> Unit) {
    Column {
        Divider()
        ConstraintLayout(
            modifier = Modifier.fillMaxWidth()
        ) {
            val (title, description, claimLink, image) = createRefs()
            Text(
                modifier = Modifier
                    .padding(dimensionResource(id = dimen.major_100))
                    .constrainAs(title) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    },
                text = stringResource(id = string.domains_claim_your_free_domain_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Image(
                modifier = Modifier
                    .constrainAs(image) {
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    },
                imageVector = ImageVector.vectorResource(id = drawable.img_claim_domain),
                contentDescription = stringResource(string.domains_claim_your_free_domain_link)
            )
            Text(
                modifier = Modifier
                    .padding(start = dimensionResource(id = dimen.major_100))
                    .constrainAs(description) {
                        top.linkTo(title.bottom)
                        start.linkTo(title.start)
                        end.linkTo(image.start)
                        bottom.linkTo(claimLink.top)
                        width = Dimension.fillToConstraints
                    },
                text = stringResource(id = string.domains_claim_your_free_domain_description),
                style = MaterialTheme.typography.subtitle1,
            )
            WCTextButton(
                modifier = Modifier
                    .constrainAs(claimLink) {
                        top.linkTo(description.bottom)
                    },
                onClick = onClaimDomainButtonTapped,
                contentPadding = PaddingValues(dimensionResource(id = dimen.major_100))
            ) {
                Text(text = stringResource(id = string.domains_claim_your_free_domain_link))
            }
        }

        Divider()

        Text(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = dimen.major_100),
                    end = dimensionResource(id = dimen.major_100),
                    top = dimensionResource(id = dimen.minor_100),
                    bottom = dimensionResource(id = dimen.major_150)
                ),
            text = stringResource(id = string.domains_purchase_redirect_notice),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = color.color_on_surface_medium)
        )
    }
}

@Composable
private fun WPComDomain(url: String, isPrimary: Boolean) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = dimensionResource(id = dimen.major_100),
                vertical = dimensionResource(id = dimen.major_200)
            )
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = string.domains_your_free_store_address),
            style = MaterialTheme.typography.subtitle1
        )
        Text(
            text = url,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )

        if (isPrimary) {
            PrimaryDomainTag()
        }
    }
}

@Composable
private fun Domain(url: String, renewal: String?, isPrimary: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = url,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )
        if (renewal != null) {
            Text(
                text = renewal,
                color = colorResource(id = color.color_on_surface_medium),
            )
        }

        if (isPrimary) {
            PrimaryDomainTag()
        }
    }
}

@Composable
private fun PrimaryDomainTag() {
    Box(
        modifier = Modifier
            .padding(vertical = dimensionResource(id = dimen.minor_100))
            .clip(RoundedCornerShape(35))
            .background(colorResource(id = color.tag_bg_main))
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = dimensionResource(id = dimen.minor_75),
                vertical = dimensionResource(id = dimen.minor_25)
            ),
            text = stringResource(id = string.domains_primary_address),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = color.tag_text_main),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ErrorScreen(errorType: ErrorType) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(painter = painterResource(id = drawable.img_woo_generic_error), contentDescription = null)

            val message = when (errorType) {
                ErrorType.GENERIC_ERROR -> stringResource(id = string.domains_generic_error_title)
                ErrorType.ACCESS_ERROR -> stringResource(id = string.domains_access_error_title)
            }
            Text(
                text = message,
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .padding(
                        top = dimensionResource(id = dimen.major_300),
                        start = dimensionResource(id = dimen.major_300),
                        end = dimensionResource(id = dimen.major_300),
                        bottom = dimensionResource(id = dimen.major_100)
                    ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PaidDomainsPreview() {
    WooThemeWithBackground {
        DomainDashboard(
            dashboardState = DashboardState(
                wpComDomain = DashboardState.Domain(
                    url = "www.test.com",
                    renewalDate = "Renewal date: 12/12/2020",
                    isPrimary = true
                ),
                isDomainClaimBannerVisible = true,
                paidDomains = listOf(
                    DashboardState.Domain(
                        url = "www.cnn.com",
                        renewalDate = "Renewal date: 12/12/2020",
                        isPrimary = false
                    )
                ),
            ),
            onFindDomainButtonTapped = {},
            onLearnMoreButtonTapped = {}
        )
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun NoDomainsPickerPreview() {
    WooThemeWithBackground {
        DomainDashboard(
            dashboardState = DashboardState(
                wpComDomain = DashboardState.Domain(
                    url = "www.test.com",
                    renewalDate = "Renewal date: 12/12/2020",
                    isPrimary = true
                ),
                isDomainClaimBannerVisible = true,
                paidDomains = listOf(),
            ),
            onFindDomainButtonTapped = {},
            onLearnMoreButtonTapped = {}
        )
    }
}

@Preview()
@Composable
fun ErrorScreenPreview() {
    ErrorScreen(ACCESS_ERROR)
}
