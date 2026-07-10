package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooColors
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooStroke
import com.woocommerce.android.ui.compose.designsystem.icons.AngleRight
import com.woocommerce.android.ui.compose.designsystem.icons.CircleFull
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
fun WooCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    WooCheckbox(
        state = ToggleableState(checked),
        onClick = onCheckedChange?.let { { it(!checked) } },
        modifier = modifier,
        enabled = enabled,
        isError = isError,
    )
}

@Composable
fun WooCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val style = wooCheckboxStyle(
        state = state,
        enabled = enabled,
        isError = isError,
        colors = WooTheme.colors,
        stroke = WooTheme.stroke,
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .wooCheckboxToggleable(
                state = state,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(CHOICE_CONTROL_SIZE)) {
            drawWooCheckbox(style)
        }
    }
}

@Composable
fun WooRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val style = wooRadioButtonStyle(
        selected = selected,
        enabled = enabled,
        colors = WooTheme.colors,
        stroke = WooTheme.stroke,
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .wooRadioSelectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(CHOICE_CONTROL_SIZE)) {
            drawWooRadioButton(style)
        }
    }
}

@Composable
fun WooFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val style = wooFilterChipStyle(
        selected = selected,
        enabled = enabled,
        colors = WooTheme.colors,
        stroke = WooTheme.stroke,
    )
    val shape = RoundedCornerShape(WooTheme.radius.large)

    CompositionLocalProvider(LocalContentColor provides style.contentColor) {
        Box(
            modifier = modifier
                .defaultMinSize(
                    minWidth = MIN_INTERACTIVE_COMPONENT_SIZE,
                    minHeight = MIN_INTERACTIVE_COMPONENT_SIZE,
                )
                .clip(shape)
                .wooFilterChipToggleable(
                    selected = selected,
                    enabled = enabled,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .height(FILTER_CHIP_HEIGHT)
                    .wooFilterChipBorder(style = style, shape = shape)
                    .background(
                        color = style.containerColor,
                        shape = shape,
                    )
                    .padding(horizontal = WooTheme.padding.padding4),
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.let {
                    WooFilterChipIcon(it)
                }
                Text(
                    text = label,
                    color = style.contentColor,
                    style = WooTheme.text.bodyMedium.emphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
                trailingIcon?.let {
                    WooFilterChipIcon(it)
                }
            }
        }
    }
}

@Composable
private fun WooFilterChipIcon(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(WooTheme.iconSize.size14),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun Modifier.wooFilterChipBorder(
    style: WooFilterChipStyle,
    shape: RoundedCornerShape,
): Modifier {
    return if (style.borderWidth > 0.dp) {
        border(
            width = style.borderWidth,
            color = style.borderColor,
            shape = shape,
        )
    } else {
        this
    }
}

private fun Modifier.wooCheckboxToggleable(
    state: ToggleableState,
    enabled: Boolean,
    onClick: (() -> Unit)?,
): Modifier {
    return if (onClick != null) {
        triStateToggleable(
            state = state,
            enabled = enabled,
            role = Role.Checkbox,
            onClick = onClick,
        )
    } else {
        semantics {
            role = Role.Checkbox
            toggleableState = state
            if (!enabled) {
                disabled()
            }
        }
    }
}

private fun Modifier.wooRadioSelectable(
    selected: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?,
): Modifier {
    return if (onClick != null) {
        selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        )
    } else {
        semantics {
            role = Role.RadioButton
            this.selected = selected
            if (!enabled) {
                disabled()
            }
        }
    }
}

private fun Modifier.wooFilterChipToggleable(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
): Modifier = toggleable(
    value = selected,
    enabled = enabled,
    role = Role.Checkbox,
    onValueChange = { onClick() },
)

private fun DrawScope.drawWooCheckbox(style: WooCheckboxStyle) {
    val strokeWidth = style.borderWidth.toPx()
    val halfStrokeWidth = strokeWidth / 2f
    val cornerRadius = CHECKBOX_CORNER_RADIUS.toPx()

    drawRoundRect(
        color = style.containerColor,
        size = size,
        cornerRadius = CornerRadius(cornerRadius),
    )
    if (style.borderWidth > 0.dp) {
        drawRoundRect(
            color = style.borderColor,
            topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
            size = Size(
                width = size.width - strokeWidth,
                height = size.height - strokeWidth,
            ),
            cornerRadius = CornerRadius(cornerRadius - halfStrokeWidth),
            style = Stroke(width = strokeWidth),
        )
    }
    when (style.mark) {
        WooCheckboxMark.Check -> drawWooCheckboxCheckmark(style.markColor)
        WooCheckboxMark.Indeterminate -> drawWooCheckboxIndeterminateMark(style.markColor)
        WooCheckboxMark.None -> Unit
    }
}

