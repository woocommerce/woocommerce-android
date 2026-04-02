package com.woocommerce.android.ui.reviews.ai

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.AlertDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.reviews.ai.AIReviewReplyViewModel.GenerationState
import kotlinx.coroutines.delay

@Composable
fun AIReviewReplyScreen(viewModel: AIReviewReplyViewModel) {
    val state by viewModel.viewState.collectAsState()
    AIReviewReplyScreen(
        state = state,
        onTextChanged = viewModel::onTextChanged,
        onDonePressed = viewModel::onDonePressed,
        onBackPressed = viewModel::onBackPressed,
        onSuggestionSelected = viewModel::onSuggestionSelected,
        onDismissOverlay = viewModel::onDismissOverlay,
        onRetryClicked = viewModel::onRetryClicked,
        onAIButtonClicked = viewModel::onAIButtonClicked,
        onConfirmAIGeneration = viewModel::onConfirmAIGeneration,
        onDismissConfirmationDialog = viewModel::onDismissConfirmationDialog,
        onUpgradeClicked = viewModel::onUpgradeClicked
    )
}

@Suppress("LongParameterList")
@Composable
fun AIReviewReplyScreen(
    state: AIReviewReplyViewModel.ViewState,
    onTextChanged: (String) -> Unit,
    onDonePressed: () -> Unit,
    onBackPressed: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onDismissOverlay: () -> Unit,
    onRetryClicked: () -> Unit,
    onAIButtonClicked: () -> Unit,
    onConfirmAIGeneration: () -> Unit,
    onDismissConfirmationDialog: () -> Unit,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackPressed() }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Toolbar(
                    title = stringResource(R.string.review_reply_screen_title),
                    onNavigationButtonClick = onBackPressed,
                    navigationIcon = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                    onActionButtonClick = onDonePressed,
                    actionButtonText = stringResource(R.string.done)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                TextField(
                    value = state.replyText,
                    onValueChange = onTextChanged,
                    placeholder = { Text(stringResource(R.string.review_reply_hint)) },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = MaterialTheme.colors.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                if (!state.showOverlay) {
                    WCColoredButton(
                        onClick = onAIButtonClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.major_100)),
                        text = stringResource(R.string.review_ai_reply_button_text),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_ai),
                                contentDescription = null,
                                tint = colorResource(R.color.woo_white)
                            )
                        }
                    )
                }
            }
        }

        if (state.showConfirmationDialog) {
            ConfirmationDialog(
                onConfirm = onConfirmAIGeneration,
                onDismiss = onDismissConfirmationDialog
            )
        }

        if (state.showOverlay) {
            Dialog(
                onDismissRequest = onDismissOverlay,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                val window = (LocalView.current.parent as? DialogWindowProvider)?.window
                SideEffect {
                    window?.let {
                        WindowInsetsControllerCompat(it, it.decorView)
                            .isAppearanceLightStatusBars = false
                    }
                }
                SuggestionsOverlay(
                    generationState = state.generationState,
                    suggestions = state.suggestions,
                    onSuggestionSelected = onSuggestionSelected,
                    onDismissOverlay = onDismissOverlay,
                    onRetryClicked = onRetryClicked,
                    onUpgradeClicked = onUpgradeClicked
                )
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.review_ai_reply_confirm_title)) },
        text = { Text(stringResource(R.string.review_ai_reply_confirm_message)) },
        confirmButton = {
            WCTextButton(
                text = stringResource(R.string.review_ai_reply_confirm_generate),
                onClick = onConfirm
            )
        },
        dismissButton = {
            WCTextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss
            )
        },
        neutralButton = {}
    )
}

@Composable
@Suppress("LongParameterList")
private fun SuggestionsOverlay(
    generationState: GenerationState,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    onDismissOverlay: () -> Unit,
    onRetryClicked: () -> Unit,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismissOverlay),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(durationMillis = 350)
            ) + fadeIn(animationSpec = tween(durationMillis = 350)),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(dimensionResource(R.dimen.major_100)),
                elevation = 12.dp,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.major_100))
            ) {
                Column(modifier = Modifier.animateContentSize(animationSpec = tween(durationMillis = 300))) {
                    OverlayHeader()

                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.major_100)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (generationState) {
                            is GenerationState.Generating -> GeneratingContent()
                            is GenerationState.Generated -> GeneratedContent(
                                suggestions = suggestions,
                                onSuggestionSelected = onSuggestionSelected
                            )
                            is GenerationState.Failed -> FailedContent(onRetryClicked = onRetryClicked)
                            is GenerationState.QuotaExceeded -> QuotaExceededContent(
                                hasUpgradeUrl = generationState.upgradeUrl != null,
                                onUpgradeClicked = onUpgradeClicked
                            )
                            is GenerationState.Idle -> Unit
                        }

                        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_50)))

                        WCTextButton(onClick = onDismissOverlay) {
                            Text(
                                text = stringResource(
                                    if (generationState is GenerationState.QuotaExceeded) {
                                        R.string.review_ai_reply_limit_dismiss
                                    } else {
                                        R.string.review_ai_reply_write_manually
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        colorResource(R.color.woo_purple_40).copy(alpha = 0.15f),
                        colorResource(R.color.woo_purple_80).copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
            .padding(dimensionResource(R.dimen.major_100))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ai),
                contentDescription = null,
                tint = colorResource(R.color.woo_purple_40),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.minor_100)))
            Text(
                text = stringResource(R.string.review_ai_reply_overlay_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GeneratingContent(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.major_100))
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ai),
            contentDescription = null,
            tint = colorResource(R.color.woo_purple_40),
            modifier = Modifier
                .size(32.dp)
                .rotate(rotation)
        )
        Text(
            text = stringResource(R.string.review_ai_reply_generating),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun GeneratedContent(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            val alpha = remember { Animatable(0f) }
            LaunchedEffect(suggestion) {
                delay(index * 100L)
                alpha.animateTo(1f, animationSpec = tween(durationMillis = 300))
            }
            SuggestionCard(
                text = suggestion,
                onClick = { onSuggestionSelected(suggestion) },
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colorResource(R.color.woo_purple_40).copy(alpha = 0.2f),
                shape = RoundedCornerShape(dimensionResource(R.dimen.minor_100))
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dimensionResource(R.dimen.minor_100)),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}

@Composable
private fun FailedContent(
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.major_100))
    ) {
        Text(
            text = stringResource(R.string.review_ai_reply_failed),
            style = MaterialTheme.typography.body1
        )
        WCColoredButton(onClick = onRetryClicked) {
            Text(text = stringResource(R.string.review_ai_reply_retry))
        }
    }
}

@Composable
private fun QuotaExceededContent(
    hasUpgradeUrl: Boolean,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ai),
            contentDescription = null,
            tint = colorResource(R.color.woo_purple_40),
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = stringResource(R.string.review_ai_reply_limit_title),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.review_ai_reply_limit_message),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        if (hasUpgradeUrl) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_50)))

            WCColoredButton(
                onClick = onUpgradeClicked,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.review_ai_reply_limit_upgrade),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_ai),
                        contentDescription = null,
                        tint = colorResource(R.color.woo_white)
                    )
                }
            )
        }
    }
}
