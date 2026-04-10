package com.woocommerce.android.ui.troubleshooting

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success

@Composable
fun TroubleshootConnectionScreen(viewModel: TroubleshootConnectionViewModel) {
    val isCheckFinished by viewModel.isCheckFinished.observeAsState()
    val viewState by viewModel.viewState.observeAsState()
    val technicalDetails by viewModel.technicalDetailsToShow.observeAsState()

    TroubleshootConnectionScreen(
        shouldEnableContactSupportButton = isCheckFinished ?: false,
        shouldDisplaySummarySection = viewState?.shouldDisplaySummary ?: false,
        checks = viewState?.checks ?: emptyList(),
        onContactSupportClicked = viewModel::onContactSupportClicked,
        onReturnClick = viewModel::onReturnClicked,
        onRetryClick = viewModel::onRetryClicked,
        onReadMoreClick = viewModel::onReadMoreClicked,
        onViewTechnicalDetailsClicked = viewModel::onViewTechnicalDetailsClicked
    )

    technicalDetails?.let { details ->
        TechnicalDetailsBottomSheet(
            technicalDetails = details,
            onDismiss = viewModel::onTechnicalDetailsDismissed
        )
    }
}

@Composable
fun TroubleshootConnectionScreen(
    shouldEnableContactSupportButton: Boolean,
    shouldDisplaySummarySection: Boolean,
    checks: List<ConnectivityCheckCardData>,
    onContactSupportClicked: () -> Unit,
    onReturnClick: () -> Unit,
    onRetryClick: (ConnectivityCheckType) -> Unit,
    onReadMoreClick: (FailureType) -> Unit,
    onViewTechnicalDetailsClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(colorResource(id = R.color.color_surface))
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
            modifier = modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.orderlist_connectivity_tool_title),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(id = R.string.orderlist_connectivity_tool_subtitle))
        }

        checks.forEach { checkData ->
            if (checkData.status !is NotStarted) {
                ConnectivityCheckCard(
                    checkData = checkData,
                    onRetryClick = { onRetryClick(checkData.type) },
                    onReadMoreClick = { onReadMoreClick((checkData.status as? Failure)?.error ?: FailureType.GENERIC) },
                    onViewTechnicalDetailsClicked = onViewTechnicalDetailsClicked
                )
                Divider(
                    modifier = Modifier
                        .padding(start = dimensionResource(id = R.dimen.major_100))
                )
            }
        }

        ConnectivitySummary(
            shouldDisplaySummarySection = shouldDisplaySummarySection,
            onReturnClick = onReturnClick,
            modifier = modifier
        )

        Spacer(modifier = modifier.weight(1f))
        WCOutlinedButton(
            enabled = shouldEnableContactSupportButton,
            onClick = { onContactSupportClicked() },
            modifier = modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.orderlist_connectivity_tool_contact_support_action))
        }
    }
}

@Composable
fun ConnectivityCheckCard(
    checkData: ConnectivityCheckCardData,
    onRetryClick: () -> Unit,
    onReadMoreClick: () -> Unit,
    onViewTechnicalDetailsClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val failure = checkData.status as? Failure
    val shouldDisplayReadMoreButton = checkData.type != ConnectivityCheckType.INTERNET &&
        checkData.type != ConnectivityCheckType.WP_COM &&
        checkData.status is Failure

    ConnectivityCheckCard(
        modifier = modifier,
        checkTitle = checkData.type.title,
        iconDrawable = checkData.type.icon,
        suggestion = checkData.type.suggestion,
        checkStatus = checkData.status,
        onReadMoreClicked = onReadMoreClick,
        onRetryConnectionClicked = onRetryClick,
        shouldDisplayReadMoreButton = shouldDisplayReadMoreButton,
        onViewTechnicalDetailsClicked = failure?.technicalDetails?.let { details ->
            {
                onViewTechnicalDetailsClicked(details)
            }
        }
    )
}

