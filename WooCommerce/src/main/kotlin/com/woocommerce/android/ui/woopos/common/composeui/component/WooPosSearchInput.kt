package com.woocommerce.android.ui.woopos.common.composeui.component

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState.Open.Input
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent.SearchIconClicked
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.items.WOO_POS_ITEMS_TOOLBAR_HEIGHT
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent
import com.woocommerce.android.ui.woopos.util.WooPosTestTags
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

@Composable
fun WooPosSearchInput(
    modifier: Modifier = Modifier,
    state: WooPosSearchInputState = WooPosSearchInputState.Closed,
    searchIconBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onEvent: (WooPosSearchUIEvent) -> Unit = {},
) {
    BackHandler(
        enabled = state is WooPosSearchInputState.Open,
        onBack = { onEvent(WooPosSearchUIEvent.Close) }
    )

    Box(
        modifier = modifier.testTag(WooPosTestTags.SEARCH_INPUT),
        contentAlignment = Alignment.CenterEnd
    ) {
        when (state) {
            is WooPosSearchInputState.Open -> {
                SearchInput(
                    state = state,
                    onEvent = onEvent,
                )
            }

            is WooPosSearchInputState.Closed -> {
                WooPosCircularIconButton(
                    icon = ImageVector.vectorResource(R.drawable.ic_search_24dp),
                    contentDescription = stringResource(
                        id = R.string.woopos_search_products,
                    ),
                    backgroundColor = searchIconBackgroundColor,
                    onClick = { onEvent(SearchIconClicked) }
                )
            }
        }
    }
}

@Composable
private fun SearchInput(
    state: WooPosSearchInputState.Open,
    onEvent: (WooPosSearchUIEvent) -> Unit
) {
    val animationDuration = 200L
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration.toInt(), easing = FastOutSlowInEasing),
        label = "borderColorAnimation"
    )

    val colorSurface = MaterialTheme.colorScheme.surface
    val colorPrimary = MaterialTheme.colorScheme.primary

    val animatedFocusedBorderColor = remember(borderColor) {
        lerp(
            colorSurface,
            colorPrimary,
            borderColor
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        WooPosBackButton(
            modifier = Modifier.padding(start = WooPosSpacing.Small.value),
            contentDescription = stringResource(R.string.woopos_search_back_content_description)
        ) { onEvent(WooPosSearchUIEvent.Close) }

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
                .heightIn(min = WOO_POS_ITEMS_TOOLBAR_HEIGHT)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
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
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false
            ),
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
                focusedBorderColor = animatedFocusedBorderColor,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                cursorColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            leadingIcon = {
                if (state.isLoading) {
                    WooPosCircularLoadingIndicator(
                        modifier = Modifier.size(WooPosIconSize.Small.value)
                    )
                } else {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(WooPosIconSize.Medium.value)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_search_24dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            trailingIcon = {
                if (textFieldValue.text.isNotEmpty()) {
                    IconButton(
                        onClick = { onEvent(WooPosSearchUIEvent.Clear) },
                        modifier = Modifier.size(WooPosIconSize.Medium.value)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_clear),
                            contentDescription = stringResource(
                                R.string.woopos_search_clear_content_description
                            ),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
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

        LaunchedEffect(state) {
            if (state.requestFocus) {
                delay(animationDuration)
                focusRequester.requestFocus()
            }
        }
    }
}

@Stable
sealed class WooPosSearchInputState : Parcelable {
    @Parcelize
    data class Open(
        val input: Input,
        val isLoading: Boolean,
        val requestFocus: Boolean = false,
    ) : WooPosSearchInputState() {
        @Parcelize
        sealed class Input(val text: String, open val cursorPosition: Int) : Parcelable {
            @Parcelize
            data class Query(val query: String, override val cursorPosition: Int) : Input(query, cursorPosition)

            @Parcelize
            data class Hint(val hint: String) : Input(hint, 0)
        }
    }

    @Parcelize
    object Closed : WooPosSearchInputState()
}

sealed class WooPosSearchUIEvent {
    object Clear : WooPosSearchUIEvent()
    object SearchIconClicked : WooPosSearchUIEvent()
    data class Search(val query: String, val cursorPosition: Int) : WooPosSearchUIEvent()
    object Close : WooPosSearchUIEvent()
}

fun WooPosSearchUIEvent.toItemsUIEvent(): WooPosItemsUIEvent = when (this) {
    WooPosSearchUIEvent.Clear -> WooPosItemsUIEvent.ClearSearchClicked
    WooPosSearchUIEvent.Close -> WooPosItemsUIEvent.CloseSearchClicked
    WooPosSearchUIEvent.SearchIconClicked -> WooPosItemsUIEvent.SearchIconClicked
    is WooPosSearchUIEvent.Search -> WooPosItemsUIEvent.SearchChanged(
        query = query,
        cursorPosition = cursorPosition,
    )
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
