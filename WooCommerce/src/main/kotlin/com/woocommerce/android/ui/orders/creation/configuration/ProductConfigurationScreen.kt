package com.woocommerce.android.ui.orders.creation.configuration

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.compose.component.SelectionCheck
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

internal const val OUTLINED_BORDER_OPACITY = 0.14f

@Composable
fun ProductConfigurationScreen(viewModel: ProductConfigurationViewModel) {
    val viewState by viewModel.viewState.collectAsState()
    BackHandler(onBack = viewModel::onCancel)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(id = R.string.product_configuration_title)) },
            navigationIcon = {
                IconButton(viewModel::onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.close)
                    )
                }
            },
            backgroundColor = colorResource(id = R.color.color_toolbar),
            elevation = 0.dp,
        )
    }) { padding ->
        when (val state = viewState) {
            is ProductConfigurationViewModel.ViewState.Error -> Text(text = state.message)
            is ProductConfigurationViewModel.ViewState.Loading -> Text(text = "Loading")
            is ProductConfigurationViewModel.ViewState.DisplayConfiguration -> {
                ProductConfigurationScreen(
                    productRules = state.productConfiguration.rules,
                    productConfiguration = state.productConfiguration,
                    productsInfo = state.productsInfo,
                    onUpdateChildrenConfiguration = viewModel::onUpdateChildrenConfiguration,
                    onSaveConfigurationClick = viewModel::onSaveConfiguration,
                    modifier = Modifier.padding(padding),
                    configurationIssues = state.configurationIssues,
                    onSelectChildrenAttributes = viewModel::onSelectChildrenAttributes
                )
            }
        }
    }
}