@Composable
fun ConnectivityCheckCard(
    modifier: Modifier = Modifier,
    @StringRes checkTitle: Int,
    @DrawableRes iconDrawable: Int,
    @StringRes suggestion: Int,
    checkStatus: ConnectivityCheckStatus,
    onReadMoreClicked: () -> Unit,
    onRetryConnectionClicked: () -> Unit,
    shouldDisplayReadMoreButton: Boolean = false,
    onViewTechnicalDetailsClicked: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(PaddingValues(dimensionResource(id = R.dimen.major_100)))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconDrawable),
                tint = MaterialTheme.colors.onSurface,
                contentDescription = stringResource(id = checkTitle),
                modifier = modifier
                    .size(dimensionResource(id = R.dimen.major_100))
            )

            Text(
                text = stringResource(id = checkTitle),
                fontWeight = FontWeight.Bold,
                modifier = modifier
                    .padding(start = dimensionResource(id = R.dimen.minor_100))
                    .fillMaxHeight()
            )

            Spacer(modifier = modifier.weight(1f))
            when (checkStatus) {
                is InProgress -> CircularProgressIndicator(
                    modifier = modifier.size(dimensionResource(id = R.dimen.major_150))
                )

                is Success -> ResultIcon(
                    icon = ImageVector.vectorResource(R.drawable.ic_check_circle_filled_24dp),
                    color = R.color.woo_green_50
                )

                is Failure -> ResultIcon(
                    icon = ImageVector.vectorResource(R.drawable.ic_error_filled_24dp),
                    color = R.color.woo_red_50
                )

                is NotStarted -> {
                    /* Do nothing */
                }
            }
        }

        if (checkStatus is Failure) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .padding(top = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = checkStatus.error?.message ?: suggestion),
                    style = MaterialTheme.typography.body2,
                    modifier = modifier.align(Alignment.Start)
                )

                WCTextButton(
                    allCaps = false,
                    icon = ImageVector.vectorResource(R.drawable.ic_repeat_24dp),
                    onClick = onRetryConnectionClicked,
                    modifier = modifier.align(Alignment.Start),
                    text = stringResource(id = R.string.orderlist_connectivity_tool_retry_action),
                    contentPadding = PaddingValues(
                        vertical = dimensionResource(id = R.dimen.minor_100),
                        horizontal = dimensionResource(id = R.dimen.minor_00)
                    )
                )

                if (shouldDisplayReadMoreButton) {
                    WCTextButton(
                        allCaps = false,
                        onClick = onReadMoreClicked,
                        icon = ImageVector.vectorResource(R.drawable.ic_arrow_outward_24dp),
                        modifier = modifier.align(Alignment.Start),
                        text = stringResource(id = R.string.orderlist_connectivity_tool_read_more_action),
                        contentPadding = PaddingValues(
                            vertical = dimensionResource(id = R.dimen.minor_100),
                            horizontal = dimensionResource(id = R.dimen.minor_00)
                        )
                    )
                }

                onViewTechnicalDetailsClicked?.let { onClick ->
                    WCTextButton(
                        allCaps = false,
                        onClick = onClick,
                        icon = ImageVector.vectorResource(R.drawable.ic_tintable_info_outline_24dp),
                        modifier = modifier.align(Alignment.Start),
                        text = stringResource(id = R.string.connectivity_tool_view_technical_details),
                        contentPadding = PaddingValues(
                            vertical = dimensionResource(id = R.dimen.minor_100),
                            horizontal = dimensionResource(id = R.dimen.minor_00)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectivitySummary(
    shouldDisplaySummarySection: Boolean,
    onReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (shouldDisplaySummarySection) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
            modifier = modifier
                .padding(vertical = dimensionResource(id = R.dimen.major_100))
                .padding(start = dimensionResource(id = R.dimen.major_100))
        ) {
            Column(
                modifier = modifier
                    .padding(end = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = stringResource(id = R.string.orderlist_connectivity_tool_summary_title),
                    fontWeight = FontWeight.Bold,
                    modifier = modifier
                        .fillMaxHeight()
                )

                Text(
                    text = stringResource(id = R.string.orderlist_connectivity_tool_summary_suggestion),
                    style = MaterialTheme.typography.body2
                )

                WCTextButton(
                    allCaps = false,
                    onClick = onReturnClick,
                    text = stringResource(id = R.string.orderlist_connectivity_tool_return_action),
                    icon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                    contentPadding = PaddingValues(
                        vertical = dimensionResource(id = R.dimen.minor_100),
                        horizontal = dimensionResource(id = R.dimen.minor_00)
                    )
                )
            }

            Divider()
        }
    }
}

@Composable
fun ResultIcon(
    icon: ImageVector,
    @ColorRes color: Int
) {
    Image(
        imageVector = icon,
        colorFilter = ColorFilter.tint(colorResource(id = color)),
        contentDescription = null
    )
}

@Preview
@Composable
fun TroubleshootConnectionScreenPreview() {
    WooThemeWithBackground {
        TroubleshootConnectionScreen(
            shouldEnableContactSupportButton = true,
            shouldDisplaySummarySection = true,
            checks = listOf(
                ConnectivityCheckCardData(ConnectivityCheckType.INTERNET, NotStarted),
                ConnectivityCheckCardData(ConnectivityCheckType.WP_COM, Success()),
                ConnectivityCheckCardData(
                    ConnectivityCheckType.STORE,
                    Failure(
                        error = FailureType.PARSE,
                        technicalDetails = "Operation: Site Connection\n" +
                            "Error Type: INVALID_RESPONSE\n" +
                            "Description: Parse error"
                    )
                ),
                ConnectivityCheckCardData(ConnectivityCheckType.ORDERS, InProgress),
                ConnectivityCheckCardData(ConnectivityCheckType.PRODUCTS, NotStarted)
            ),
            onContactSupportClicked = {},
            onReturnClick = {},
            onRetryClick = {},
            onReadMoreClick = {},
            onViewTechnicalDetailsClicked = {}
        )
    }
}
