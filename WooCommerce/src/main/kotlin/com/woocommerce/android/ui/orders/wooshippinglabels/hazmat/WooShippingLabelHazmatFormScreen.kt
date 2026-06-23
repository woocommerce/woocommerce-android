package com.woocommerce.android.ui.orders.wooshippinglabels.hazmat

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.clickableAnnotatedStringRes
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.model.ShippingLabelHazmatCategory

@Composable
fun WooShippingLabelHazmatFormScreen(viewModel: WooShippingLabelHazmatFormViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    WooShippingLabelHazmatFormScreen(
        containsHazmatChecked = viewState?.containsHazmatChecked == true,
        selectedHazmatCategory = viewState?.currentHazmatSelection,
        isSaveButtonVisible = viewState?.isSaveButtonVisible == true,
        isSelectCategoryButtonVisible = viewState?.isSelectCategoryButtonVisible == true,
        onContainsHazmatChanged = viewModel::onContainsHazmatChanged,
        onSelectCategoryClick = viewModel::onSelectCategoryClick,
        onSaveClick = viewModel::onSaveClick,
        onUrlSelected = viewModel::onUrlSelected
    )
}

@Composable
fun WooShippingLabelHazmatFormScreen(
    containsHazmatChecked: Boolean,
    selectedHazmatCategory: ShippingLabelHazmatCategory?,
    isSaveButtonVisible: Boolean,
    isSelectCategoryButtonVisible: Boolean,
    onContainsHazmatChanged: (Boolean) -> Unit,
    onSelectCategoryClick: () -> Unit,
    onSaveClick: () -> Unit,
    onUrlSelected: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.woo_shipping_labels_hazmat_info_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.color_on_surface)
        )
        Row(
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.woo_shipping_labels_hazmat_info_contains_hazmat),
                style = MaterialTheme.typography.bodyLarge,
                color = colorResource(id = R.color.color_on_surface),
                modifier = modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            )
            Checkbox(
                checked = containsHazmatChecked,
                onCheckedChange = onContainsHazmatChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = colorResource(id = R.color.color_on_surface_disabled),
                )
            )
        }

        if (selectedHazmatCategory != null) {
            HazmatCategoryEditingSection(
                selectedHazmatCategory = selectedHazmatCategory,
                onSelectCategoryClick = onSelectCategoryClick
            )
        } else if (isSelectCategoryButtonVisible) {
            WCColoredButton(
                text = stringResource(R.string.woo_shipping_labels_hazmat_info_select_category),
                onClick = onSelectCategoryClick,
                modifier = modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(modifier = modifier.padding(vertical = 8.dp))

        Text(
            text = stringResource(R.string.woo_shipping_labels_hazmat_info_full_description),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.color_on_surface),
        )

        Text(
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.color_on_surface),
            text = clickableAnnotatedStringRes(
                stringResId = R.string.woo_shipping_labels_hazmat_info_tooltip_1,
                onUrlClick = { url ->
                    when (url) {
                        "usps-hazmat" -> onUrlSelected(AppUrls.USPS_HAZMAT_INSTRUCTIONS)
                        "hazmat-tool" -> onUrlSelected(AppUrls.USPS_HAZMAT_SEARCH_TOOL)
                    }
                }
            )
        )

        Text(
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.color_on_surface),
            text = clickableAnnotatedStringRes(
                stringResId = R.string.woo_shipping_labels_hazmat_info_tooltip_2,
                onUrlClick = { onUrlSelected(AppUrls.DHL_EXPRESS_HAZMAT_INSTRUCTIONS) }
            )
        )

        if (isSaveButtonVisible) {
            Spacer(modifier = Modifier.weight(1f))

            WCColoredButton(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
fun HazmatCategoryEditingSection(
    selectedHazmatCategory: ShippingLabelHazmatCategory,
    onSelectCategoryClick: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.product_category),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = colorResource(id = R.color.color_on_surface),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onSelectCategoryClick,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled_24dp),
                    tint = colorResource(id = R.color.color_icon_menu),
                    contentDescription = stringResource(id = R.string.shipping_label_package_selected_description)
                )
            }
        }
        HazmatSelectionCard(selectedHazmatCategory)
    }
}

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WooShippingLabelHazmatFormScreenPreview() {
    WooThemeWithBackground {
        WooShippingLabelHazmatFormScreen(
            containsHazmatChecked = false,
            selectedHazmatCategory = null,
            isSaveButtonVisible = false,
            isSelectCategoryButtonVisible = true,
            onContainsHazmatChanged = {},
            onSelectCategoryClick = {},
            onSaveClick = {},
            onUrlSelected = {}
        )
    }
}

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WooShippingLabelHazmatFormScreenWithSelectionPreview() {
    WooThemeWithBackground {
        WooShippingLabelHazmatFormScreen(
            containsHazmatChecked = true,
            selectedHazmatCategory = ShippingLabelHazmatCategory.CLASS_1,
            isSaveButtonVisible = false,
            isSelectCategoryButtonVisible = false,
            onContainsHazmatChanged = {},
            onSelectCategoryClick = {},
            onSaveClick = {},
            onUrlSelected = {}
        )
    }
}
