package com.woocommerce.android.ui.customer.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
internal fun CustomerSummaryRow(
    name: String,
    email: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    username: String? = null,
) {
    CustomerSummaryRow(
        name = AnnotatedString(name),
        email = AnnotatedString(email),
        onClick = onClick,
        modifier = modifier,
        username = username?.let(::AnnotatedString),
    )
}

@Composable
internal fun CustomerSummaryRow(
    name: AnnotatedString,
    email: AnnotatedString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    username: AnnotatedString? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100),
            )
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = name,
                    color = colorResource(id = R.color.color_on_surface),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.W500,
                )
                username?.let {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = it,
                        color = colorResource(id = R.color.color_on_surface_medium),
                        style = MaterialTheme.typography.subtitle1,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = email,
                color = colorResource(id = R.color.color_on_surface),
                style = MaterialTheme.typography.body2,
            )
        }
    }
}
