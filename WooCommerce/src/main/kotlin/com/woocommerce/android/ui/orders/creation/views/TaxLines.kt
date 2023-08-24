package com.woocommerce.android.ui.orders.creation.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun TaxLines(taxLines: List<TaxLineUiModel>?) {
    Column {
        taxLines?.forEach { taxLine ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${taxLine.label} · ${taxLine.ratePercent}",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.color_on_surface_medium),
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = taxLine.taxTotal,
                    style = MaterialTheme.typography.body2,
                    color = colorResource(id = R.color.color_on_surface_medium),
                )
            }
        }
    }
}

data class TaxLineUiModel(
    val label: String,
    val ratePercent: String,
    val taxTotal: String,
)
