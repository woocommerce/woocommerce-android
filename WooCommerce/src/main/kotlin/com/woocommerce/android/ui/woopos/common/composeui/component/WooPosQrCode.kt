package com.woocommerce.android.ui.woopos.common.composeui.component

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

@Composable
fun WooPosQrCode(
    data: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(data, sizePx) { encodeToBitmap(data, sizePx) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        filterQuality = FilterQuality.None,
    )
}

private fun encodeToBitmap(data: String, sizePx: Int): Bitmap {
    if (data.isBlank()) return blankBitmap(sizePx)
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = try {
        QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    } catch (e: WriterException) {
        WooLog.e(T.POS, "WooPosQrCode: encode failed", e)
        return blankBitmap(sizePx)
    } catch (e: IllegalArgumentException) {
        WooLog.e(T.POS, "WooPosQrCode: invalid input", e)
        return blankBitmap(sizePx)
    }
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            pixels[y * sizePx + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.RGB_565)
}

private fun blankBitmap(sizePx: Int): Bitmap =
    Bitmap.createBitmap(IntArray(sizePx * sizePx) { Color.WHITE }, sizePx, sizePx, Bitmap.Config.RGB_565)
