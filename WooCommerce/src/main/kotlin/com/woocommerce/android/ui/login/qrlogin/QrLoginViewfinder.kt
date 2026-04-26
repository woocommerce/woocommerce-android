package com.woocommerce.android.ui.login.qrlogin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import kotlin.math.min

private val ScrimColor = Color(0xB3000000)
private val CornerColor = Color(0xFFB475F0)
private const val FRAME_FRACTION = 0.62f
private val CornerStroke = 4.dp
private val CornerArm = 28.dp
private val FrameRadius = 24.dp

@Composable
fun QrLoginViewfinder() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1 (bottom): scrim + brackets, drawn directly on the camera preview.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = min(size.width, size.height) * FRAME_FRACTION
            val left = (size.width - side) / 2f
            val top = (size.height - side) / 2f
            val radiusPx = FrameRadius.toPx()
            val cornerStrokePx = CornerStroke.toPx()
            val arm = CornerArm.toPx()

            // Scrim with a rounded-rect hole. EvenOdd fill creates the cutout: the outer
            // rectangle is filled, the inner rounded rect punches through.
            val scrim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset(left, top), Size(side, side)),
                        cornerRadius = CornerRadius(radiusPx)
                    )
                )
            }
            drawPath(path = scrim, color = ScrimColor)

            // Bold corner brackets
            val r = left + side
            val b = top + side
            val corners = Path().apply {
                // top-left
                moveTo(left, top + arm)
                lineTo(left, top + radiusPx)
                quadraticTo(left, top, left + radiusPx, top)
                lineTo(left + arm, top)
                // top-right
                moveTo(r - arm, top)
                lineTo(r - radiusPx, top)
                quadraticTo(r, top, r, top + radiusPx)
                lineTo(r, top + arm)
                // bottom-right
                moveTo(r, b - arm)
                lineTo(r, b - radiusPx)
                quadraticTo(r, b, r - radiusPx, b)
                lineTo(r - arm, b)
                // bottom-left
                moveTo(left + arm, b)
                lineTo(left + radiusPx, b)
                quadraticTo(left, b, left, b - radiusPx)
                lineTo(left, b - arm)
            }
            drawPath(
                path = corners,
                color = CornerColor,
                style = Stroke(width = cornerStrokePx)
            )
        }

        // Layer 2 (top): the URL pill, rendered on top of the scrim so its background isn't
        // dimmed by the overlay.
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            VisitUrlPill()
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun VisitUrlPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            text = stringResource(
                id = R.string.login_qr_scanner_visit_url,
                stringResource(id = R.string.login_qr_prologue_url)
            ),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
