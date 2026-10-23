package com.woocommerce.android.ui.barcodescanner

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BarcodeEAN13Code
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.compose.component.dismissWCModalBottomSheet
import com.woocommerce.android.ui.compose.theme.WooTheme
import kotlinx.coroutines.delay

private const val TEST_BARCODE_EAN13 = "4006381333931"
private const val TEST_SCAN_SUCCESS_DISPLAY_MS = 3000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareBarcodeScannerInfoSheet(onDismissRequest: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isTestScanSuccessful by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isTestScanSuccessful) {
        if (isTestScanSuccessful) {
            delay(TEST_SCAN_SUCCESS_DISPLAY_MS)
            dismissWCModalBottomSheet(coroutineScope, sheetState, onDismissRequest)
        }
    }
    WCModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        HardwareBarcodeScannerInfoContent(
            isTestScanSuccessful = isTestScanSuccessful,
            modifier = Modifier.listenForBarcodes(
                onBarcodeEvent = { result ->
                    if (result is BarcodeInputDetector.BarcodeResult.Success) {
                        isTestScanSuccessful = true
                    }
                },
                enabled = !isTestScanSuccessful,
            ),
        )
    }
}

@Composable
private fun HardwareBarcodeScannerInfoContent(
    isTestScanSuccessful: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(id = R.dimen.major_150))
            .padding(bottom = dimensionResource(id = R.dimen.major_150)),
    ) {
        Text(
            text = stringResource(R.string.barcode_scanning_hardware_scanner_info_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(R.string.barcode_scanning_hardware_scanner_info_setup),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_75)))
        Text(
            text = stringResource(R.string.barcode_scanning_hardware_scanner_info_usage),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
        Text(
            text = stringResource(R.string.barcode_scanning_hardware_scanner_info_test_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = stringResource(R.string.barcode_scanning_hardware_scanner_info_test_instruction),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        AnimatedContent(
            modifier = Modifier
                .fillMaxWidth()
                .height(TEST_AREA_HEIGHT),
            targetState = isTestScanSuccessful,
            transitionSpec = {
                (fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                    .togetherWith(fadeOut())
            },
            contentAlignment = Alignment.Center,
            label = "hardwareScannerTestArea",
        ) { isSuccessful ->
            when (isSuccessful) {
                true -> TestScanSuccess()
                false -> TestBarcode()
            }
        }
    }
}

@Composable
private fun TestScanSuccess() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            modifier = Modifier.size(TEST_SUCCESS_ICON_SIZE),
            imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle_filled_24dp),
            contentDescription = null,
            tint = colorResource(id = R.color.woo_green_50),
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = stringResource(R.string.barcode_scanning_hardware_scanner_info_test_success),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun TestBarcode() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorResource(id = R.color.woo_white),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)),
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_150),
                vertical = dimensionResource(id = R.dimen.major_100),
            ),
        contentAlignment = Alignment.Center,
    ) {
        BarcodeEAN13Code(
            content = TEST_BARCODE_EAN13,
            widthDp = maxWidth,
            heightDp = TEST_BARCODE_HEIGHT,
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HardwareBarcodeScannerInfoContentPreview() {
    WooTheme {
        HardwareBarcodeScannerInfoContent(isTestScanSuccessful = false)
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HardwareBarcodeScannerInfoContentSuccessPreview() {
    WooTheme {
        HardwareBarcodeScannerInfoContent(isTestScanSuccessful = true)
    }
}

private val TEST_AREA_HEIGHT = 120.dp
private val TEST_BARCODE_HEIGHT = 88.dp
private val TEST_SUCCESS_ICON_SIZE = 64.dp
