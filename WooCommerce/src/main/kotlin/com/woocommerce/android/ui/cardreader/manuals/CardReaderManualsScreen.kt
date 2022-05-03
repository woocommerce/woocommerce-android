package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.cardreader.manuals.CardReaderManualsViewModel

@Composable
fun ManualsScreen(
    cardReaderManualsViewModel: CardReaderManualsViewModel = viewModel()
) {
    ManualsList(
        list = cardReaderManualsViewModel.manualState
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
            modifier = Modifier
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.minor_100)
                )
        )
        Column(
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .align(Alignment.CenterVertically)
        ) {
            Text(text = manualLabel)
        }
    }
}

@Composable
fun ManualsList(
    list: List<CardReaderManualsViewModel.ManualItem>
) {
    LazyColumn(
        modifier = Modifier
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
                modifier = Modifier.offset(x = dimensionResource(id = R.dimen.major_100)),
                color = colorResource(id = R.color.divider_color),
                thickness = dimensionResource(id = R.dimen.minor_10)
            )
        }
    }
}
