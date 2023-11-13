package com.woocommerce.android.ui.login.jetpack.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader
import kotlinx.coroutines.delay

private const val FORBIDDEN_ERROR_CODE = 403

@Composable
fun JetpackActivationMainScreen(viewModel: JetpackActivationMainViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationMainScreen(
            viewState = it,
            onCloseClick = viewModel::onCloseClick,
            onContinueClick = viewModel::onContinueClick,
            onGetHelpClick = viewModel::onGetHelpClick,
            onRetryClick = viewModel::onRetryClick
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun JetpackActivationMainScreen(
    viewState: JetpackActivationMainViewModel.ViewState,
    onCloseClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    onGetHelpClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { Toolbar(onNavigationButtonClick = onCloseClick) }
    ) { paddingValues ->
        val transition = updateTransition(targetState = viewState, label = "State Transition")

        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(id = R.dimen.major_100))
                .verticalScroll(rememberScrollState())
        ) {
            JetpackToWooHeader(isError = viewState is JetpackActivationMainViewModel.ViewState.ErrorViewState)

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

            transition.AnimatedContent(
                contentKey = { it is JetpackActivationMainViewModel.ViewState.ErrorViewState },
                transitionSpec = {
                    fadeIn(animationSpec = tween(DefaultDurationMillis, delayMillis = DefaultDurationMillis)) with
                        fadeOut(animationSpec = tween(DefaultDurationMillis))
                },
                modifier = Modifier.weight(1f)
            ) { targetState ->
                when (targetState) {
                    is JetpackActivationMainViewModel.ViewState.ProgressViewState -> ProgressState(
                        viewState = targetState,
                        onContinueClick = onContinueClick
                    )
                    is JetpackActivationMainViewModel.ViewState.ErrorViewState -> ErrorState(
                        viewState = targetState,
                        onGetHelpClick = onGetHelpClick,
                        onRetryClick = onRetryClick,
                        onCancelClick = onCloseClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressState(
    viewState: JetpackActivationMainViewModel.ViewState.ProgressViewState,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        val title = if (viewState.isDone) {
            if (viewState.isJetpackInstalled) R.string.login_jetpack_connection_steps_screen_title_done
            else R.string.login_jetpack_installation_steps_screen_title_done
        } else {
            if (viewState.isJetpackInstalled) R.string.login_jetpack_connection_steps_screen_title
            else R.string.login_jetpack_installation_steps_screen_title
        }
        Text(
            text = stringResource(id = title),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        val subtitle = if (viewState.isDone) {
            R.string.login_jetpack_steps_screen_subtitle_done
        } else {
            R.string.login_jetpack_steps_screen_subtitle
        }
        Text(
            text = annotatedStringRes(stringResId = subtitle, viewState.siteUrl),
            style = MaterialTheme.typography.body1
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        viewState.steps.forEach { step ->
            JetpackActivationStep(
                step,
                viewState.connectionStep,
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.minor_100))
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        var showLoading by remember { mutableStateOf(false) }
        AnimatedVisibility(
            visible = viewState.isDone,
            enter = slideInVertically { fullHeight -> fullHeight },
            exit = slideOutVertically { fullHeight -> fullHeight }
        ) {
            WCColoredButton(
                onClick = {
                    showLoading = true
                    onContinueClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !showLoading
            ) {
                if (showLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)),
                        color = colorResource(id = R.color.color_on_primary_surface),
                    )
                } else {
                    Text(text = stringResource(id = R.string.login_jetpack_installation_go_to_store_button))
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Suppress("ComplexMethod")
private fun AnimatedVisibilityScope.ErrorState(
    viewState: JetpackActivationMainViewModel.ViewState.ErrorViewState,
    onGetHelpClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        val title = when (viewState.stepType) {
            JetpackActivationMainViewModel.StepType.Installation -> R.string.login_jetpack_installation_error_installing
            JetpackActivationMainViewModel.StepType.Activation -> R.string.login_jetpack_installation_error_activating
            JetpackActivationMainViewModel.StepType.Connection -> R.string.login_jetpack_installation_error_authorizing
            else -> null
        }
        title?.let {
            Text(
                text = stringResource(id = it),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        val subtitle = when {
            viewState.errorCode == FORBIDDEN_ERROR_CODE &&
                viewState.stepType == JetpackActivationMainViewModel.StepType.Connection ->
                R.string.login_jetpack_installation_error_connection_permission_message

            viewState.errorCode == FORBIDDEN_ERROR_CODE ->
                R.string.login_jetpack_installation_error_plugin_permission_message

            viewState.stepType == JetpackActivationMainViewModel.StepType.Connection ->
                R.string.login_jetpack_installation_error_connection_message

            else -> R.string.login_jetpack_installation_error_generic_message
        }
        Text(
            text = stringResource(id = subtitle),
            style = MaterialTheme.typography.body1
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))

        val suggestion = when {
            viewState.errorCode == FORBIDDEN_ERROR_CODE ->
                R.string.login_jetpack_installation_error_forbidden_suggestion

            viewState.stepType == JetpackActivationMainViewModel.StepType.Connection ->
                R.string.login_jetpack_installation_error_connection_suggestion

            else -> R.string.login_jetpack_installation_error_generic_suggestion
        }
        Text(
            text = stringResource(id = suggestion),
            style = MaterialTheme.typography.body2
        )
        viewState.errorCode?.let {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
            Text(
                text = stringResource(id = R.string.login_jetpack_installation_error_code_template, it),
                style = MaterialTheme.typography.caption,
                color = colorResource(id = R.color.color_on_surface_medium),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
        WCTextButton(
            onClick = onGetHelpClick,
            colors = ButtonDefaults.textButtonColors(contentColor = colorResource(id = R.color.color_accent_3)),
            contentPadding = PaddingValues(vertical = dimensionResource(id = R.dimen.minor_100))
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
                modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_help_24dp), contentDescription = null)
                Text(text = stringResource(id = R.string.login_jetpack_installation_get_support))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        val buttonsModifier = Modifier
            .fillMaxWidth()
            .animateEnterExit(
                enter = slideInVertically(animationSpec = tween(delayMillis = DefaultDurationMillis)) { fullHeight ->
                    fullHeight
                },
                exit = slideOutVertically { fullHeight -> fullHeight }
            )
        if (viewState.errorCode != FORBIDDEN_ERROR_CODE) {
            val retryButton = when (viewState.stepType) {
                JetpackActivationMainViewModel.StepType.Installation ->
                    R.string.login_jetpack_installation_retry_installing
                JetpackActivationMainViewModel.StepType.Activation ->
                    R.string.login_jetpack_installation_retry_activating
                JetpackActivationMainViewModel.StepType.Connection ->
                    R.string.login_jetpack_installation_retry_authorizing
                else -> null
            }
            retryButton?.let {
                WCColoredButton(onClick = onRetryClick, modifier = buttonsModifier) {
                    Text(text = stringResource(id = it))
                }
            }
        }
        WCOutlinedButton(onClick = onCancelClick, modifier = buttonsModifier) {
            Text(text = stringResource(id = R.string.login_jetpack_installation_cancel))
        }
    }
}

@Composable
private fun JetpackActivationStep(
    step: JetpackActivationMainViewModel.Step,
    connectionStep: JetpackActivationMainViewModel.ConnectionStep,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.defaultMinSize(minHeight = dimensionResource(id = R.dimen.major_300))
    ) {
        val indicatorModifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_60))
        when (step.state) {
            JetpackActivationMainViewModel.StepState.Idle -> {
                IdleCircle(indicatorModifier)
            }
            JetpackActivationMainViewModel.StepState.Ongoing -> {
                CircularProgressIndicator(indicatorModifier)
            }
            JetpackActivationMainViewModel.StepState.Success -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_progress_circle_complete),
                    contentDescription = null,
                    modifier = indicatorModifier
                )
            }
            is JetpackActivationMainViewModel.StepState.Error -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gridicons_notice),
                    contentDescription = null,
                    tint = colorResource(id = R.color.color_error),
                    modifier = indicatorModifier
                )
            }
        }

        Column {
            val isIdle = step.state == JetpackActivationMainViewModel.StepState.Idle
            Text(
                text = stringResource(id = step.type.title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = if (isIdle) FontWeight.Normal
                else FontWeight.Bold,
                color = colorResource(
                    id = if (isIdle) R.color.color_on_surface_medium
                    else R.color.color_on_surface
                )
            )

            if (step.state is JetpackActivationMainViewModel.StepState.Error) {
                Text(
                    text = stringResource(id = R.string.login_jetpack_installation_error),
                    color = colorResource(id = R.color.color_error),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (step.type == JetpackActivationMainViewModel.StepType.Connection) {
                ConnectionStepHint(connectionStep)
            }
        }
    }
}

@Composable
private fun ConnectionStepHint(connectionStep: JetpackActivationMainViewModel.ConnectionStep) {
    // Show no hint for PreConnection step
    if (connectionStep != JetpackActivationMainViewModel.ConnectionStep.PreConnection) {
        val (text, color) = when (connectionStep) {
            JetpackActivationMainViewModel.ConnectionStep.Validation ->
                Pair(
                    R.string.login_jetpack_steps_authorizing_validation,
                    R.color.color_on_surface_medium
                )
            else ->
                Pair(
                    R.string.login_jetpack_steps_authorizing_done,
                    R.color.woo_green_50
                )
        }
        Text(
            text = stringResource(id = text),
            color = colorResource(id = color),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun IdleCircle(modifier: Modifier = Modifier) {
    val color = colorResource(id = R.color.color_on_surface_medium)
    val stroke = with(LocalDensity.current) {
        Stroke(width = dimensionResource(id = R.dimen.minor_25).toPx(), cap = StrokeCap.Round)
    }
    Canvas(modifier = modifier) {
        drawCircle(
            color = color,
            radius = (size.minDimension - stroke.width) / 2,
            style = stroke
        )
    }
}

private val JetpackActivationMainViewModel.StepType.title
    get() = when (this) {
        JetpackActivationMainViewModel.StepType.Installation -> R.string.login_jetpack_steps_installing
        JetpackActivationMainViewModel.StepType.Activation -> R.string.login_jetpack_steps_activating
        JetpackActivationMainViewModel.StepType.Connection -> R.string.login_jetpack_steps_authorizing
        JetpackActivationMainViewModel.StepType.Done -> R.string.login_jetpack_steps_done
    }

@Suppress("MagicNumber")
private class ViewStatePreviewProvider : PreviewParameterProvider<JetpackActivationMainViewModel.ViewState> {
    override val values: Sequence<JetpackActivationMainViewModel.ViewState>
        get() = sequenceOf(
            JetpackActivationMainViewModel.ViewState.ProgressViewState(
                siteUrl = "reallyniceshirts.com",
                isJetpackInstalled = false,
                steps = listOf(
                    JetpackActivationMainViewModel.Step(
                        type = JetpackActivationMainViewModel.StepType.Installation,
                        state = JetpackActivationMainViewModel.StepState.Success
                    ),
                    JetpackActivationMainViewModel.Step(
                        type = JetpackActivationMainViewModel.StepType.Activation,
                        state = JetpackActivationMainViewModel.StepState.Ongoing
                    ),
                    JetpackActivationMainViewModel.Step(
                        type = JetpackActivationMainViewModel.StepType.Activation,
                        state = JetpackActivationMainViewModel.StepState.Error(403)
                    ),
                    JetpackActivationMainViewModel.Step(
                        type = JetpackActivationMainViewModel.StepType.Connection,
                        state = JetpackActivationMainViewModel.StepState.Idle
                    ),
                    JetpackActivationMainViewModel.Step(
                        type = JetpackActivationMainViewModel.StepType.Done,
                        state = JetpackActivationMainViewModel.StepState.Idle
                    )
                ),
                connectionStep = JetpackActivationMainViewModel.ConnectionStep.PreConnection
            ),
            JetpackActivationMainViewModel.ViewState.ErrorViewState(
                stepType = JetpackActivationMainViewModel.StepType.Installation,
                errorCode = 503
            )
        )
}

@Composable
@Preview
private fun JetpackActivationPreview(
    @PreviewParameter(provider = ViewStatePreviewProvider::class) state: JetpackActivationMainViewModel.ViewState
) {
    WooThemeWithBackground {
        JetpackActivationMainScreen(
            viewState = state
        )
    }
}

@Composable
@Preview
private fun JetpackActivationProgressToErrorPreview() {
    val defaultState = JetpackActivationMainViewModel.ViewState.ProgressViewState(
        siteUrl = "reallyniceshirts.com",
        isJetpackInstalled = false,
        steps = listOf(
            JetpackActivationMainViewModel.Step(
                type = JetpackActivationMainViewModel.StepType.Installation,
                state = JetpackActivationMainViewModel.StepState.Success
            ),
            JetpackActivationMainViewModel.Step(
                type = JetpackActivationMainViewModel.StepType.Activation,
                state = JetpackActivationMainViewModel.StepState.Error(503)
            ),
            JetpackActivationMainViewModel.Step(
                type = JetpackActivationMainViewModel.StepType.Connection,
                state = JetpackActivationMainViewModel.StepState.Idle
            ),
            JetpackActivationMainViewModel.Step(
                type = JetpackActivationMainViewModel.StepType.Done,
                state = JetpackActivationMainViewModel.StepState.Idle
            )
        ),
        connectionStep = JetpackActivationMainViewModel.ConnectionStep.PreConnection
    )
    val errorState = JetpackActivationMainViewModel.ViewState.ErrorViewState(
        stepType = JetpackActivationMainViewModel.StepType.Activation,
        errorCode = 503
    )

    var state: JetpackActivationMainViewModel.ViewState by remember { mutableStateOf(defaultState) }
    var retryTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(retryTrigger) {
        delay(1000)
        state = errorState
    }

    WooThemeWithBackground {
        JetpackActivationMainScreen(
            viewState = state,
            onRetryClick = {
                state = defaultState
                retryTrigger = !retryTrigger
            }
        )
    }
}