private fun DrawScope.drawWooCheckboxCheckmark(color: Color) {
    val checkmarkPath = Path().apply {
        moveTo(size.width * CHECKMARK_START_X, size.height * CHECKMARK_START_Y)
        lineTo(size.width * CHECKMARK_MIDDLE_X, size.height * CHECKMARK_MIDDLE_Y)
        lineTo(size.width * CHECKMARK_END_X, size.height * CHECKMARK_END_Y)
    }
    drawPath(
        path = checkmarkPath,
        color = color,
        style = Stroke(
            width = CHECKMARK_STROKE_WIDTH.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.drawWooCheckboxIndeterminateMark(color: Color) {
    drawLine(
        color = color,
        start = Offset(
            x = size.width * INDETERMINATE_MARK_START_X,
            y = size.height * INDETERMINATE_MARK_Y,
        ),
        end = Offset(
            x = size.width * INDETERMINATE_MARK_END_X,
            y = size.height * INDETERMINATE_MARK_Y,
        ),
        strokeWidth = INDETERMINATE_MARK_STROKE_WIDTH.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawWooRadioButton(style: WooRadioButtonStyle) {
    val strokeWidth = style.borderWidth.toPx()
    val halfStrokeWidth = strokeWidth / 2f
    val radius = size.minDimension / 2f

    drawCircle(
        color = style.containerColor,
        radius = radius,
    )
    if (style.borderWidth > 0.dp) {
        drawCircle(
            color = style.borderColor,
            radius = radius - halfStrokeWidth,
            style = Stroke(width = strokeWidth),
        )
    }
    if (style.dotColor != Color.Transparent) {
        drawCircle(
            color = style.dotColor,
            radius = RADIO_DOT_RADIUS.toPx(),
        )
    }
}

internal data class WooCheckboxStyle(
    val containerColor: Color,
    val borderColor: Color,
    val markColor: Color,
    val borderWidth: Dp,
    val mark: WooCheckboxMark,
)

internal enum class WooCheckboxMark {
    None,
    Check,
    Indeterminate,
}

internal data class WooRadioButtonStyle(
    val containerColor: Color,
    val borderColor: Color,
    val dotColor: Color,
    val borderWidth: Dp,
)

internal data class WooFilterChipStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val borderWidth: Dp,
)

internal fun wooCheckboxStyle(
    checked: Boolean,
    enabled: Boolean,
    colors: WooColors,
    stroke: WooStroke,
    isError: Boolean = false,
): WooCheckboxStyle = wooCheckboxStyle(
    state = ToggleableState(checked),
    enabled = enabled,
    isError = isError,
    colors = colors,
    stroke = stroke,
)

internal fun wooCheckboxStyle(
    state: ToggleableState,
    enabled: Boolean,
    isError: Boolean,
    colors: WooColors,
    stroke: WooStroke,
): WooCheckboxStyle {
    val selectedContainerColor = if (isError) colors.alert.red else colors.primary
    val selectedContentColor = if (isError) colors.alert.onRed else colors.onPrimary
    val disabledStateLayerColor = colors.stateLayer.onSurfaceOpacity16
    val mark = when (state) {
        ToggleableState.On -> WooCheckboxMark.Check
        ToggleableState.Indeterminate -> WooCheckboxMark.Indeterminate
        ToggleableState.Off -> WooCheckboxMark.None
    }

    return when {
        !enabled && state == ToggleableState.Off -> WooCheckboxStyle(
            containerColor = Color.Transparent,
            borderColor = disabledStateLayerColor,
            markColor = Color.Transparent,
            borderWidth = stroke.medium,
            mark = WooCheckboxMark.None,
        )

        !enabled -> WooCheckboxStyle(
            containerColor = disabledStateLayerColor,
            borderColor = Color.Transparent,
            markColor = colors.onPrimary,
            borderWidth = stroke.none,
            mark = mark,
        )

        state == ToggleableState.Off -> WooCheckboxStyle(
            containerColor = Color.Transparent,
            borderColor = selectedContainerColor,
            markColor = Color.Transparent,
            borderWidth = stroke.medium,
            mark = WooCheckboxMark.None,
        )

        else -> WooCheckboxStyle(
            containerColor = selectedContainerColor,
            borderColor = Color.Transparent,
            markColor = selectedContentColor,
            borderWidth = stroke.none,
            mark = mark,
        )
    }
}

internal fun wooRadioButtonStyle(
    selected: Boolean,
    enabled: Boolean,
    colors: WooColors,
    stroke: WooStroke,
): WooRadioButtonStyle {
    val disabledStateLayerColor = colors.stateLayer.onSurfaceOpacity16
    return when {
        selected && enabled -> WooRadioButtonStyle(
            containerColor = colors.primary,
            borderColor = Color.Transparent,
            dotColor = colors.onPrimary,
            borderWidth = stroke.none,
        )

        selected -> WooRadioButtonStyle(
            containerColor = disabledStateLayerColor,
            borderColor = Color.Transparent,
            dotColor = colors.onPrimary,
            borderWidth = stroke.none,
        )

        enabled -> WooRadioButtonStyle(
            containerColor = Color.Transparent,
            borderColor = colors.primary,
            dotColor = Color.Transparent,
            borderWidth = stroke.medium,
        )

        else -> WooRadioButtonStyle(
            containerColor = Color.Transparent,
            borderColor = disabledStateLayerColor,
            dotColor = Color.Transparent,
            borderWidth = stroke.medium,
        )
    }
}

internal fun wooFilterChipStyle(
    selected: Boolean,
    enabled: Boolean,
    colors: WooColors,
    stroke: WooStroke,
): WooFilterChipStyle {
    return when {
        selected -> WooFilterChipStyle(
            containerColor = colors.container.secondaryContainer,
            contentColor = if (enabled) colors.surface.onDefault else colors.surface.onVariantLowest,
            borderColor = Color.Transparent,
            borderWidth = stroke.none,
        )

        else -> WooFilterChipStyle(
            containerColor = colors.surface.default,
            contentColor = if (enabled) colors.surface.onDefault else colors.surface.onVariantLowest,
            borderColor = colors.outlineVariant,
            borderWidth = stroke.regular,
        )
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooChoiceControlsPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooChoiceControlsDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooChoiceControlsDemo(
    modifier: Modifier = Modifier,
) {
    var checkedCheckboxChecked by rememberSaveable { mutableStateOf(true) }
    var uncheckedCheckboxChecked by rememberSaveable { mutableStateOf(false) }
    var unselectedRadioSelected by rememberSaveable { mutableStateOf(false) }
    var baseFilterSelected by rememberSaveable { mutableStateOf(false) }
    var trailingFilterSelected by rememberSaveable { mutableStateOf(false) }
    var selectedFilterSelected by rememberSaveable { mutableStateOf(true) }
    var fullFilterSelected by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        ChoiceControlStateRows(
            checkedCheckboxChecked = checkedCheckboxChecked,
            onCheckedCheckboxChange = { checkedCheckboxChecked = it },
            uncheckedCheckboxChecked = uncheckedCheckboxChecked,
            onUncheckedCheckboxChange = { uncheckedCheckboxChecked = it },
            unselectedRadioSelected = unselectedRadioSelected,
            onSelectedRadioClick = { unselectedRadioSelected = false },
            onUnselectedRadioClick = { unselectedRadioSelected = true },
        )
        FilterChipRows(
            baseFilterSelected = baseFilterSelected,
            onBaseFilterClick = { baseFilterSelected = !baseFilterSelected },
            trailingFilterSelected = trailingFilterSelected,
            onTrailingFilterClick = { trailingFilterSelected = !trailingFilterSelected },
            selectedFilterSelected = selectedFilterSelected,
            onSelectedFilterClick = { selectedFilterSelected = !selectedFilterSelected },
            fullFilterSelected = fullFilterSelected,
            onFullFilterClick = { fullFilterSelected = !fullFilterSelected },
        )
    }
}

@Composable
private fun ChoiceControlStateRows(
    checkedCheckboxChecked: Boolean,
    onCheckedCheckboxChange: (Boolean) -> Unit,
    uncheckedCheckboxChecked: Boolean,
    onUncheckedCheckboxChange: (Boolean) -> Unit,
    unselectedRadioSelected: Boolean,
    onSelectedRadioClick: () -> Unit,
    onUnselectedRadioClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooCheckbox(
                checked = checkedCheckboxChecked,
                onCheckedChange = onCheckedCheckboxChange,
                modifier = Modifier.testTag(WooChoiceControlsDemoTags.CHECKED_CHECKBOX),
            )
            WooCheckbox(
                checked = uncheckedCheckboxChecked,
                onCheckedChange = onUncheckedCheckboxChange,
            )
            WooCheckbox(state = ToggleableState.Indeterminate, onClick = {})
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooCheckbox(checked = true, onCheckedChange = {}, isError = true)
            WooCheckbox(checked = false, onCheckedChange = {}, isError = true)
            WooCheckbox(checked = true, onCheckedChange = {}, enabled = false)
            WooCheckbox(checked = false, onCheckedChange = {}, enabled = false)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooRadioButton(selected = !unselectedRadioSelected, onClick = onSelectedRadioClick)
            WooRadioButton(
                selected = unselectedRadioSelected,
                onClick = onUnselectedRadioClick,
                modifier = Modifier.testTag(WooChoiceControlsDemoTags.UNSELECTED_RADIO),
            )
            WooRadioButton(selected = true, onClick = {}, enabled = false)
            WooRadioButton(selected = false, onClick = {}, enabled = false)
        }
    }
}

@Composable
private fun FilterChipRows(
    baseFilterSelected: Boolean,
    onBaseFilterClick: () -> Unit,
    trailingFilterSelected: Boolean,
    onTrailingFilterClick: () -> Unit,
    selectedFilterSelected: Boolean,
    onSelectedFilterClick: () -> Unit,
    fullFilterSelected: Boolean,
    onFullFilterClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooFilterChip(
                selected = baseFilterSelected,
                onClick = onBaseFilterClick,
                label = "Filter",
            )
            WooFilterChip(
                selected = trailingFilterSelected,
                onClick = onTrailingFilterClick,
                label = "Filter",
                trailingIcon = { ChoiceControlPreviewIcon(WooIcons.Regular.AngleRight) },
            )
            WooFilterChip(
                selected = selectedFilterSelected,
                onClick = onSelectedFilterClick,
                label = "Selected",
                leadingIcon = {
                    ChoiceControlPreviewIcon(WooIcons.Solid.CircleFull)
                },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooFilterChip(
                selected = fullFilterSelected,
                onClick = onFullFilterClick,
                label = "Selected",
                leadingIcon = {
                    ChoiceControlPreviewIcon(WooIcons.Solid.CircleFull)
                },
                trailingIcon = { ChoiceControlPreviewIcon(WooIcons.Regular.AngleRight) },
            )
            WooFilterChip(selected = false, onClick = {}, label = "Disabled", enabled = false)
        }
    }
}

@Composable
private fun ChoiceControlPreviewIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(WooTheme.iconSize.size14),
    )
}

