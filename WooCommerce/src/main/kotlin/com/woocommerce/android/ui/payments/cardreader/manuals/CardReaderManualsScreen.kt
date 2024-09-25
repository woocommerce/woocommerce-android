package com.woocommerce.android.ui.payments.cardreader.manuals

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun CardReaderManualsScaffold(
    viewModel: CardReaderManualsViewModel,
    navController: NavController
) {
    WooThemeWithBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_card_reader_manuals)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back_24dp),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    backgroundColor = colorResource(id = R.color.color_toolbar),
                )
            }
        ) { innerPadding ->
            CardReaderManualsScreen(cardReaderManualsViewModel = viewModel, contentPadding = innerPadding)
        }
    }
}

@Composable
fun CardReaderManualsScreen(
    cardReaderManualsViewModel: CardReaderManualsViewModel = viewModel(),
    contentPadding: PaddingValues
) {
    ManualsList(
        list = cardReaderManualsViewModel.manualState,
        modifier = Modifier.padding(contentPadding)
    )
}

@Composable
fun ManualListItem(
    manualLabel: String,
    manualIcon: Int,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClickLabel = null,
                role = Role.Button,
                onClick = onManualClick
            )

    ) {
        Image(
            painterResource(manualIcon),
            contentDescription = stringResource(R.string.card_reader_icon_content_description),
            modifier
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.minor_100)
                )
        )
        Column(
            modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .align(Alignment.CenterVertically)
        ) {
            Text(text = manualLabel)
        }
    }
}

@Composable
fun ManualsList(
    list: List<CardReaderManualsViewModel.ManualItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier
            .background(color = MaterialTheme.colors.surface)

    ) {
        items(
            items = list
        ) { manual ->
            ManualListItem(
                manualLabel = stringResource(id = manual.label),
                manualIcon = manual.icon,
                onManualClick = manual.onManualClicked
            )
            Divider(
                modifier = Modifier.offset(dimensionResource(id = R.dimen.card_reader_manuals_divider)),
                color = colorResource(id = R.color.divider_color),
                thickness = dimensionResource(id = R.dimen.minor_10)
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ManualsList(
        list = listOf(
            CardReaderManualsViewModel.ManualItem(
                icon = R.drawable.ic_chipper_reader,
                label = R.string.card_reader_bbpos_manual_card_reader,
                onManualClicked = { }
            ),
            CardReaderManualsViewModel.ManualItem(
                icon = R.drawable.ic_m2_reader,
                label = R.string.card_reader_m2_manual_card_reader,
                onManualClicked = { }
            ),
            CardReaderManualsViewModel.ManualItem(
                icon = R.drawable.ic_wisepad3_reader,
                label = R.string.card_reader_wisepad_3_manual_card_reader,
                onManualClicked = { }
            )
        )
    )
}
