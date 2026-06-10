package com.woocommerce.android.ui.login

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R

@Composable
fun HelpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(dimensionResource(id = R.dimen.minor_50)),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_help_24dp),
            contentDescription = stringResource(id = R.string.help),
            tint = tint,
        )
    }
}
