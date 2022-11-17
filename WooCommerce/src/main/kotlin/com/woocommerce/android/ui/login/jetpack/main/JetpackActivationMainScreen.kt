package com.woocommerce.android.ui.login.jetpack.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader

@Composable
fun JetpackActivationMainScreen(viewModel: JetpackActivationMainViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationMainScreen(
            viewState = it,
            onCloseClick = viewModel::onCloseClick
        )
    }
}

@Composable
fun JetpackActivationMainScreen(
    viewState: JetpackActivationMainViewModel.ViewState,
    onCloseClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { Toolbar(onCloseClick) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(id = R.dimen.major_100))
                .verticalScroll(rememberScrollState())
        ) {
            JetpackToWooHeader()
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            Text(
                text = stringResource(
                    id = if (viewState.isJetpackInstalled) R.string.login_jetpack_connection_steps_screen_title
                    else R.string.login_jetpack_installation_steps_screen_title
                ),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            Text(
                text = annotatedStringRes(
                    stringResId = R.string.login_jetpack_steps_screen_subtitle,
                    viewState.siteUrl
                ),
                style = MaterialTheme.typography.body1
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            viewState.steps.forEach { step ->
                JetpackActivationStep(
                    step,
                    modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.minor_100))
                )
            }
        }
    }
}

@Composable
private fun JetpackActivationStep(step: JetpackActivationMainViewModel.Step, modifier: Modifier = Modifier) {
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
                    text = step.state.code?.let {
                        stringResource(id = R.string.login_jetpack_installation_error_code_template, it)
                    } ?: "",
                    color = colorResource(id = R.color.color_error),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (step.type == JetpackActivationMainViewModel.StepType.Connection) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colorResource(id = R.color.woo_orange_50),
                        modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_40))
                    )
                    Text(
                        text = stringResource(id = R.string.login_jetpack_steps_authorizing_hint),
                        color = colorResource(R.color.woo_orange_50),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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

@Composable
private fun Toolbar(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    Filled.Clear,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
@Preview
private fun JetpackActivationMainScreenPreview() {
    WooThemeWithBackground {
        JetpackActivationMainScreen(
            viewState = JetpackActivationMainViewModel.ViewState(
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
                )
            )
        )
    }
}
