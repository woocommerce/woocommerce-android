package com.woocommerce.android.ui.woopos.common.composeui.component

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState.Open.Input
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import kotlinx.coroutines.delay

private val BUTTON_SIZE = 40.dp
private val INPUT_FIELD_HEIGHT = 56.dp
private const val ANIMATION_TIME = 300L

@Composable
fun WooPosSearchInput(
    modifier: Modifier = Modifier,
    state: WooPosSearchInputState = WooPosSearchInputState.Closed,
    onEvent: (WooPosSearchUIEvent) -> Unit = {},
) {
    BackHandler(
        enabled = state is WooPosSearchInputState.Open,
        onBack = { onEvent(WooPosSearchUIEvent.Close) }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(INPUT_FIELD_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        when (state) {
            is WooPosSearchInputState.Open -> {
                AnimatedSearchInput(
                    state = state,
                    onEvent = onEvent,
                )
            }

            WooPosSearchInputState.Closed -> {
                Spacer(modifier = Modifier.weight(1f))
                WooPosCircularIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = stringResource(
                        id = R.string.woopos_search_products,
                    ),
                    onClick = { onEvent(WooPosSearchUIEvent.Search("", 0)) }
                )
            }
        }
    }
}
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AnimatedSearchInput(
    state: WooPosSearchInputState.Open,
    onEvent: (WooPosSearchUIEvent) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val focusRequester = remember { FocusRequester() }
        var isExpanded by remember { mutableStateOf(state.hasAnimationPlayed) }
        var isClosing by remember { mutableStateOf(false) }

        val transition = updateTransition(
            targetState = if (isClosing) false else isExpanded,
            label = "searchTransition"
        )

        val width = animateSearchWidth(transition, maxWidth)
        val height = animateSearchHeight(transition)
        val cornerRadius = animateCornerRadius(transition)
        val iconAlpha = animateIconAlpha(transition, isClosing)
        val backButtonAlpha = animateIconAlpha(transition, isClosing)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(width)
        ) {
            IconButton(
                onClick = {
                    isClosing = true
                },
                modifier = Modifier.alpha(backButtonAlpha)
            ) {
                Icon(
                    modifier = Modifier.size(28.dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(
                        R.string.woopos_search_back_content_description
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))

            val (hint, query) = when (state.input) {
                is Input.Query -> "" to state.input.text
                is Input.Hint -> state.input.text to ""
            }

            var textFieldValue by remember {
                mutableStateOf(
                    TextFieldValue(
                        text = query,
                        selection = TextRange(state.input.cursorPosition)
                    )
                )
            }

            LaunchedEffect(query) {
                if (query != textFieldValue.text) {
                    textFieldValue = TextFieldValue(
                        text = query,
                        selection = TextRange(state.input.cursorPosition)
                    )
                }
            }

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue: TextFieldValue ->
                    textFieldValue = newValue
                    onEvent(
                        WooPosSearchUIEvent.Search(
                            newValue.text,
                            newValue.selection.start
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .focusRequester(focusRequester),
                placeholder = {
                    WooPosText(
                        text = hint,
                        modifier = Modifier.alpha(iconAlpha),
                        style = WooPosTypography.BodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                textStyle = WooPosTypography.BodyMedium.style
                    .copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(cornerRadius),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onEvent(
                            WooPosSearchUIEvent.Search(
                                textFieldValue.text,
                                textFieldValue.selection.start
                            )
                        )
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .alpha(iconAlpha)
                            .size(26.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
                trailingIcon = {
                    when {
                        state.isLoading -> {
                            WooPosCircularLoadingIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .alpha(iconAlpha)
                            )
                        }

                        textFieldValue.text.isNotEmpty() -> {
                            IconButton(
                                onClick = { onEvent(WooPosSearchUIEvent.Clear) },
                                modifier = Modifier
                                    .alpha(iconAlpha)
                                    .size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(
                                        R.string.woopos_search_clear_content_description
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                },
            )
        }

        LaunchedEffect(Unit) {
            if (!state.hasAnimationPlayed) {
                isExpanded = true
                delay(ANIMATION_TIME)
                focusRequester.requestFocus()
                onEvent(WooPosSearchUIEvent.AnimationComplete)
            }
        }

        LaunchedEffect(isClosing) {
            if (isClosing) {
                delay(ANIMATION_TIME)
                onEvent(WooPosSearchUIEvent.Close)
            }
        }
    }
}

@Composable
private fun animateSearchWidth(
    transition: Transition<Boolean>,
    maxWidth: Dp,
): Dp {
    val width by transition.animateDp(
        label = "width",
        transitionSpec = {
            tween(
                durationMillis = ANIMATION_TIME.toInt(),
                easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
            )
        }
    ) { expanded ->
        if (expanded) maxWidth else BUTTON_SIZE
    }
    return width
}

@Composable
private fun animateSearchHeight(transition: Transition<Boolean>): Dp {
    val height by transition.animateDp(
        label = "height",
        transitionSpec = {
            tween(
                durationMillis = ANIMATION_TIME.toInt(),
                easing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
            )
        }
    ) { expanded ->
        if (expanded) INPUT_FIELD_HEIGHT else BUTTON_SIZE
    }
    return height
}

@Composable
private fun animateCornerRadius(transition: Transition<Boolean>): Dp {
    val cornerRadius by transition.animateDp(
        label = "cornerRadius",
        transitionSpec = {
            tween(
                durationMillis = ANIMATION_TIME.toInt(),
                easing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
            )
        }
    ) { expanded ->
        if (expanded) WooPosCornerRadius.Medium.value else BUTTON_SIZE / 2
    }
    return cornerRadius
}

@Composable
private fun animateIconAlpha(
    transition: Transition<Boolean>,
    isClosing: Boolean
): Float {
    val iconAlpha by transition.animateFloat(
        label = "iconAlpha",
        transitionSpec = {
            tween(
                durationMillis = (ANIMATION_TIME * 0.7).toInt(),
                easing = FastOutSlowInEasing,
                delayMillis = if (isClosing) 0 else (ANIMATION_TIME * 0.3).toInt()
            )
        }
    ) { expanded ->
        if (expanded) 1f else 0f
    }
    return iconAlpha
}

sealed class WooPosSearchInputState {
    data class Open(
        val input: Input,
        val isLoading: Boolean,
        val hasAnimationPlayed: Boolean = false,
    ) : WooPosSearchInputState() {
        sealed class Input(val text: String, open val cursorPosition: Int) {
            data class Query(val query: String, override val cursorPosition: Int) : Input(query, cursorPosition)
            data class Hint(val hint: String) : Input(hint, 0)
        }
    }

    object Closed : WooPosSearchInputState()
}

sealed class WooPosSearchUIEvent {
    object Clear : WooPosSearchUIEvent()
    data class Search(val query: String, val cursorPosition: Int) : WooPosSearchUIEvent()
    object Close : WooPosSearchUIEvent()
    object AnimationComplete : WooPosSearchUIEvent()
}

@WooPosPreview
@Composable
fun WooPosSearchInputOpenSearchPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Small.value)
        ) {
            WooPosSearchInput(
                state = WooPosSearchInputState.Open(
                    Input.Query(
                        "Search products...",
                        cursorPosition = 0
                    ),
                    isLoading = false,
                    hasAnimationPlayed = true
                ),
                onEvent = {}
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosSearchInputClosedPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Small.value)
        ) {
            WooPosSearchInput(
                state = WooPosSearchInputState.Closed,
                onEvent = {}
            )
        }
    }
}
