package com.woocommerce.android.ui.login.storecreation.domainpicker

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.domainpicker.SiteDomainPickerViewModel.DomainSuggestion
import com.woocommerce.android.ui.login.storecreation.domainpicker.SiteDomainPickerViewModel.SiteDomainPickerState

@Composable
fun SiteDomainPickerScreen(viewModel: SiteDomainPickerViewModel) {
    viewModel.viewState.observeAsState(SiteDomainPickerState()).value.let { viewState ->
        Scaffold(topBar = {
            Toolbar(onArrowBackPressed = viewModel::onBackPressed)
        }) {
            SiteDomainSearchForm(
                state = viewState,
                onDomainQueryChanged = viewModel::onDomainChanged,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Toolbar(
    onArrowBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            IconButton(onClick = onArrowBackPressed) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
private fun SiteDomainSearchForm(
    state: SiteDomainPickerState,
    onDomainQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(id = R.dimen.major_125)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_domain_picker_title),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(id = R.string.store_creation_domain_picker_subtitle),
            style = MaterialTheme.typography.body1,
        )
        WCSearchField(
            value = state.domain,
            onValueChange = onDomainQueryChanged,
            hint = stringResource(id = R.string.store_creation_domain_picker_hint),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(
                    BorderStroke(
                        width = dimensionResource(id = R.dimen.minor_10),
                        color = colorResource(id = R.color.gray_5)
                    ),
                    RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
                ),
            backgroundColor = TextFieldDefaults.outlinedTextFieldColors().backgroundColor(enabled = true).value
        )
        DomainSuggestionList(
            suggestions = state.domainSuggestions, isLoading = state.isLoading
        )
    }
}

@Composable
private fun DomainSuggestionList(suggestions: List<DomainSuggestion>, isLoading: Boolean) {

}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun SiteDomainPickerPreview() {
    WooThemeWithBackground {
        SiteDomainSearchForm(
            state = SiteDomainPickerState(),
            onDomainQueryChanged = {}
        )
    }
}

