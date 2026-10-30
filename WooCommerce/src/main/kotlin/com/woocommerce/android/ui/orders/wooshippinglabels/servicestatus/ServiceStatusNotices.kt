package com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus.ServiceStatusNotice.Severity

@Composable
internal fun ServiceStatusNotices(
    notices: List<ServiceStatusNotice>,
    onActionClick: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    topSpacing: Dp = 0.dp
) {
    val initialNoticeIds = remember { notices.map { it.id }.toSet() }

    WooDesignSystemTheme {
        Column(modifier = modifier.animateContentSize()) {
            notices.forEachIndexed { index, notice ->
                key(notice.id) {
                    AnimatedServiceStatusNotice(
                        notice = notice,
                        animateIn = notice.id !in initialNoticeIds,
                        topPadding = if (index == 0) topSpacing else WooTheme.spacing.space3,
                        onActionClick = onActionClick,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedServiceStatusNotice(
    notice: ServiceStatusNotice,
    animateIn: Boolean,
    topPadding: Dp,
    onActionClick: (String) -> Unit,
    onDismiss: (String) -> Unit
) {
    val visibleState = remember { MutableTransitionState(!animateIn).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top)
    ) {
        ServiceStatusNoticeBanner(
            notice = notice,
            onActionClick = onActionClick,
            onDismiss = onDismiss,
            modifier = Modifier.padding(top = topPadding)
        )
    }
}

@Composable
private fun ServiceStatusNoticeBanner(
    notice: ServiceStatusNotice,
    onActionClick: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actionUrl = notice.actionUrl
    val severityLabel = stringResource(
        when (notice.severity) {
            Severity.INFO -> R.string.woo_shipping_labels_service_status_severity_info
            Severity.WARNING -> R.string.woo_shipping_labels_service_status_severity_warning
            Severity.ERROR -> R.string.woo_shipping_labels_service_status_severity_error
        }
    )
    WooNoticeBanner(
        title = notice.message,
        modifier = modifier.semantics(mergeDescendants = true) { stateDescription = severityLabel },
        tone = when (notice.severity) {
            Severity.INFO -> WooNoticeBannerTone.Info
            Severity.WARNING -> WooNoticeBannerTone.Warning
            Severity.ERROR -> WooNoticeBannerTone.Error
        },
        actionLabel = actionUrl?.let { stringResource(R.string.woo_shipping_labels_service_status_view_status_page) },
        onActionClick = actionUrl?.let { { onActionClick(it) } },
        dismissContentDescription = if (notice.isDismissible) {
            stringResource(R.string.woo_shipping_labels_service_status_dismiss, notice.message)
        } else {
            null
        },
        onDismissClick = if (notice.isDismissible) {
            { onDismiss(notice.id) }
        } else {
            null
        }
    )
}

@PreviewLightDark
@Composable
private fun ServiceStatusNoticesPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            ServiceStatusNotices(
                notices = listOf(
                    ServiceStatusNotice(
                        id = "1",
                        severity = Severity.ERROR,
                        message = "USPS: API Outage | Status: Investigating",
                        isDismissible = false,
                        actionUrl = "https://www.easypoststatus.com"
                    ),
                    ServiceStatusNotice(
                        id = "2",
                        severity = Severity.WARNING,
                        message = "UPS: Label creation delays | Status: Monitoring",
                        isDismissible = true,
                        actionUrl = null
                    ),
                    ServiceStatusNotice(
                        id = "3",
                        severity = Severity.INFO,
                        message = "DHL Express: Scheduled maintenance tonight. Label purchases and rate quotes may " +
                            "be delayed between 11 PM and 2 AM UTC while the carrier upgrades its systems.",
                        isDismissible = true,
                        actionUrl = "https://www.easypoststatus.com"
                    )
                ),
                onActionClick = {},
                onDismiss = {},
                modifier = Modifier.padding(WooTheme.padding.padding5)
            )
        }
    }
}
