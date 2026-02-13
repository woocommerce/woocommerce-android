package com.woocommerce.android.ui.pushnotifications.connection

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.IdleCircle
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge

@Composable
fun WooPushNotificationsConnectionStepsScreen(
    viewModel: WooPushNotificationsConnectionStepsViewModel,
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        WooPushNotificationsConnectionStepsScreen(
            siteAddress = viewState.siteAddress,
            steps = viewState.steps,
            onCancelClick = onCancelClick,
            onContinueClick = onContinueClick,
            modifier = modifier
        )
    }
}

@Composable
private fun WooPushNotificationsConnectionStepsScreen(
    siteAddress: String,
    steps: List<ConnectionStepUiModel>,
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasInProgressStep = steps.any { it.status == ConnectionStepStatus.IN_PROGRESS }

    Scaffold(
        modifier = modifier,
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCancelClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                WordPressWooBadge()

                Text(
                    text = stringResource(id = R.string.woo_push_notifications_connection_steps_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 32.dp)
                )

                Text(
                    text = annotatedStringRes(
                        R.string.woo_push_notifications_connection_steps_body,
                        siteAddress
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    steps.forEach { step ->
                        ConnectionStepRow(
                            step = step,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            if (!hasInProgressStep) {
                WCColoredButton(
                    onClick = onContinueClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.woo_push_notifications_connection_steps_go_to_my_store))
                }
            }
        }
    }
}

@Composable
private fun ConnectionStepRow(
    step: ConnectionStepUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        ConnectionStepStatusIcon(
            status = step.status
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = step.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (step.status == ConnectionStepStatus.NOT_STARTED) {
                    FontWeight.Normal
                } else {
                    FontWeight.Bold
                },
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            Text(
                text = step.statusMessageRes?.let { stringResource(id = it) }
                    ?: stringResource(id = step.status.defaultTextRes),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (step.status == ConnectionStepStatus.ERROR) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                color = when (step.status) {
                    ConnectionStepStatus.COMPLETE -> colorResource(id = R.color.woo_green_50)
                    ConnectionStepStatus.ERROR -> colorResource(id = R.color.color_error)
                    else -> colorResource(id = R.color.color_on_surface_medium)
                }
            )
        }
    }
}

@Composable
private fun ConnectionStepStatusIcon(
    status: ConnectionStepStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        ConnectionStepStatus.COMPLETE -> {
            Image(
                painter = painterResource(id = R.drawable.ic_progress_circle_complete),
                contentDescription = null,
                modifier = modifier.size(26.dp)
            )
        }

        ConnectionStepStatus.IN_PROGRESS -> {
            CircularProgressIndicator(
                modifier = modifier.size(26.dp),
                color = colorResource(id = R.color.woo_push_notifications_connection_steps_progressbar),
            )
        }

        ConnectionStepStatus.NOT_STARTED -> {
            IdleCircle()
        }

        ConnectionStepStatus.ERROR -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_gridicons_notice),
                contentDescription = null,
                tint = colorResource(id = R.color.color_error),
                modifier = modifier.size(26.dp)
            )
        }
    }
}

data class ConnectionStepUiModel(
    @StringRes val title: Int,
    val status: ConnectionStepStatus,
    @StringRes val statusMessageRes: Int? = null
)

enum class ConnectionStepStatus {
    COMPLETE,
    IN_PROGRESS,
    NOT_STARTED,
    ERROR
}

private val ConnectionStepStatus.defaultTextRes: Int
    @StringRes get() = when (this) {
        ConnectionStepStatus.COMPLETE -> R.string.woo_push_notifications_connection_steps_complete
        ConnectionStepStatus.IN_PROGRESS -> R.string.woo_push_notifications_connection_steps_in_progress
        ConnectionStepStatus.NOT_STARTED -> R.string.woo_push_notifications_connection_steps_not_started
        ConnectionStepStatus.ERROR -> R.string.woo_push_notifications_connection_steps_error
    }

@Composable
@Preview
private fun WooPushNotificationsConnectionStepsScreenPreview() {
    WooThemeWithBackground {
        WooPushNotificationsConnectionStepsScreen(
            siteAddress = "coffeebeans.com",
            steps = listOf(
                ConnectionStepUiModel(
                    title = R.string.woo_push_notifications_connection_steps_step_connect_store,
                    status = ConnectionStepStatus.IN_PROGRESS
                ),
                ConnectionStepUiModel(
                    title = R.string.woo_push_notifications_connection_steps_step_check_plugin_compatibility,
                    status = ConnectionStepStatus.NOT_STARTED
                ),
                ConnectionStepUiModel(
                    title = R.string.woo_push_notifications_connection_steps_step_enable_push_notifications,
                    status = ConnectionStepStatus.NOT_STARTED
                )
            ),
            onCancelClick = {},
            onContinueClick = {}
        )
    }
}
