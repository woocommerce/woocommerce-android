package com.woocommerce.android.ui.login.qrlogin

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.copyToClipboard
import com.woocommerce.android.extensions.windowHeightSizeClass
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun QrLoginPrologueScreen(
    onScanClicked: () -> Unit,
    onFallbackClicked: () -> Unit,
) {
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            // The scanner asks for the permission again on first composition; if the user just
            // granted it here, that follow-up returns immediately with no extra dialog.
            // If the user denied, we stay on the prologue — they can tap again to retry, or
            // pick the site-URL fallback. We don't navigate to a black scanner-with-permission-
            // prompt screen.
            if (granted) onScanClicked()
        }
    )
    val handleScanClicked: () -> Unit = {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            onScanClicked()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
        ) {
            // Content scrolls so the fallback link below stays visible in landscape on phones
            // where the static layout would otherwise push it off the bottom edge.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Content()
            }
            Buttons(onScanClicked = handleScanClicked, onFallbackClicked = onFallbackClicked)
        }
    }
}

@Composable
private fun Content() {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Tablets have enough vertical room for the portrait stack in landscape too — only the
    // compact height bucket (phones in landscape) needs the compacted layout.
    val isCompactHeight = LocalContext.current.windowHeightSizeClass == WindowSizeClass.Compact
    if (isLandscape && isCompactHeight) ContentLandscape() else ContentPortrait()
}

@Composable
private fun ContentPortrait() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(id = R.dimen.major_300)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QrIconBadge()
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_200)))
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
        UrlBadge()
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_125)))
        Text(
            text = stringResource(id = R.string.login_qr_prologue_step_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.prologue_login_on_background_tertiary),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Landscape phones only have ~400dp of vertical space, so the portrait layout pushes the bottom
 * CTAs off-screen. Pair the QR icon with the title on a single row and keep the URL line and
 * step hint horizontally centered below — everything fits without scrolling and reads as a
 * single centered block.
 */
@Composable
private fun ContentLandscape() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QrIconBadge(
                size = dimensionResource(id = R.dimen.image_major_50),
                iconSize = dimensionResource(id = R.dimen.image_minor_80),
            )
            Text(
                text = stringResource(id = R.string.login_qr_prologue_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colorResource(id = R.color.prologue_login_on_background),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.login_qr_prologue_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colorResource(id = R.color.prologue_login_on_background_secondary)
            )
            UrlBadge()
        }
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        Text(
            text = stringResource(id = R.string.login_qr_prologue_step_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.prologue_login_on_background_tertiary),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QrIconBadge(
    size: Dp = dimensionResource(id = R.dimen.image_major_72),
    iconSize: Dp = dimensionResource(id = R.dimen.image_minor_100),
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colorResource(id = R.color.prologue_login_url_badge_background)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_qr_code_scanner),
            contentDescription = null,
            tint = colorResource(id = R.color.prologue_login_on_background),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun UrlBadge() {
    val context = LocalContext.current
    val url = stringResource(id = R.string.login_qr_prologue_url)
    val clipboardLabel = stringResource(id = R.string.login_qr_prologue_url_clipboard_label)
    val copiedMessage = stringResource(id = R.string.login_qr_prologue_url_copied)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.major_75)))
            .background(colorResource(id = R.color.prologue_login_url_badge_background))
            .clickable {
                context.copyToClipboard(clipboardLabel, url)
                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
            }
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_125),
                vertical = dimensionResource(id = R.dimen.major_85)
            )
    ) {
        Text(
            text = url,
            style = MaterialTheme.typography.titleLarge,
            color = colorResource(id = R.color.prologue_login_on_background),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Buttons(
    onScanClicked: () -> Unit,
    onFallbackClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // White-on-purple to stand out from the purple prologue background. The default
        // WCColoredButton uses the primary purple as its container, which blends into the
        // background in light mode. Mirrors the existing legacy prologue's
        // `Woo.Button.Colored.White` style.
        WCColoredButton(
            onClick = onScanClicked,
            text = stringResource(id = R.string.login_qr_prologue_scan_button),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.prologue_login_button_color),
                contentColor = colorResource(id = R.color.color_on_surface),
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        WCTextButton(
            onClick = onFallbackClicked,
            contentPadding = PaddingValues(vertical = dimensionResource(id = R.dimen.major_75)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.login_qr_prologue_fallback_link),
                color = colorResource(id = R.color.prologue_login_on_background),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun QrLoginPrologueScreenPreview() {
    WooThemeWithBackground {
        QrLoginPrologueScreen(onScanClicked = {}, onFallbackClicked = {})
    }
}
