package com.woocommerce.android.ui.products.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadge
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeTone
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooCell
import com.woocommerce.android.ui.compose.designsystem.component.WooCellTrailingAffordance
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledTonalButton
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.component.WooSwitch
import com.woocommerce.android.ui.compose.designsystem.icons.AngleRight
import com.woocommerce.android.ui.compose.designsystem.icons.Star
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
internal fun ProductDetailRow(row: ProductDetailRowUiModel) {
    val modifier = Modifier.testTag(ProductDetailTestTags.row(row.key))
    when (row) {
        is ProductDetailRowUiModel.Divider -> WooDivider(modifier)
        is ProductDetailRowUiModel.Property -> ProductDetailPropertyRow(row, modifier)
        is ProductDetailRowUiModel.ComplexProperty -> ProductDetailComplexPropertyRow(row, modifier)
        is ProductDetailRowUiModel.Rating -> ProductDetailRatingRow(row, modifier)
        is ProductDetailRowUiModel.Editable -> ProductDetailEditableRow(row, modifier)
        is ProductDetailRowUiModel.PropertyGroup -> ProductDetailPropertyGroupRow(row, modifier)
        is ProductDetailRowUiModel.Link -> ProductDetailLinkRow(row, modifier)
        is ProductDetailRowUiModel.Button -> ProductDetailButtonRow(row, modifier)
        is ProductDetailRowUiModel.Switch -> ProductDetailSwitchRow(row, modifier)
        is ProductDetailRowUiModel.Warning -> WooNoticeBanner(
            title = row.content,
            tone = WooNoticeBannerTone.Warning,
            modifier = modifier.padding(WooTheme.padding.padding5),
        )
    }
}

@Composable
private fun ProductDetailPropertyRow(
    row: ProductDetailRowUiModel.Property,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_ROW_HEIGHT)
                .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding4),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(row.title),
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.emphasized,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = row.value,
                color = WooTheme.colors.surface.onVariant,
                style = WooTheme.text.bodyLarge.regular,
            )
        }
        ProductDetailOptionalDivider(
            show = row.showDivider,
        )
    }
}

@Composable
private fun ProductDetailComplexPropertyRow(
    row: ProductDetailRowUiModel.ComplexProperty,
    modifier: Modifier,
) {
    val title = row.title?.let { stringResource(it) }.orEmpty()
    val value = AnnotatedString.fromHtml(row.value)
    Column {
        ProductDetailPropertyCell(
            title = if (row.showTitle && row.title != null) title else value.text,
            description = value.takeIf { row.showTitle && row.title != null },
            icon = row.icon,
            maxLines = row.maxLines,
            onClick = row.onClick,
            modifier = modifier,
        )
        ProductDetailOptionalDivider(
            show = row.showDivider,
            hasLeadingIcon = row.icon != null,
        )
    }
}

@Composable
private fun ProductDetailRatingRow(
    row: ProductDetailRowUiModel.Rating,
    modifier: Modifier,
) {
    Column {
        ProductDetailPropertyCell(
            title = stringResource(row.title),
            description = null,
            icon = row.icon,
            onClick = row.onClick,
            modifier = modifier,
            additionalContent = {
                ProductDetailRatingSummary(
                    rating = row.rating,
                    reviewCount = row.value,
                )
            },
        )
        ProductDetailOptionalDivider(
            show = row.showDivider,
            hasLeadingIcon = true,
        )
    }
}

@Composable
private fun ProductDetailEditableRow(
    row: ProductDetailRowUiModel.Editable,
    modifier: Modifier,
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = MIN_EDITABLE_HEIGHT)
                .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding3),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductDetailEditableField(row, Modifier.weight(1f))
            ProductDetailEditableBadge(row)
        }
        WooDivider()
    }
}

