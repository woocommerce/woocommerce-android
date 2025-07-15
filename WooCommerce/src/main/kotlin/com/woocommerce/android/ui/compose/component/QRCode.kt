package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun QRCode(
    content: String,
    size: Dp,
    @DrawableRes overlayId: Int? = null,
) {
    Image(
        modifier = Modifier
            .size(size)
            .border(
                width = 5.dp,
                color = colorResource(id = R.color.woo_white),
            )
            .padding(4.dp)
            .background(
                color = colorResource(id = R.color.woo_white)
            ),
        painter = rememberBarcodeBitmapPainter(
            content,
            barcodeFormat = BarcodeFormat.QR_CODE,
            widthDp = size,
            heightDp = size,
            overlayId = overlayId,
        ),
        contentDescription = "QR Code",
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
fun BarcodeEAN13Code(
    content: String,
    widthDp: Dp,
    heightDp: Dp,
    codeColor: Color,
    backgroundColor: Color,
    @DrawableRes overlayId: Int? = null,
) {
    Image(
        modifier = Modifier
            .size(widthDp, heightDp)
            .border(
                width = 5.dp,
                color = backgroundColor,
            )
            .padding(4.dp)
            .background(
                color = backgroundColor
            ),
        painter = rememberBarcodeBitmapPainter(
            content,
            barcodeFormat = BarcodeFormat.EAN_13,
            widthDp = widthDp,
            heightDp = heightDp,
            codeColor = codeColor,
            overlayId = overlayId,
        ),
        contentDescription = stringResource(id = R.string.barcode_ean13_content_description),
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
private fun rememberBarcodeBitmapPainter(
    content: String,
    barcodeFormat: BarcodeFormat,
    widthDp: Dp,
    heightDp: Dp,
    codeColor: Color = colorResource(id = R.color.woo_black_90),
    @DrawableRes overlayId: Int?,
): BitmapPainter {
    val density = LocalDensity.current
    val widthPx = with(density) { widthDp.roundToPx() }
    val heightPx = with(density) { heightDp.roundToPx() }

    var bitmap by remember(content) { mutableStateOf<Bitmap?>(null) }

    val pixelColor = codeColor.toArgb()

    LaunchedEffect(bitmap) {
        if (bitmap != null) return@LaunchedEffect

        launch(Dispatchers.IO) {
            val newBitmap = generateCode(content, barcodeFormat, widthPx, heightPx, pixelColor)

            bitmap = newBitmap
        }
    }

    val context = LocalContext.current
    return remember(bitmap) {
        val overlay = overlayId?.let {
            ContextCompat.getDrawable(context, overlayId)?.run {
                toBitmap(intrinsicWidth, intrinsicHeight)
            }
        }
        val currentBitmap = bitmap ?: createBitmap(widthPx, heightPx)
            .apply {
                eraseColor(android.graphics.Color.TRANSPARENT)
            }
        BitmapPainter(
            if (overlay != null) {
                currentBitmap.addOverlayToCenter(overlay).asImageBitmap()
            } else {
                currentBitmap.asImageBitmap()
            }
        )
    }
}

private fun generateCode(
    content: String,
    barcodeFormat: BarcodeFormat,
    widthPx: Int,
    heightPx: Int,
    pixelColor: Int
): Bitmap {
    val bitmapMatrix = try {
        MultiFormatWriter().encode(
            content,
            barcodeFormat,
            widthPx,
            heightPx,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 0,
            )
        )
    } catch (ex: WriterException) {
        WooLog.e(WooLog.T.CARD_READER, "Error generating QR code", ex)
        null
    }

    val matrixWidth = bitmapMatrix?.width ?: widthPx
    val matrixHeight = bitmapMatrix?.height ?: heightPx

    val newBitmap = createBitmap(bitmapMatrix?.width ?: widthPx, bitmapMatrix?.height ?: heightPx)

    for (x in 0 until matrixWidth) {
        for (y in 0 until matrixHeight) {
            val shouldColorPixel = bitmapMatrix?.get(x, y) ?: false
            if (!shouldColorPixel) continue
            newBitmap[x, y] = pixelColor
        }
    }
    return newBitmap
}

private fun Bitmap.addOverlayToCenter(overlayBitmap: Bitmap): Bitmap {
    val bitmap2Width = overlayBitmap.width
    val bitmap2Height = overlayBitmap.height
    val marginLeft = (width * HALF - bitmap2Width * HALF).toFloat()
    val marginTop = (height * HALF - bitmap2Height * HALF).toFloat()
    val canvas = Canvas(this)
    canvas.drawBitmap(this, Matrix(), null)
    canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, Paint().apply { alpha = OVERLAY_ALPHA })
    return this
}

private const val HALF = 0.5
private const val OVERLAY_ALPHA = 230

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun QRCodePreview() {
    WooThemeWithBackground {
        QRCode(
            content = "https://woocommerce.com",
            size = 150.dp,
            overlayId = R.drawable.img_woo_white
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EAN13CodePreview() {
    WooThemeWithBackground {
        BarcodeEAN13Code(
            content = "https://woocommerce.com",
            widthDp = 150.dp,
            heightDp = 80.dp,
            overlayId = R.drawable.img_woo_white,
            codeColor = Color.Black,
            backgroundColor = Color.White
        )
    }
}
