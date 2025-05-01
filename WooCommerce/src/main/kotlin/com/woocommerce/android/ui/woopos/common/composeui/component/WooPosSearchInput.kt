package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Cancel
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState.Open.Input
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import kotlinx.coroutines.delay

private val INPUT_FIELD_HEIGHT = 56.dp

@Composable
fun WooPosSearchInput(
    modifier: Modifier = Modifier,
    state: WooPosSearchInputState = WooPosSearchInputState.Closed,
    animationDuration: Int = 300,
    onEvent: (WooPosSearchUIEvent) -> Unit = {},
) {
    BackHandler(
        enabled = state is WooPosSearchInputState.Open,
        onBack = { onEvent(WooPosSearchUIEvent.Close) }
    )

    var lastOpenState by rememberSaveable { mutableStateOf<WooPosSearchInputState.Open?>(null) }

    val searchVisibleState = remember { MutableTransitionState(state is WooPosSearchInputState.Closed) }
    val inputVisibleState = remember { MutableTransitionState(state is WooPosSearchInputState.Open) }

    LaunchedEffect(state) {
        searchVisibleState.targetState = state is WooPosSearchInputState.Closed
        inputVisibleState.targetState = state is WooPosSearchInputState.Open

        if (state is WooPosSearchInputState.Open) {
            lastOpenState = state
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(INPUT_FIELD_HEIGHT),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visibleState = inputVisibleState,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = animationDuration / 2,
                    easing = FastOutSlowInEasing,
                )
            ),
        ) {
            lastOpenState?.let {
                SearchInput(
                    state = it,
                    animationDuration = animationDuration.toLong(),
                    onEvent = onEvent,
                )
            }
        }

        AnimatedVisibility(
            visibleState = searchVisibleState,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = animationDuration,
                    delayMillis = animationDuration / 3,
                    easing = FastOutSlowInEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = animationDuration / 3
                )
            )
        ) {
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

@Composable
private fun SearchInput(
    state: WooPosSearchInputState.Open,
    animationDuration: Long,
    onEvent: (WooPosSearchUIEvent) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = { onEvent(WooPosSearchUIEvent.Close) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(
                    R.string.woopos_search_back_content_description
                ),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
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
                .height(INPUT_FIELD_HEIGHT)
                .focusRequester(focusRequester),
            placeholder = {
                WooPosText(
                    text = hint,
                    style = WooPosTypography.BodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = WooPosTheme.colors.onSurfaceVariantLowest,
                )
            },
            textStyle = WooPosTypography.BodyMedium.style
                .copy(fontWeight = FontWeight.Bold),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
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
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            trailingIcon = {
                when {
                    state.isLoading -> {
                        WooPosCircularLoadingIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    textFieldValue.text.isNotEmpty() -> {
                        IconButton(
                            onClick = { onEvent(WooPosSearchUIEvent.Clear) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
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

        LaunchedEffect(query) {
            if (query != textFieldValue.text) {
                textFieldValue = TextFieldValue(
                    text = query,
                    selection = TextRange(state.input.cursorPosition)
                )
            }
        }

        LaunchedEffect(Unit) {
            if (!state.hasAnimationPlayed) {
                delay(animationDuration)
                focusRequester.requestFocus()
                onEvent(WooPosSearchUIEvent.AnimationComplete)
            }
        }
    }
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
