package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.login.storecreation.countrypicker.CountryPickerViewModel.StoreCreationCountry

@Composable
fun CountryPickerScreen(viewModel: CountryPickerViewModel) {
    viewModel.countryPickerContent.observeAsState().value?.let { countryPickerContent ->
        Scaffold(topBar = {
            Toolbar(
                title = { Text("") },
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationButtonClick = viewModel::onArrowBackPressed,
                actions = {
                    IconButton(onClick = viewModel::onHelpPressed) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_help_24dp),
                            contentDescription = stringResource(id = R.string.help)
                        )
                    }
                }
            )
        }) { padding ->
            CountryPickerForm(
                countryPickerContent = countryPickerContent,
                onContinueClicked = viewModel::onContinueClicked,
                onCountrySelected = viewModel::onCountrySelected,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun CountryPickerForm(
    countryPickerContent: CountryPickerViewModel.CountryPickerContent,
    onContinueClicked: () -> Unit,
    onCountrySelected: (StoreCreationCountry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        ) {
            Text(
                text = countryPickerContent.storeName.uppercase(),
                style = MaterialTheme.typography.caption,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            Text(
                text = stringResource(id = R.string.store_creation_country_picker_title),
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = stringResource(id = R.string.store_creation_country_picker_description),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            AvailableCountriesList(
                countryPickerContent.countries,
                onCountrySelected = onCountrySelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
            onClick = onContinueClicked,
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }
    }
}

@Composable
private fun AvailableCountriesList(
    countries: List<StoreCreationCountry>,
    onCountrySelected: (StoreCreationCountry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(countries) { _, country ->
            CountryItem(
                country = country,
                onCountrySelected = onCountrySelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@Composable
private fun CountryItem(
    country: StoreCreationCountry,
    onCountrySelected: (StoreCreationCountry) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = dimensionResource(id = if (country.isSelected) R.dimen.minor_25 else R.dimen.minor_10),
                color = colorResource(
                    if (country.isSelected) R.color.color_primary else R.color.divider_color
                ),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            )
            .clip(shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .background(
                color = colorResource(
                    id = if (country.isSelected)
                        if (isSystemInDarkTheme()) R.color.color_surface else R.color.woo_purple_10
                    else R.color.color_surface
                )
            )
            .clickable { onCountrySelected(country) }
    ) {
        Text(
            text = country.countryName,
            color = colorResource(
                id = if (isSystemInDarkTheme() && country.isSelected) R.color.color_primary
                else R.color.color_on_surface
            ),
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_100),
                top = dimensionResource(id = R.dimen.major_75),
                bottom = dimensionResource(id = R.dimen.major_75),
                end = dimensionResource(id = R.dimen.major_100),
            )
        )
    }
}
