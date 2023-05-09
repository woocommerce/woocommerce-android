package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.woocommerce.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun QRCode(
    content: String,
    size: Dp,
    padding: Dp,
) {
    Image(
        painter = rememberQrBitmapPainter(
            content,
            size = size,
            padding = padding,
        ),
        contentDescription = "QR Code",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun rememberQrBitmapPainter(
    content: String,
    size: Dp,
    padding: Dp,
): BitmapPainter {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }
    val paddingPx = with(density) { padding.roundToPx() }

    var bitmap by remember(content) { mutableStateOf<Bitmap?>(null) }

    val pixelColor = colorResource(id = R.color.color_on_surface).toArgb()

    LaunchedEffect(bitmap) {
        if (bitmap != null) return@LaunchedEffect

        launch(Dispatchers.IO) {
            val qrCodeWriter = QRCodeWriter()

            val encodeHints = mutableMapOf<EncodeHintType, Any?>()
                .apply {
                    this[EncodeHintType.MARGIN] = paddingPx
                }

            val bitmapMatrix = try {
                qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, encodeHints)
            } catch (ex: WriterException) {
                null
            }

            val matrixWidth = bitmapMatrix?.width ?: sizePx
            val matrixHeight = bitmapMatrix?.height ?: sizePx

            val newBitmap = Bitmap.createBitmap(
                bitmapMatrix?.width ?: sizePx,
                bitmapMatrix?.height ?: sizePx,
                Bitmap.Config.ARGB_8888,
            )

            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                    val shouldColorPixel = bitmapMatrix?.get(x, y) ?: false
                    if (!shouldColorPixel) continue
                    newBitmap.setPixel(x, y, pixelColor)
                }
            }

            bitmap = newBitmap
        }
    }

    return remember(bitmap) {
        val currentBitmap = bitmap ?: Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            .apply {
                eraseColor(Color.TRANSPARENT)
            }

        BitmapPainter(currentBitmap.asImageBitmap())
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun QRCodePreview() {
    QRCode(
        content = "https://woocommerce.com",
        size = 150.dp,
        padding = 5.dp,
    )
}
