package com.woocommerce.android.ui.login.qrlogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R

@Composable
fun QrLoginPrologueScreen(
    onScanClicked: () -> Unit,
    onFallbackClicked: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.prologue_login_background_color))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_prologue_bg_white),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(colorResource(id = R.color.prologue_login_shape_color)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(id = R.dimen.major_150))
                .padding(top = systemBarsPadding.calculateTopPadding())
                .padding(
                    bottom = navBarsPadding.calculateBottomPadding()
                        + dimensionResource(id = R.dimen.major_100)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Hero()
        }
    }
}

@Composable
private fun Hero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(id = R.dimen.major_300)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.login_qr_prologue_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colorResource(id = R.color.prologue_login_on_background),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        Text(
            text = stringResource(id = R.string.login_qr_prologue_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = colorResource(id = R.color.prologue_login_on_background_secondary),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_125)))
        Text(
            text = stringResource(id = R.string.login_qr_prologue_step_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.prologue_login_on_background_tertiary),
            textAlign = TextAlign.Center
        )
    }
}