@Suppress("ComplexMethod")
@Composable
fun ProductConfigurationScreen(
    productRules: ProductRules,
    productConfiguration: ProductConfiguration,
    productsInfo: Map<Long, ProductInfo>,
    onUpdateChildrenConfiguration: (Long, String, String) -> Unit,
    onSaveConfigurationClick: () -> Unit,
    onSelectChildrenAttributes: (itemId: Long) -> Unit,
    modifier: Modifier = Modifier,
    configurationIssues: List<String> = emptyList()
) {
    Surface {
        Column(modifier = modifier) {
            val isMaxChildrenReached = productConfiguration.isMaxChildrenReached()
            LazyColumn(Modifier.weight(1f)) {
                val configurationItems = productConfiguration.childrenConfiguration?.entries?.toList() ?: emptyList()
                items(configurationItems) { childMapEntry ->
                    val item = productsInfo.getOrDefault(
                        childMapEntry.key,
                        ProductInfo(
                            childMapEntry.key,
                            -1L,
                            stringResource(id = R.string.default_product_title, childMapEntry.key),
                            null
                        )
                    )

                    val hasQuantityRule = childMapEntry.value.containsKey(QuantityRule.KEY)
                    val hasOptionalRule = childMapEntry.value.containsKey(OptionalRule.KEY)
                    val hasVariableRule = childMapEntry.value.containsKey(VariableProductRule.KEY)

                    val hasVariableRuleAndQuantityRule = hasVariableRule && hasQuantityRule
                    val hasQuantityAndOptionalRules = hasQuantityRule && hasOptionalRule
                    val hasVariableQuantityAndOptionalRules = hasVariableRuleAndQuantityRule && hasOptionalRule

                    when {
                        hasVariableQuantityAndOptionalRules -> {
                            val quantityRule = productRules.childrenRules
                                ?.get(childMapEntry.key)
                                ?.get(QuantityRule.KEY) as? QuantityRule

                            val attributes = childMapEntry.value[VariableProductRule.KEY]
                                .toAttributesFromConfigurationStringOrNull()

                            val quantity = childMapEntry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f
                            val isIncluded = childMapEntry.value[OptionalRule.KEY]?.toBoolean() ?: false

                            OptionalVariableQuantityProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = null,
                                quantity = quantity,
                                onQuantityChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, QuantityRule.KEY, value.toString())
                                },
                                minValue = quantityRule?.quantityMin,
                                maxValue = if (isMaxChildrenReached) quantity else quantityRule?.quantityMax,
                                onSelectAttributes = { onSelectChildrenAttributes(item.id) },
                                isIncluded = isIncluded,
                                onSwitchChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, OptionalRule.KEY, value.toString())
                                },
                                attributes = attributes,
                                isSelectionEnabled = isMaxChildrenReached.not() || (isMaxChildrenReached && isIncluded)
                            )
                        }

                        hasVariableRuleAndQuantityRule -> {
                            val quantityRule = productRules.childrenRules
                                ?.get(childMapEntry.key)
                                ?.get(QuantityRule.KEY) as? QuantityRule

                            val attributes = childMapEntry.value[VariableProductRule.KEY]
                                .toAttributesFromConfigurationStringOrNull()

                            val quantity = childMapEntry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f
                            val isIncluded = quantity > 0f

                            VariableQuantityProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = null,
                                quantity = quantity,
                                onQuantityChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, QuantityRule.KEY, value.toString())
                                },
                                minValue = quantityRule?.quantityMin,
                                maxValue = if (isMaxChildrenReached) quantity else quantityRule?.quantityMax,
                                onSelectAttributes = { onSelectChildrenAttributes(item.id) },
                                attributes = attributes,
                                isSelectionEnabled = isMaxChildrenReached.not() || (isMaxChildrenReached && isIncluded)
                            )
                        }

                        hasQuantityAndOptionalRules -> {
                            val quantityRule = productRules.childrenRules
                                ?.get(childMapEntry.key)
                                ?.get(QuantityRule.KEY) as? QuantityRule

                            val quantity = childMapEntry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f
                            val isIncluded = childMapEntry.value[OptionalRule.KEY]?.toBoolean() ?: false

                            OptionalQuantityProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = null,
                                quantity = quantity,
                                onQuantityChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, QuantityRule.KEY, value.toString())
                                },
                                isIncluded = isIncluded,
                                onSwitchChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, OptionalRule.KEY, value.toString())
                                },
                                minValue = quantityRule?.quantityMin,
                                maxValue = if (isMaxChildrenReached) quantity else quantityRule?.quantityMax,
                                isSelectionEnabled = isMaxChildrenReached.not() || (isMaxChildrenReached && isIncluded)
                            )
                        }

                        hasQuantityRule -> {
                            val quantityRule = productRules.childrenRules
                                ?.get(childMapEntry.key)
                                ?.get(QuantityRule.KEY) as? QuantityRule

                            val quantity = childMapEntry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f
                            val isIncluded = quantity > 0f

                            QuantityProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = null,
                                quantity = quantity,
                                onQuantityChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, QuantityRule.KEY, value.toString())
                                },
                                minValue = quantityRule?.quantityMin,
                                maxValue = if (isMaxChildrenReached) quantity else quantityRule?.quantityMax,
                                isSelectionEnabled = isMaxChildrenReached.not() || (isMaxChildrenReached && isIncluded)
                            )
                        }

                        hasOptionalRule -> {
                            val isIncluded = childMapEntry.value[OptionalRule.KEY]?.toBoolean() ?: false
                            OptionalProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = null,
                                isIncluded = isIncluded,
                                onSwitchChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, OptionalRule.KEY, value.toString())
                                },
                                isSelectionEnabled = isMaxChildrenReached.not() || (isMaxChildrenReached && isIncluded)
                            )
                        }

                        else -> {}
                    }
                    Divider(
                        color = colorResource(id = R.color.divider_color),
                        thickness = dimensionResource(id = R.dimen.minor_10)
                    )
                }
            }

            ConfigurationIssues(
                issues = configurationIssues,
                modifier = Modifier.fillMaxWidth()
            )

            Divider(
                color = colorResource(id = R.color.divider_color),
                thickness = dimensionResource(id = R.dimen.minor_10)
            )
            WCColoredButton(
                onClick = onSaveConfigurationClick,
                text = stringResource(id = R.string.save_configuration),
                enabled = configurationIssues.isEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@Composable
fun OptionalQuantityProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    quantity: Float,
    onQuantityChanged: (Float) -> Unit,
    isIncluded: Boolean,
    onSwitchChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    minValue: Float? = null,
    isSelectionEnabled: Boolean = true
) {
    val description = stringResource(id = R.string.order_configuration_product_selection, title)
    val state = if (!isSelectionEnabled) stringResource(id = R.string.disabled) else ""

    val itemModifier = if (isSelectionEnabled) {
        modifier
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = state
            }
            .clickable { onSwitchChanged(!isIncluded) }
    } else {
        modifier.clearAndSetSemantics {
            contentDescription = description
            stateDescription = state
        }
    }

    ConfigurableListItem(
        title = title,
        imageUrl = imageUrl,
        info = info,
        modifier = itemModifier.padding(vertical = 16.dp, horizontal = 8.dp),
        configurableControlStart = {
            SelectionCheck(
                isSelected = isIncluded,
                onSelectionChange = onSwitchChanged,
                modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target)),
                isEnabled = isSelectionEnabled
            )
        },
        configurableControlEnd = {
            AnimatedVisibility(
                visible = isIncluded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Stepper(
                    value = quantity,
                    onStepUp = { value -> onQuantityChanged(value) },
                    onStepDown = { value -> onQuantityChanged(value) },
                    isStepDownEnabled = isIncluded && quantity > (minValue ?: Float.MIN_VALUE),
                    isStepUpEnabled = isIncluded && quantity < (maxValue ?: Float.MAX_VALUE)
                )
            }
        }
    )
}

