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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.copyToClipboard
import com.woocommerce.android.extensions.findActivity
import com.woocommerce.android.extensions.windowHeightSizeClass
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.WooPermissionUtils

@Composable
fun QrLoginPrologueScreen(
    onScanClicked: () -> Unit,
    onFallbackClicked: () -> Unit,
    onCameraPermissionDialogShown: (CameraDenialState) -> Unit = {},
    onCameraPermissionDialogPrimary: (CameraDenialState) -> Unit = {},
    onCameraPermissionDialogDismissed: (CameraDenialState) -> Unit = {},
) {
    val context = LocalContext.current
    var denialState by rememberSaveable { mutableStateOf(CameraDenialState.Hidden) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                denialState = CameraDenialState.Hidden
                onScanClicked()
            } else {
                // After a denial, shouldShowRequestPermissionRationale tells us whether Android
                // will keep prompting (true → first denial) or has stopped prompting (false →
                // permanently denied or "Don't ask again"). Before the very first request this
                // returns false too, but we only reach this branch after a denial.
                val activity = context.findActivity()
                denialState = if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.CAMERA
                    )
                ) {
                    CameraDenialState.FirstDenial
                } else {
                    CameraDenialState.PermanentlyDenied
                }
            }
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
    LaunchedEffect(denialState) {
        if (denialState != CameraDenialState.Hidden) {
            onCameraPermissionDialogShown(denialState)
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
            // Hero scrolls so the fallback link below stays visible in landscape on phones
            // where the static layout would otherwise push it off the bottom edge.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Hero()
            }
            Buttons(onScanClicked = handleScanClicked, onFallbackClicked = onFallbackClicked)
        }
    }

    if (denialState != CameraDenialState.Hidden) {
        val dismissDialog = {
            val tappedState = denialState
            denialState = CameraDenialState.Hidden
            onCameraPermissionDialogDismissed(tappedState)
        }
        CameraPermissionDialog(
            state = denialState,
            onPrimary = {
                val tappedState = denialState
                denialState = CameraDenialState.Hidden
                onCameraPermissionDialogPrimary(tappedState)
                when (tappedState) {
                    CameraDenialState.FirstDenial ->
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    CameraDenialState.PermanentlyDenied ->
                        WooPermissionUtils.showAppSettings(context, openInNewStack = false)
                    CameraDenialState.Hidden -> Unit
                }
            },
            onCancel = dismissDialog,
        )
    }
}

@Composable
private fun Hero() {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Tablets have enough vertical room for the portrait stack in landscape too — only the
    // compact height bucket (phones in landscape) needs the compacted hero.
    val isCompactHeight = LocalContext.current.windowHeightSizeClass == WindowSizeClass.Compact
    if (isLandscape && isCompactHeight) HeroLandscape() else HeroPortrait()
}

@Composable
private fun HeroPortrait() {
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
 * Landscape phones only have ~400dp of vertical space, so the portrait hero pushes the bottom
 * CTAs off-screen. Pair the QR icon with the title on a single row and keep the URL line and
 * step hint horizontally centered below — everything fits without scrolling and reads as a
 * single centered block.
 */
@Composable
private fun HeroLandscape() {
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

/**
 * Two-state camera-permission fallback dialog. The copy and primary button label diverge between
 * [CameraDenialState.FirstDenial] (system will keep prompting on next request) and
 * [CameraDenialState.PermanentlyDenied] (user has to enable the permission in Settings). The
 * cancel button just closes the dialog — the prologue's own "Sign in with site address instead"
 * link is still available underneath.
 */
@Composable
private fun CameraPermissionDialog(
    state: CameraDenialState,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
) {
    val title = when (state) {
        CameraDenialState.FirstDenial -> R.string.login_qr_prologue_camera_denied_title
        CameraDenialState.PermanentlyDenied -> R.string.login_qr_prologue_camera_blocked_title
        CameraDenialState.Hidden -> return
    }
    val body = when (state) {
        CameraDenialState.FirstDenial -> R.string.login_qr_prologue_camera_denied_body
        CameraDenialState.PermanentlyDenied -> R.string.login_qr_prologue_camera_blocked_body
        CameraDenialState.Hidden -> return
    }
    val primaryLabel = when (state) {
        CameraDenialState.FirstDenial -> R.string.login_qr_prologue_camera_denied_allow_button
        CameraDenialState.PermanentlyDenied -> R.string.login_qr_prologue_camera_blocked_settings_button
        CameraDenialState.Hidden -> return
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(id = title)) },
        text = { Text(text = stringResource(id = body)) },
        confirmButton = {
            TextButton(onClick = onPrimary) {
                Text(text = stringResource(id = primaryLabel))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@LightDarkThemePreviews
@Composable
private fun QrLoginPrologueScreenPreview() {
    WooThemeWithBackground {
        QrLoginPrologueScreen(onScanClicked = {}, onFallbackClicked = {})
    }
}

@Preview(name = "First denial — light")
@Preview(name = "First denial — dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CameraPermissionFirstDenialPreview() {
    WooThemeWithBackground {
        CameraPermissionDialog(
            state = CameraDenialState.FirstDenial,
            onPrimary = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Permanently denied — light")
@Preview(name = "Permanently denied — dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CameraPermissionPermanentlyDeniedPreview() {
    WooThemeWithBackground {
        CameraPermissionDialog(
            state = CameraDenialState.PermanentlyDenied,
            onPrimary = {},
            onCancel = {},
        )
    }
}
