package com.woocommerce.android.ui.woopos.common.composeui.component

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun WooPosQrCode(
    data: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    centerLogoResId: Int? = null,
) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val errorCorrection = if (centerLogoResId != null) ErrorCorrectionLevel.H else ErrorCorrectionLevel.M
    val bitmap = remember(data, sizePx, errorCorrection) { encodeToBitmap(data, sizePx, errorCorrection) }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
            // QR codes are inherently pixelated; disable filtering so edges stay sharp.
            filterQuality = FilterQuality.None,
        )
        if (centerLogoResId != null) {
            val logoSize = size * CENTER_LOGO_RATIO
            Image(
                painter = painterResource(id = centerLogoResId),
                contentDescription = null,
                modifier = Modifier
                    .size(logoSize)
                    .clip(RoundedCornerShape(LOGO_CORNER_RADIUS))
                    .background(White)
                    .padding(LOGO_INNER_PADDING),
            )
        }
    }
}

private fun encodeToBitmap(data: String, sizePx: Int, errorCorrection: ErrorCorrectionLevel): Bitmap {
    val hints = mapOf(
        com.google.zxing.EncodeHintType.ERROR_CORRECTION to errorCorrection,
        com.google.zxing.EncodeHintType.MARGIN to 1,
    )
    val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            pixels[y * sizePx + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.RGB_565)
}

private const val CENTER_LOGO_RATIO = 0.22f
private val LOGO_CORNER_RADIUS = 8.dp
private val LOGO_INNER_PADDING = 4.dp
