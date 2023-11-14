package com.woocommerce.android.ui.orders.creation.configuration

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

internal const val OUTLINED_BORDER_OPACITY = 0.14f

@Composable
fun ProductConfigurationScreen(viewModel: ProductConfigurationViewModel) {
    val viewState by viewModel.viewState.collectAsState()
    BackHandler(onBack = viewModel::onCancel)
    Scaffold(topBar = {
        Column {
            val issues = (viewState as? ProductConfigurationViewModel.ViewState.DisplayConfiguration)
                ?.configurationIssues ?: emptyList()

            AnimatedVisibility(visible = issues.isEmpty().not()) {
                ConfigurationIssues(
                    issues = issues,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
        }
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

                            val info = childMapEntry.value[VariableProductRule.KEY]
                                .getAttributeOptionsFromJsonStringOrNull()

                            OptionalVariableQuantityProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = info,
                                quantity = childMapEntry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f,
                                onQuantityChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, QuantityRule.KEY, value.toString())
                                },
                                minValue = quantityRule?.quantityMin,
                                maxValue = quantityRule?.quantityMax,
                                onSelectAttributes = { onSelectChildrenAttributes(item.id) },
                                isIncluded = childMapEntry.value[OptionalRule.KEY]?.toBoolean() ?: false,
                                onSwitchChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, OptionalRule.KEY, value.toString())
                                }
                            )
                        }

                        hasVariableRuleAndQuantityRule -> {
                            val quantityRule = productRules.childrenRules
                                ?.get(childMapEntry.key)
                                ?.get(QuantityRule.KEY) as? QuantityRule

                            val info = childMapEntry.value[VariableProductRule.KEY]
                                .getAttributeOptionsFromJsonStringOrNull()

                            VariableQuantityProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = info,
                                quantity = childMapEntry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f,
                                onQuantityChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, QuantityRule.KEY, value.toString())
                                },
                                minValue = quantityRule?.quantityMin,
                                maxValue = quantityRule?.quantityMax,
                                onSelectAttributes = { onSelectChildrenAttributes(item.id) }
                            )
                        }

                        hasQuantityAndOptionalRules -> {
                            val quantityRule = productRules.childrenRules
                                ?.get(childMapEntry.key)
                                ?.get(QuantityRule.KEY) as? QuantityRule

                            OptionalQuantityProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = null,
                                quantity = childMapEntry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f,
                                onQuantityChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, QuantityRule.KEY, value.toString())
                                },
                                isIncluded = childMapEntry.value[OptionalRule.KEY]?.toBoolean() ?: false,
                                onSwitchChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, OptionalRule.KEY, value.toString())
                                },
                                minValue = quantityRule?.quantityMin,
                                maxValue = quantityRule?.quantityMax
                            )
                        }

                        hasQuantityRule -> {
                            val quantityRule = productRules.childrenRules
                                ?.get(childMapEntry.key)
                                ?.get(QuantityRule.KEY) as? QuantityRule
                            QuantityProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = null,
                                quantity = childMapEntry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f,
                                onQuantityChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, QuantityRule.KEY, value.toString())
                                },
                                minValue = quantityRule?.quantityMin,
                                maxValue = quantityRule?.quantityMax
                            )
                        }

                        hasOptionalRule -> {
                            OptionalProductItem(
                                title = item.title,
                                imageUrl = item.imageUrl,
                                info = null,
                                isIncluded = childMapEntry.value[OptionalRule.KEY]?.toBoolean() ?: false,
                                onSwitchChanged = { value ->
                                    onUpdateChildrenConfiguration(item.id, OptionalRule.KEY, value.toString())
                                }
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
    minValue: Float? = null
) {
    ConfigurableListItem(
        title = title,
        imageUrl = imageUrl,
        info = info,
        modifier = modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        configurableControlStart = {
            SelectionCheck(
                isSelected = isIncluded,
                onSelectionChange = onSwitchChanged,
                modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target))
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
    minValue: Float? = null
) {
    ConfigurableListItem(
        title = title,
        imageUrl = imageUrl,
        info = info,
        modifier = modifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        configurableControlStart = {
            SelectionCheck(
                isSelected = quantity > 0f,
                isEnabled = minValue == null || minValue <= 0f,
                onSelectionChange = { selected ->
                    onQuantityChanged(if (selected) 1f else 0f)
                },
                modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target))
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
                modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target))
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

@Composable
fun SelectionCheck(
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val selectionDrawable = if (isSelected) {
        R.drawable.ic_rounded_chcekbox_checked
    } else {
        R.drawable.ic_rounded_chcekbox_unchecked
    }

    val colorFilter = if (isEnabled) null else ColorFilter.tint(Color.Gray)

    Box(
        modifier = modifier.clickable { onSelectionChange(!isSelected) },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = selectionDrawable,
            modifier = modifier.wrapContentSize(),
            label = "itemSelection"
        ) { icon ->
            Image(
                painter = painterResource(id = icon),
                colorFilter = colorFilter,
                contentDescription = "imageContentDescription"
            )
        }
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
            contentDescription = stringResource(R.string.product_image_content_description),
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
    Row(
        modifier = modifier
            .background(colorResource(id = R.color.woo_blue_5))
            .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_info_outline_20dp),
            contentDescription = stringResource(id = R.string.configuration_issues),
            tint = colorResource(id = R.color.blaze_blue_60)
        )
        LazyColumn(modifier = Modifier.padding(start = 8.dp)) {
            items(issues) { issue -> Text(text = " • $issue", color = MaterialTheme.colors.onSurface) }
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
    minValue: Float? = null
) {
    Column(modifier = modifier.clickable { onSelectAttributes() }) {
        ConfigurableListItem(
            title = title,
            imageUrl = imageUrl,
            info = info,
            configurableControlStart = {
                SelectionCheck(
                    isSelected = quantity > 0f,
                    isEnabled = minValue == null || minValue <= 0f,
                    onSelectionChange = { selected ->
                        onQuantityChanged(if (selected) 1f else 0f)
                    },
                    modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target))
                )
            },
            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp, end = 0.dp)
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

        WCTextButton(
            onClick = { onSelectAttributes() },
            text = stringResource(id = R.string.configuration_variable_update),
            allCaps = false,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
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
    minValue: Float? = null
) {
    Column(modifier = modifier.clickable { onSelectAttributes() }) {
        ConfigurableListItem(
            title = title,
            imageUrl = imageUrl,
            info = info,
            configurableControlStart = {
                SelectionCheck(
                    isSelected = isIncluded,
                    onSelectionChange = onSwitchChanged,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target))
                )
            },
            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp, end = 0.dp)
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

        WCTextButton(
            onClick = { onSelectAttributes() },
            text = stringResource(id = R.string.configuration_variable_update),
            allCaps = false,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
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

fun String?.getAttributeOptionsFromJsonStringOrNull(): String? {
    return this?.let { attributes ->
        val regexPattern = "\"option\"\\s*:\\s*\"([^\"]+)\""
        val regex = Regex(regexPattern)
        val matches = regex.findAll(attributes)
        val optionsList = matches.map { it.groupValues[1] }
        optionsList.joinToString(" • ")
    }
}