@Composable
fun QuantityProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    quantity: Float,
    onQuantityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    minValue: Float? = null,
    isSelectionEnabled: Boolean = true
) {
    val description = stringResource(id = R.string.order_configuration_product_selection, title)
    val state = if (!isSelectionEnabled) stringResource(id = R.string.disabled) else ""

    val itemModifier = if (isSelectionEnabled) {
        modifier
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = state
            }
            .clickable {
                if (minValue == null || minValue <= 0f) onQuantityChanged(if (quantity == 0f) 1f else 0f)
            }
    } else {
        modifier.clearAndSetSemantics {
            contentDescription = description
            stateDescription = state
        }
    }

    ConfigurableListItem(
        title = title,
        imageUrl = imageUrl,
        info = info,
        modifier = itemModifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        configurableControlStart = {
            SelectionCheck(
                isSelected = quantity > 0f,
                isEnabled = (minValue == null || minValue <= 0f) && isSelectionEnabled,
                onSelectionChange = { selected ->
                    onQuantityChanged(if (selected) 1f else 0f)
                },
                modifier = Modifier
                    .semantics(mergeDescendants = false) {}
                    .size(dimensionResource(id = R.dimen.min_tap_target))
            )
        }
    ) {
        AnimatedVisibility(
            visible = quantity > 0f,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Stepper(
                value = quantity,
                onStepUp = { value -> onQuantityChanged(value) },
                onStepDown = { value -> onQuantityChanged(value) },
                isStepDownEnabled = quantity > (minValue ?: Float.MIN_VALUE),
                isStepUpEnabled = quantity < (maxValue ?: Float.MAX_VALUE)
            )
        }
    }
}

@Preview
@Composable
fun QuantityProductItemPreview() {
    WooThemeWithBackground {
        QuantityProductItem(
            title = "This is an optional item with a very very very long title that should wrap into two columns",
            imageUrl = null,
            info = null,
            quantity = 1f,
            onQuantityChanged = {}
        )
    }
}

@Composable
fun OptionalProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    isIncluded: Boolean,
    onSwitchChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isSelectionEnabled: Boolean = true
) {
    ConfigurableListItem(
        title = title,
        imageUrl = imageUrl,
        info = info,
        modifier = modifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        configurableControlStart = {
            SelectionCheck(
                isSelected = isIncluded,
                onSelectionChange = onSwitchChanged,
                modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target)),
                isEnabled = isSelectionEnabled
            )
        }
    )
}

@Preview
@Composable
fun OptionalChildrenPreview() {
    WooThemeWithBackground {
        OptionalProductItem(
            title = "This is an optional item with a very very very long title that should wrap into two columns",
            imageUrl = null,
            info = null,
            isIncluded = true,
            onSwitchChanged = {}
        )
    }
}

@Preview
@Composable
fun SelectionCheckPreview() {
    var value: Boolean by rememberSaveable { mutableStateOf(false) }
    WooThemeWithBackground {
        SelectionCheck(
            isSelected = value,
            onSelectionChange = { newValue -> value = newValue }
        )
    }
}

@Composable
fun ConfigurableListItem(
    title: String,
    imageUrl: String?,
    info: String?,
    modifier: Modifier = Modifier,
    configurableControlStart: @Composable () -> Unit = {},
    configurableControlEnd: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.wrapContentSize()) {
            configurableControlStart()
        }
        OrderProductItem(
            title = title,
            imageUrl = imageUrl,
            info = info,
            modifier = Modifier.weight(2f)
        )
        Box(modifier = Modifier.wrapContentSize()) {
            configurableControlEnd()
        }
    }
}

@Preview
@Composable
fun ConfigurableListItemPreview() {
    WooThemeWithBackground {
        ConfigurableListItem(
            title = "This the product title",
            imageUrl = "not valid url",
            info = "this is the product description"
        ) {
            Button(onClick = { }) {
                Text(text = "Configure Item")
            }
        }
    }
}

