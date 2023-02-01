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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.domain.DomainChangeViewModel.ViewState.DomainsState
import com.woocommerce.android.ui.prefs.domain.DomainChangeViewModel.ViewState.LoadingState

@Composable
fun CurrentDomainsScreen(viewModel: DomainChangeViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        Crossfade(targetState = state) { viewState ->
            Scaffold(topBar = {
                Toolbar(
                    title = stringResource(id = string.domains),
                    onNavigationButtonClick = viewModel::onCancelPressed,
                    onActionButtonClick = viewModel::onHelpPressed,
                )
            }) { padding ->
                when (viewState) {
                    is DomainsState -> {
                        DomainChange(
                            domainsState = viewState,
                            onFindDomainButtonTapped = viewModel::onFindDomainButtonTapped,
                            onDismissBannerButtonTapped = viewModel::onDismissBannerButtonTapped,
                            modifier = Modifier
                                .background(MaterialTheme.colors.surface)
                                .fillMaxSize()
                                .padding(padding)
                        )
                    }
                    is LoadingState -> ProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DomainChange(
    domainsState: DomainsState,
    onFindDomainButtonTapped: () -> Unit,
    onDismissBannerButtonTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        WPComDomain(domainsState.wpComDomain.url, domainsState.wpComDomain.isPrimary)

        val isBannerVisible = remember { mutableStateOf(domainsState.isDomainClaimBannerVisible) }
        AnimatedVisibility(visible = isBannerVisible.value, exit = slideOutVertically()) {
            ClaimDomainBanner(
                onClaimDomainButtonTapped = onFindDomainButtonTapped,
                onDismissBannerButtonTapped = onDismissBannerButtonTapped
            )
        }

        if (domainsState.paidDomains.isEmpty()) {
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
                Modifier.padding(
                    horizontal = dimensionResource(id = dimen.major_100),
                    vertical = dimensionResource(id = dimen.minor_50)
                )
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = drawable.ic_info_outline_20dp),
                    contentDescription = stringResource(string.domains_learn_more)
                )
                Text(
                    modifier = Modifier.padding(start = dimensionResource(id = dimen.major_100)),
                    text = stringResource(id = string.domains_learn_more),
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = color.color_on_surface_medium)
                )
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
                items(domainsState.paidDomains) { domain ->
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
private fun ClaimDomainBanner(
    onClaimDomainButtonTapped: () -> Unit,
    onDismissBannerButtonTapped: () -> Unit
) {
    Column {
        Divider()
        ConstraintLayout(
            modifier = Modifier.fillMaxWidth()
        ) {
            val (title, description, claimLink, image, closeButton) = createRefs()
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
            IconButton(
                modifier = Modifier
                    .constrainAs(closeButton) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    },
                onClick = onDismissBannerButtonTapped
            ) {
                Icon(
                    imageVector = Filled.Close,
                    contentDescription = stringResource(string.domains_your_free_store_address)
                )
            }
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

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun NamePickerPreview() {
    WooThemeWithBackground {
        DomainChange(
            domainsState = DomainsState(
                wpComDomain = DomainsState.Domain(
                    url = "www.test.com",
                    renewalDate = "Renewal date: 12/12/2020",
                    isPrimary = true
                ),
                isDomainClaimBannerVisible = true,
                paidDomains = listOf(
                    DomainsState.Domain(
                        url = "www.cnn.com",
                        renewalDate = "Renewal date: 12/12/2020",
                        isPrimary = false
                    )
                ),
            ),
            onFindDomainButtonTapped = {},
            onDismissBannerButtonTapped = {}
        )
    }
}