@Composable
private fun ProductDetailEditableField(
    row: ProductDetailRowUiModel.Editable,
    modifier: Modifier,
) {
    val callback by rememberUpdatedState(row.onTextChanged)
    val focusRequester = remember(row.key) { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember(row.key) { mutableStateOf(false) }
    var restoreFocus by rememberSaveable(row.key) { mutableStateOf(false) }
    var hasEditedWhileFocused by rememberSaveable(row.key) { mutableStateOf(false) }
    var value by rememberSaveable(row.key, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(titleFieldValue(row.text, moveCursorToEnd = row.shouldFocus))
    }
    val shouldRestoreFocus = restoreFocus

    LaunchedEffect(row.text, isFocused, restoreFocus, hasEditedWhileFocused) {
        val synchronizedState = synchronizeTitleFieldState(
            externalText = row.text,
            isFocused = isFocused,
            shouldFocus = row.shouldFocus,
            currentState = ProductDetailTitleFieldState(
                value = value,
                restoreFocus = restoreFocus,
                hasEditedWhileFocused = hasEditedWhileFocused,
            ),
        )
        value = synchronizedState.value
        restoreFocus = synchronizedState.restoreFocus
        hasEditedWhileFocused = synchronizedState.hasEditedWhileFocused
    }
    LaunchedEffect(row.shouldFocus, row.isReadOnly) {
        if (row.shouldFocus && !row.isReadOnly) focusRequester.requestFocus()
    }
    LaunchedEffect(Unit) {
        if (shouldRestoreFocus && !row.isReadOnly) focusRequester.requestFocus()
    }

    fun finishEditing() {
        restoreFocus = false
        hasEditedWhileFocused = false
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    BasicTextField(
        value = value,
        onValueChange = { updatedValue ->
            val textChanged = updatedValue.text != value.text
            value = updatedValue
            if (textChanged) {
                hasEditedWhileFocused = hasEditedWhileFocused || isFocused
                callback?.invoke(updatedValue.text)
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    restoreFocus = true
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Enter) {
                    finishEditing()
                    true
                } else {
                    false
                }
            }
            .testTag(ProductDetailTestTags.TITLE),
        enabled = !row.isReadOnly,
        singleLine = true,
        textStyle = WooTheme.text.titleLarge.regular.copy(color = WooTheme.colors.surface.onDefault),
        cursorBrush = SolidColor(WooTheme.colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { finishEditing() }),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.text.isEmpty()) {
                    Text(
                        text = stringResource(row.hint),
                        color = WooTheme.colors.surface.onVariant,
                        style = WooTheme.text.titleLarge.regular,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ProductDetailEditableBadge(row: ProductDetailRowUiModel.Editable) {
    if (row.badgeText != null && row.badgeTone != null) {
        WooBadge(
            text = stringResource(row.badgeText),
            tone = when (row.badgeTone) {
                ProductDetailBadgeTone.NEUTRAL -> WooBadgeTone.Neutral
                ProductDetailBadgeTone.WARNING -> WooBadgeTone.Warning
            },
        )
    }
}

@Composable
private fun ProductDetailPropertyGroupRow(
    row: ProductDetailRowUiModel.PropertyGroup,
    modifier: Modifier,
) {
    val propertyValue = buildString {
        row.properties.forEach { property ->
            when {
                property.label.isEmpty() -> append(property.value)
                property.value.isNotEmpty() -> {
                    if (isNotEmpty()) append('\n')
                    append(stringResource(row.propertyFormat, property.label, property.value))
                }
            }
        }
    }
    val isSingleUntitledValue = row.properties.size == 1 && !row.showTitle
    Column {
        ProductDetailPropertyCell(
            title = if (isSingleUntitledValue) propertyValue else stringResource(row.title),
            description = propertyValue.takeUnless { isSingleUntitledValue }?.let(::AnnotatedString),
            icon = row.icon,
            onClick = row.onClick,
            isHighlighted = row.isHighlighted,
            modifier = modifier,
        )
        ProductDetailOptionalDivider(
            show = row.showDivider,
            hasLeadingIcon = row.icon != null,
        )
    }
}

@Composable
private fun ProductDetailLinkRow(
    row: ProductDetailRowUiModel.Link,
    modifier: Modifier,
) {
    Column {
        WooCell(
            title = stringResource(row.title),
            onClick = row.onClick,
            enabled = row.onClick != null,
            modifier = modifier.disabledWhen(row.onClick == null),
            leadingContent = row.icon?.let { icon -> { ProductDetailIcon(icon) } },
            trailingContent = row.onClick?.let { { WooCellTrailingAffordance() } },
        )
        ProductDetailOptionalDivider(
            show = row.showDivider,
            hasLeadingIcon = row.icon != null,
        )
    }
}

@Composable
private fun ProductDetailButtonRow(
    row: ProductDetailRowUiModel.Button,
    modifier: Modifier,
) {
    var showTooltip by rememberSaveable(row.key) { mutableStateOf(row.tooltip != null) }
    val onClick by rememberUpdatedState(row.onClick)
    val onTooltipDismiss by rememberUpdatedState(row.tooltip?.onDismiss)

    Column {
        Column(
            modifier = modifier.padding(
                start = WooTheme.padding.padding7,
                end = WooTheme.padding.padding7,
                bottom = WooTheme.padding.padding4,
            ),
            horizontalAlignment = Alignment.Start,
        ) {
            Box {
                WooFilledTonalButton(
                    text = stringResource(row.text),
                    onClick = onClick,
                    size = WooButtonSize.Small,
                    leadingIcon = row.icon?.let { icon -> { ProductDetailIcon(icon) } },
                )
                DropdownMenu(
                    expanded = showTooltip && row.tooltip != null,
                    onDismissRequest = { showTooltip = false },
                    modifier = Modifier
                        .width(TOOLTIP_WIDTH)
                        .background(WooTheme.colors.surface.bright),
                ) {
                    row.tooltip?.let { tooltip ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(WooTheme.padding.padding5),
                            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                        ) {
                            Text(
                                text = stringResource(tooltip.title),
                                color = WooTheme.colors.surface.onDefault,
                                style = WooTheme.text.titleMedium.emphasized,
                            )
                            Text(
                                text = stringResource(tooltip.text),
                                color = WooTheme.colors.surface.onVariant,
                                style = WooTheme.text.bodyMedium.regular,
                            )
                            WooFilledTonalButton(
                                text = stringResource(tooltip.dismissButtonText),
                                onClick = {
                                    showTooltip = false
                                    onTooltipDismiss?.invoke()
                                },
                            )
                        }
                    }
                }
            }
            row.link?.let { link ->
                Spacer(modifier = Modifier.size(WooTheme.spacing.space3))
                Text(
                    text = productDetailAiAttributionText(
                        parsedHtml = AnnotatedString.fromHtml(stringResource(link.text)),
                        onVariantColor = WooTheme.colors.surface.onVariant,
                        linkColor = WooTheme.colors.primary,
                    ),
                    style = WooTheme.text.bodyMedium.emphasized,
                    modifier = Modifier.clickable(role = Role.Button, onClick = link.onClick),
                )
            }
        }
        ProductDetailOptionalDivider(
            show = row.showDivider,
        )
    }
}

@Composable
private fun ProductDetailSwitchRow(
    row: ProductDetailRowUiModel.Switch,
    modifier: Modifier,
) {
    val onStateChanged by rememberUpdatedState(row.onStateChanged)
    WooCell(
        title = stringResource(row.title),
        enabled = row.onStateChanged != null,
        onClick = row.onStateChanged?.let { { onStateChanged?.invoke(!row.isOn) } },
        modifier = modifier.disabledWhen(row.onStateChanged == null),
        leadingContent = row.icon?.let { icon -> { ProductDetailIcon(icon) } },
        trailingContent = {
            WooSwitch(
                checked = row.isOn,
                onCheckedChange = null,
                enabled = row.onStateChanged != null,
            )
        },
    )
}

@Composable
private fun ProductDetailPropertyCell(
    title: String,
    description: AnnotatedString?,
    icon: Int?,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    maxLines: Int = Int.MAX_VALUE,
    isHighlighted: Boolean = false,
    additionalContent: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        modifier
    }
    val foregroundColor = if (isHighlighted) {
        WooTheme.colors.status.onWarningContainer
    } else {
        WooTheme.colors.surface.onDefault
    }

    Row(
        modifier = clickableModifier
            .fillMaxWidth()
            .heightIn(min = MIN_ROW_HEIGHT)
            .background(WooTheme.colors.surface.bright)
            .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding4),
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { ProductDetailIcon(it, tint = foregroundColor) }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
        ) {
            Text(
                text = title,
                color = foregroundColor,
                style = WooTheme.text.bodyLarge.emphasized,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            description?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    color = if (isHighlighted) foregroundColor else WooTheme.colors.surface.onVariant,
                    style = WooTheme.text.bodyMedium.regular,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            additionalContent?.invoke()
        }
        if (onClick != null) {
            Icon(
                imageVector = WooIcons.Regular.AngleRight,
                contentDescription = stringResource(R.string.product_property_edit),
                tint = foregroundColor,
                modifier = Modifier.size(WooTheme.iconSize.size18),
            )
        }
    }
}

@Composable
private fun ProductRating(rating: Float) {
    val ratingDescription = stringResource(R.string.product_rating_content_description, rating)
    Row(modifier = Modifier.clearAndSetSemantics { contentDescription = ratingDescription }) {
        repeat(RATING_STAR_COUNT) { index ->
            val fraction = (rating - index).coerceIn(0f, 1f)
            Box(modifier = Modifier.size(WooTheme.iconSize.size18)) {
                Icon(
                    imageVector = WooIcons.Regular.Star,
                    contentDescription = null,
                    tint = WooTheme.colors.alert.orange,
                    modifier = Modifier.fillMaxSize(),
                )
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clipToBounds(),
                    ) {
                        Icon(
                            imageVector = WooIcons.Solid.Star,
                            contentDescription = null,
                            tint = WooTheme.colors.alert.orange,
                            modifier = Modifier.size(WooTheme.iconSize.size18),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailRatingSummary(
    rating: Float,
    reviewCount: String,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
    ) {
        ProductRating(rating)
        Text(
            text = reviewCount,
            color = WooTheme.colors.surface.onVariant,
            style = WooTheme.text.bodyMedium.regular,
        )
    }
}

private fun titleFieldValue(text: String, moveCursorToEnd: Boolean) = TextFieldValue(
    text = text,
    selection = if (moveCursorToEnd) TextRange(text.length) else TextRange.Zero,
)

private fun synchronizeTitleFieldValue(
    externalText: String,
    currentValue: TextFieldValue,
    preserveCurrentValue: Boolean,
    moveCursorToEnd: Boolean,
) = if (externalText == currentValue.text || preserveCurrentValue) {
    currentValue
} else {
    titleFieldValue(externalText, moveCursorToEnd)
}

internal fun synchronizeTitleFieldState(
    externalText: String,
    isFocused: Boolean,
    shouldFocus: Boolean,
    currentState: ProductDetailTitleFieldState,
): ProductDetailTitleFieldState {
    val matchesExternalText = externalText == currentState.value.text
    val shouldClearEditing = !isFocused && matchesExternalText
    return ProductDetailTitleFieldState(
        value = synchronizeTitleFieldValue(
            externalText = externalText,
            currentValue = currentState.value,
            preserveCurrentValue = currentState.hasEditedWhileFocused &&
                (isFocused || currentState.restoreFocus),
            moveCursorToEnd = isFocused || shouldFocus,
        ),
        restoreFocus = currentState.restoreFocus && !shouldClearEditing,
        hasEditedWhileFocused = currentState.hasEditedWhileFocused && !shouldClearEditing,
    )
}

internal data class ProductDetailTitleFieldState(
    val value: TextFieldValue,
    val restoreFocus: Boolean,
    val hasEditedWhileFocused: Boolean,
)

@Composable
private fun ProductDetailIcon(
    icon: Int,
    tint: androidx.compose.ui.graphics.Color = WooTheme.colors.surface.onVariant,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(WooTheme.iconSize.size24),
    )
}

@Composable
private fun ProductDetailOptionalDivider(
    show: Boolean,
    hasLeadingIcon: Boolean = false,
) {
    if (show) {
        val startPadding = WooTheme.padding.padding7 + if (hasLeadingIcon) {
            WooTheme.iconSize.size24 + WooTheme.spacing.space5
        } else {
            0.dp
        }
        WooDivider(modifier = Modifier.padding(start = startPadding))
    }
}

internal fun productDetailAiAttributionText(
    parsedHtml: AnnotatedString,
    onVariantColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
) = parsedHtml.getLinkAnnotations(start = 0, end = parsedHtml.length).let { linkRanges ->
    val textWithoutLinkAnnotations = parsedHtml.flatMapAnnotations { range ->
        if (range.item is LinkAnnotation) emptyList() else listOf(range)
    }
    buildAnnotatedString {
        withStyle(SpanStyle(color = onVariantColor)) {
            append(textWithoutLinkAnnotations)
        }
        linkRanges.forEach { range ->
            addStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
                start = range.start,
                end = range.end,
            )
        }
    }
}

private fun Modifier.disabledWhen(isDisabled: Boolean) = if (isDisabled) {
    semantics { disabled() }
} else {
    this
}

private const val RATING_STAR_COUNT = 5
private val MIN_ROW_HEIGHT = 56.dp
private val MIN_EDITABLE_HEIGHT = 64.dp
private val TOOLTIP_WIDTH = 280.dp
