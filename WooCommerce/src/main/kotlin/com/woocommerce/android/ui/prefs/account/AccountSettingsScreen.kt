package com.woocommerce.android.ui.prefs.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun AccountSettingsScreen(
    onCloseAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        Divider()
        AccountSettingsItem(
            title = stringResource(id = R.string.settings_close_account).uppercase(),
            onClick = onCloseAccountClick,
            textColor = colorResource(id = R.color.woo_pink_50),
            boldText = true,
            modifier = Modifier.fillMaxWidth()
        )
        Divider()
    }
}

@Composable
private fun AccountSettingsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    boldText: Boolean = false,
    textColor: Color = LocalContentColor.current
) {
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                start = dimensionResource(id = R.dimen.major_375),
                top = dimensionResource(id = R.dimen.major_100),
                bottom = dimensionResource(id = R.dimen.major_100),
                end = dimensionResource(id = R.dimen.major_100)
            )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = if (boldText) FontWeight.Medium else null,
            color = textColor.let {
                if (!enabled) it.copy(alpha = ContentAlpha.disabled) else it
            }
        )
        subtitle?.let {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                fontWeight = if (boldText) FontWeight.Medium else null,
                color = textColor.let {
                    if (!enabled) it.copy(alpha = ContentAlpha.disabled) else it
                }
            )
        }
    }
}

@Composable
@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AccountSettingsScreenPreview() {
    WooThemeWithBackground {
        AccountSettingsScreen(onCloseAccountClick = {})
    }
}