@Composable
fun OrderProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.ic_product),
            error = painterResource(R.drawable.ic_product),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .size(dimensionResource(R.dimen.major_300))
                .clip(RoundedCornerShape(3.dp))
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .wrapContentHeight()
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1
            )
            if (!info.isNullOrEmpty()) {
                Text(
                    text = info,
                    style = MaterialTheme.typography.body2.copy(
                        color = MaterialTheme.colors.onSurface.copy(alpha = .8f)
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun OrderProductItemWithInfoPreview() {
    WooThemeWithBackground {
        OrderProductItem(
            title = "This the product title",
            imageUrl = "not valid url",
            info = "this is the product description"
        )
    }
}

@Preview
@Composable
fun OrderProductItemWithoutInfoPreview() {
    WooThemeWithBackground {
        OrderProductItem(
            title = "This the product title",
            imageUrl = "not valid url",
            info = null
        )
    }
}

@Composable
fun Stepper(
    value: Float,
    onStepUp: (Float) -> Unit,
    onStepDown: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isStepDownEnabled: Boolean = true,
    isStepUpEnabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = OUTLINED_BORDER_OPACITY),
                shape = RoundedCornerShape(8.dp)
            )

    ) {
        OutlinedButton(
            onClick = { onStepDown(value - 1) },
            enabled = isStepDownEnabled,
            modifier = Modifier
                .sizeIn(
                    minWidth = dimensionResource(id = R.dimen.min_tap_target),
                    minHeight = dimensionResource(id = R.dimen.min_tap_target)
                )
                .padding(top = 1.dp, bottom = 1.dp, start = 2.dp),
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gridicons_minus),
                contentDescription = stringResource(
                    id = R.string.order_configuration_change_product_quantity,
                    value,
                    value - 1f
                )
            )
        }

        BasicTextField(
            value = value.formatToString(),
            readOnly = true,
            onValueChange = {},
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.subtitle1.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface
            ),
            modifier = Modifier
                .background(color = MaterialTheme.colors.surface)
                .width(dimensionResource(id = R.dimen.min_tap_target))

        )
        OutlinedButton(
            onClick = { onStepUp(value + 1) },
            enabled = isStepUpEnabled,
            modifier = Modifier
                .sizeIn(
                    minWidth = dimensionResource(id = R.dimen.min_tap_target),
                    minHeight = dimensionResource(id = R.dimen.min_tap_target)
                )
                .padding(top = 1.dp, bottom = 1.dp, end = 2.dp),
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = stringResource(
                    id = R.string.order_configuration_change_product_quantity,
                    value,
                    value + 1f
                )
            )
        }
    }
}

