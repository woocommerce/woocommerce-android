package com.woocommerce.android.ui.pushnotifications.connection

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.IdleCircle
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepState
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepType
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.ViewState

@Composable
fun WooPushNotificationsConnectionStepsScreen(
    viewModel: WooPushNotificationsConnectionStepsViewModel
) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        WooPushNotificationsConnectionStepsScreen(
            viewState = viewState,
            onCloseClick = viewModel::onCloseClick,
            onGoToStoreClick = viewModel::onGoToStoreClick,
            onRetryClick = viewModel::onRetryClick,
            onContactSupportClick = viewModel::onContactSupportClick,
            onUpdatePluginClick = viewModel::onUpdatePluginClick
        )
    }
}

@Composable
private fun WooPushNotificationsConnectionStepsScreen(
    viewState: ViewState,
    onCloseClick: () -> Unit,
    onGoToStoreClick: () -> Unit,
    onRetryClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    onUpdatePluginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCloseClick,
                actions = {
                    if (viewState.isError) {
                        IconButton(onClick = onContactSupportClick) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_help_24dp),
                                contentDescription = stringResource(id = R.string.help),
                            )
                        }
                    }
                }
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
                    text = stringResource(id = viewState.titleRes),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 32.dp)
                )

                Text(
                    text = annotatedStringRes(
                        viewState.bodyRes,
                        viewState.siteAddress
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    viewState.steps.forEach { step ->
                        ConnectionStepRow(
                            step = step,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = viewState.isError,
                enter = fadeIn(animationSpec = tween(DefaultDurationMillis)),
                exit = fadeOut(animationSpec = tween(DefaultDurationMillis))
            ) {
                if (viewState.isPluginUpdateRequired) {
                    Column {
                        WCColoredButton(
                            onClick = onUpdatePluginClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.woo_push_notifications_connection_steps_update_plugin
                                )
                            )
                        }
                        WCOutlinedButton(
                            onClick = onRetryClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.woo_push_notifications_connection_steps_retry
                                )
                            )
                        }
                    }
                } else {
                    WCColoredButton(
                        onClick = onRetryClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.woo_push_notifications_connection_steps_retry
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = viewState.isDone,
                enter = slideInVertically { fullHeight -> fullHeight },
                exit = slideOutVertically { fullHeight -> fullHeight }
            ) {
                WCColoredButton(
                    onClick = onGoToStoreClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.woo_push_notifications_connection_steps_go_to_my_store
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStepRow(
    step: WooPushNotificationsConnectionStepsViewModel.Step,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        ConnectionStepStatusIcon(state = step.state)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = step.type.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (step.state == StepState.Idle) {
                    FontWeight.Normal
                } else {
                    FontWeight.Bold
                },
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            Text(
                text = step.state.statusText,
                style = MaterialTheme.typography.bodySmall,
                color = when (step.state) {
                    StepState.Success -> colorResource(id = R.color.woo_green_50)
                    is StepState.Error -> MaterialTheme.colorScheme.error
                    else -> colorResource(id = R.color.color_on_surface_medium)
                }
            )
        }
    }
}

@Composable
private fun ConnectionStepStatusIcon(
    state: StepState,
    modifier: Modifier = Modifier
) {
    when (state) {
        StepState.Success -> {
            Image(
                painter = painterResource(id = R.drawable.ic_progress_circle_complete),
                contentDescription = null,
                modifier = modifier.size(26.dp)
            )
        }

        StepState.Ongoing -> {
            CircularProgressIndicator(
                modifier = modifier.size(26.dp),
                color = colorResource(id = R.color.woo_push_notifications_connection_steps_progressbar),
            )
        }

        StepState.Idle -> {
            IdleCircle()
        }

        is StepState.Error -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_gridicons_notice),
                contentDescription = null,
                tint = colorResource(id = R.color.color_error),
                modifier = modifier.size(26.dp)
            )
        }
    }
}

private val StepType.title: Int
    @StringRes get() = when (this) {
        StepType.ConnectStore ->
            R.string.woo_push_notifications_connection_steps_step_connect_store

        StepType.CheckPluginCompatibility ->
            R.string.woo_push_notifications_connection_steps_step_check_plugin_compatibility

        StepType.EnablePushNotifications ->
            R.string.woo_push_notifications_connection_steps_step_enable_push_notifications
    }

private val StepState.statusText: String
    @Composable
    get() = when (this) {
        StepState.Success -> stringResource(R.string.woo_push_notifications_connection_steps_complete)
        StepState.Ongoing -> stringResource(R.string.woo_push_notifications_connection_steps_in_progress)
        StepState.Idle -> stringResource(R.string.woo_push_notifications_connection_steps_not_started)
        is StepState.Error -> errorMessage.getText()
    }

@Composable
@Preview
private fun WooPushNotificationsConnectionStepsPreview() {
    WooThemeWithBackground {
        WooPushNotificationsConnectionStepsScreen(
            viewState = ViewState(
                titleRes = R.string.woo_push_notifications_connection_steps_title_connect,
                bodyRes = R.string.woo_push_notifications_connection_steps_body_connect,
                siteAddress = "coffeebeans.com",
                steps = listOf(
                    WooPushNotificationsConnectionStepsViewModel.Step(
                        type = StepType.CheckPluginCompatibility,
                        state = StepState.Ongoing
                    ),
                    WooPushNotificationsConnectionStepsViewModel.Step(
                        type = StepType.ConnectStore,
                        state = StepState.Idle
                    ),
                    WooPushNotificationsConnectionStepsViewModel.Step(
                        type = StepType.EnablePushNotifications,
                        state = StepState.Idle
                    )
                )
            ),
            onCloseClick = {},
            onGoToStoreClick = {},
            onRetryClick = {},
            onContactSupportClick = {},
            onUpdatePluginClick = {}
        )
    }
}

@Composable
@Preview
private fun WooPushNotificationsConnectionStepsPreviewError() {
    WooThemeWithBackground {
        WooPushNotificationsConnectionStepsScreen(
            viewState = ViewState(
                titleRes = R.string.woo_push_notifications_connection_steps_title_connect,
                bodyRes = R.string.woo_push_notifications_connection_steps_body_connect,
                siteAddress = "coffeebeans.com",
                steps = listOf(
                    WooPushNotificationsConnectionStepsViewModel.Step(
                        type = StepType.CheckPluginCompatibility,
                        state = StepState.Success
                    ),
                    WooPushNotificationsConnectionStepsViewModel.Step(
                        type = StepType.ConnectStore,
                        state = StepState.Error(UiString.UiStringText("Error connecting to store"))
                    ),
                    WooPushNotificationsConnectionStepsViewModel.Step(
                        type = StepType.EnablePushNotifications,
                        state = StepState.Idle
                    )
                )
            ),
            onCloseClick = {},
            onGoToStoreClick = {},
            onRetryClick = {},
            onContactSupportClick = {},
            onUpdatePluginClick = {}
        )
    }
}