internal object WooChoiceControlsDemoTags {
    const val CHECKED_CHECKBOX = "WooChoiceControlsDemoCheckedCheckbox"
    const val UNSELECTED_RADIO = "WooChoiceControlsDemoUnselectedRadio"
}

private val CHOICE_CONTROL_SIZE = 24.dp
private val CHECKBOX_CORNER_RADIUS = 8.dp
private val CHECKMARK_STROKE_WIDTH = 2.dp
private val INDETERMINATE_MARK_STROKE_WIDTH = 2.4f.dp
private val RADIO_DOT_RADIUS = 4.dp
private val FILTER_CHIP_HEIGHT = 32.dp
private val MIN_INTERACTIVE_COMPONENT_SIZE = 48.dp

private const val CHECKMARK_START_X = 0.29f
private const val CHECKMARK_START_Y = 0.52f
private const val CHECKMARK_MIDDLE_X = 0.43f
private const val CHECKMARK_MIDDLE_Y = 0.67f
private const val CHECKMARK_END_X = 0.72f
private const val CHECKMARK_END_Y = 0.35f
private const val INDETERMINATE_MARK_START_X = 0.325f
private const val INDETERMINATE_MARK_END_X = 0.675f
private const val INDETERMINATE_MARK_Y = 0.5f