@Preview
@Composable
fun StepperPreview() {
    var value: Float by rememberSaveable { mutableStateOf(100f) }
    WooThemeWithBackground {
        Stepper(
            value = value,
            onStepDown = { newValue -> value = newValue },
            onStepUp = { newValue -> value = newValue },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun ConfigurationIssues(
    issues: List<String>,
    modifier: Modifier = Modifier
) {
    val text = if (issues.isEmpty()) {
        stringResource(id = R.string.configuration_complete)
    } else {
        stringResource(id = R.string.configuration_required)
    }
    val color = if (issues.isEmpty()) {
        colorResource(id = R.color.woo_green_5)
    } else {
        colorResource(id = R.color.woo_blue_5)
    }
    Column(
        modifier = modifier
            .padding(all = 16.dp)
            .background(
                shape = RoundedCornerShape(8.dp),
                color = color
            )
            .padding(all = 16.dp)
            .animateContentSize()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        if (issues.isNotEmpty()) {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(issues) { issue ->
                    Text(
                        text = issue,
                        style = MaterialTheme.typography.body1,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ConfigurationIssuesPreview() {
    WooThemeWithBackground {
        ConfigurationIssues(listOf("Need to select 2 items", "Caipi -> please choose product options"))
    }
}

@Composable
fun VariableQuantityProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    quantity: Float,
    onQuantityChanged: (Float) -> Unit,
    onSelectAttributes: () -> Unit,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    minValue: Float? = null,
    attributes: List<VariantOption>? = null,
    isSelectionEnabled: Boolean = true
) {
    val description = stringResource(id = R.string.order_configuration_product_selection, title)
    val state = if (!isSelectionEnabled) stringResource(id = R.string.disabled) else ""

    val itemModifier = if (isSelectionEnabled) {
        modifier
            .semantics {
                contentDescription = description
                stateDescription = state
            }
            .clickable {
                if (quantity > 0f) {
                    onSelectAttributes()
                } else {
                    onQuantityChanged(1f)
                }
            }
    } else {
        modifier.clearAndSetSemantics {
            contentDescription = description
            stateDescription = state
        }
    }

    Column(modifier = itemModifier.animateContentSize()) {
        ConfigurableListItem(
            title = title,
            imageUrl = imageUrl,
            info = info,
            configurableControlStart = {
                SelectionCheck(
                    isSelected = quantity > 0f,
                    isEnabled = (minValue == null || minValue <= 0f) && isSelectionEnabled,
                    onSelectionChange = { selected ->
                        onQuantityChanged(if (selected) 1f else 0f)
                    },
                    modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target))
                )
            },
            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 0.dp)
        ) {
            AnimatedVisibility(
                visible = quantity > 0f,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Stepper(
                    value = quantity,
                    onStepUp = { value -> onQuantityChanged(value) },
                    onStepDown = { value -> onQuantityChanged(value) },
                    isStepDownEnabled = quantity > (minValue ?: Float.MIN_VALUE),
                    isStepUpEnabled = quantity < (maxValue ?: Float.MAX_VALUE),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = quantity > 0f,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VariableSelection(
                attributes = attributes,
                onSelectAttributes = onSelectAttributes,
                modifier = Modifier.padding(start = 56.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun OptionalVariableQuantityProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    quantity: Float,
    onQuantityChanged: (Float) -> Unit,
    onSelectAttributes: () -> Unit,
    isIncluded: Boolean,
    onSwitchChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    minValue: Float? = null,
    attributes: List<VariantOption>? = null,
    isSelectionEnabled: Boolean = true
) {
    val description = stringResource(id = R.string.order_configuration_product_selection, title)
    val state = if (!isSelectionEnabled) stringResource(id = R.string.disabled) else ""

    val itemModifier = if (isSelectionEnabled) {
        modifier
            .semantics {
                contentDescription = description
                stateDescription = state
            }
            .clickable {
                if (isIncluded) {
                    onSelectAttributes()
                } else {
                    onSwitchChanged(true)
                }
            }
    } else {
        modifier.clearAndSetSemantics {
            contentDescription = description
            stateDescription = state
        }
    }

    Column(modifier = itemModifier.animateContentSize()) {
        ConfigurableListItem(
            title = title,
            imageUrl = imageUrl,
            info = info,
            configurableControlStart = {
                SelectionCheck(
                    isSelected = isIncluded,
                    onSelectionChange = onSwitchChanged,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target)),
                    isEnabled = isSelectionEnabled
                )
            },
            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 0.dp)
        ) {
            AnimatedVisibility(
                visible = isIncluded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Stepper(
                    value = quantity,
                    onStepUp = { value -> onQuantityChanged(value) },
                    onStepDown = { value -> onQuantityChanged(value) },
                    isStepDownEnabled = quantity > (minValue ?: Float.MIN_VALUE),
                    isStepUpEnabled = quantity < (maxValue ?: Float.MAX_VALUE),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isIncluded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VariableSelection(
                attributes = attributes,
                onSelectAttributes = onSelectAttributes,
                modifier = Modifier.padding(start = 56.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun VariableSelection(
    attributes: List<VariantOption>?,
    onSelectAttributes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )

        if (attributes?.isNotEmpty() == true) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                attributes.forEach { attribute ->
                    val annotatedString = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(attribute.name)
                        }
                        append(" ${attribute.option}")
                    }
                    Text(text = annotatedString, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .clickable { onSelectAttributes() }
                .padding(vertical = 8.dp)
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                text = stringResource(id = R.string.configuration_variable_update),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.primary,
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(dimensionResource(id = R.dimen.major_200))
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Preview
@Composable
fun VariableQuantityProductItemPreview() {
    WooThemeWithBackground {
        VariableQuantityProductItem(
            title = "This is an item with title",
            imageUrl = null,
            info = "Attribute 1 • Attribute 2",
            quantity = 1f,
            onQuantityChanged = {},
            onSelectAttributes = {}
        )
    }
}

@Suppress("UNCHECKED_CAST")
private fun String?.toAttributesFromConfigurationStringOrNull(): List<VariantOption>? {
    return this?.runCatching {
        val gson = Gson()
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        val map = gson.fromJson<Map<String, Any>>(this, mapType)
        (map[VariableProductRule.VARIATION_ATTRIBUTES] as? List<Map<String, Any>>)?.mapNotNull { attribute ->
            VariantOption(
                id = attribute["id"] as? Long,
                name = attribute["name"]?.toString(),
                option = attribute["option"]?.toString()
            ).takeIf { variantOption -> variantOption != VariantOption.empty }
        }
    }?.getOrNull()
}
