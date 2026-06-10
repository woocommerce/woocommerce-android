@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.woopos.common.composeui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Suppress("LongMethod", "LargeClass")
object WooPosIcons {
    val NotFound: ImageVector
        @Composable
        get() = notFound(
            primaryColor = MaterialTheme.colorScheme.secondary,
            secondaryColor = MaterialTheme.colorScheme.primary,
            tertiaryColor = WooPosTheme.colors.tertiaryIconColor
        )

    val Check: ImageVector
        @Composable
        get() = check(
            color = MaterialTheme.colorScheme.onSurface
        )

    val ErrorX: ImageVector
        @Composable
        get() = errorX(
            primaryColor = MaterialTheme.colorScheme.secondary,
            secondaryColor = MaterialTheme.colorScheme.primary,
            tertiaryColor = WooPosTheme.colors.tertiaryIconColor
        )

    val OrdersEmpty: ImageVector
        @Composable
        get() = ordersEmpty(
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            tertiaryColor = WooPosTheme.colors.tertiaryIconColor
        )

    val BluetoothSettings: ImageVector
        @Composable
        get() = bluetoothSettings(
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            tertiaryColor = WooPosTheme.colors.tertiaryIconColor
        )

    val CouponsEmpty: ImageVector
        @Composable
        get() = couponsEmpty(
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            tertiaryColor = WooPosTheme.colors.tertiaryIconColor
        )

    val CardReaderScanning: ImageVector
        @Composable
        get() = if (isSystemInDarkTheme()) {
            cardReaderWithNfcDark(
                blobColor = Color(0xFFB280FF).copy(alpha = 0.5f),
                readerBodyColor = Color(0xFFBEA0F2),
                readerFaceColor = Color(0xFFDFD1FB),
                nfcColor = Color(0xFF966CCF)
            )
        } else {
            cardReaderWithNfc(
                blobColor = Color(0xFFAD86E9),
                readerBodyColor = Color(0xFFDFD1FB),
                readerFaceColor = Color(0xFFF2EDFF),
                nfcColor = Color(0xFF966CCF)
            )
        }

    val CardReaderFound: ImageVector
        @Composable
        get() = if (isSystemInDarkTheme()) {
            cardReaderWithNfcDark(
                blobColor = Color(0xFF9252E0).copy(alpha = 0.5f),
                readerBodyColor = Color(0xFFBEA0F2),
                readerFaceColor = Color(0xFFDFD1FB),
                nfcColor = Color(0xFF966CCF)
            )
        } else {
            cardReaderWithNfc(
                blobColor = Color(0xFF7F54B3),
                readerBodyColor = Color(0xFFDFD1FB),
                readerFaceColor = Color(0xFFF2EDFF),
                nfcColor = Color(0xFF966CCF)
            )
        }

    val CardReaderConnecting: ImageVector
        @Composable
        get() = if (isSystemInDarkTheme()) {
            cardReaderWithNfcDark(
                blobColor = Color(0xFFBE99FF).copy(alpha = 0.5f),
                readerBodyColor = Color(0xFFBEA0F2),
                readerFaceColor = Color(0xFFDFD1FB),
                nfcColor = Color(0xFFCFB9F6)
            )
        } else {
            cardReaderWithNfc(
                blobColor = Color(0xFFBEA0F2),
                readerBodyColor = Color(0xFFDFD1FB),
                readerFaceColor = Color(0xFFF2EDFF),
                nfcColor = Color(0xFFCFB9F6)
            )
        }

    val CardReaderSuccess: ImageVector
        @Composable
        get() = if (isSystemInDarkTheme()) {
            cardReaderWithNfcDark(
                blobColor = Color(0xFF66EA87).copy(alpha = 0.5f),
                readerBodyColor = Color(0xFF9EFAAD),
                readerFaceColor = Color(0xFFDBFFDB),
                nfcColor = Color(0xFF18CD82)
            )
        } else {
            cardReaderWithNfc(
                blobColor = Color(0xFF66EA87),
                readerBodyColor = Color(0xFF9EFAAD),
                readerFaceColor = Color(0xFFDBFFDB),
                nfcColor = Color(0xFF18CD82)
            )
        }

    val CardReaderError: ImageVector
        @Composable
        get() = if (isSystemInDarkTheme()) {
            cardReaderErrorDark(
                blobColor = Color(0xFFFFB31A).copy(alpha = 0.5f),
                readerBodyColor = Color(0xFFBD460A),
                readerFaceColor = Color(0xFFF5C13D),
                exclamationColor = Color(0xFFBD4200)
            )
        } else {
            cardReaderError(
                blobColor = Color(0xFFFFEE86),
                readerBodyColor = Color(0xFFFF7122),
                readerFaceColor = Color(0xFFFFBE16),
                exclamationColor = Color(0xFFBD4200)
            )
        }

    val CardReaderBatteryLow: ImageVector
        @Composable
        get() = if (isSystemInDarkTheme()) {
            cardReaderBatteryDark(
                blobColor = Color(0xFF85857A).copy(alpha = 0.5f),
                readerBodyColor = Color(0xFF82807D),
                readerFaceColor = Color(0xFFBAB7B2),
                batteryColor = Color.Black,
                batteryLevelColor = Color(0xFFFF7122)
            )
        } else {
            cardReaderBattery(
                blobColor = Color(0xFFE7E7E5),
                readerBodyColor = Color(0xFF767471),
                readerFaceColor = Color(0xFFBAB7B2),
                batteryColor = Color.White,
                batteryLevelColor = Color(0xFFFF7122)
            )
        }

    val CardReaderNotConnected: ImageVector
        @Composable
        get() = cardReaderNotConnected(
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            tertiaryColor = WooPosTheme.colors.tertiaryIconColor
        )

    private fun notFound(
        primaryColor: Color,
        secondaryColor: Color,
        tertiaryColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "NotFound",
            defaultWidth = 138.dp,
            defaultHeight = 138.dp,
            viewportWidth = 138f,
            viewportHeight = 138f
        ).apply {
            path(fill = SolidColor(secondaryColor)) {
                moveTo(88.0801f, 29.5267f)
                lineTo(84.0311f, 33.5911f)
                lineTo(104.299f, 53.936f)
                lineTo(108.348f, 49.8715f)
                lineTo(88.0801f, 29.5267f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(104.299f, 29.5315f)
                lineTo(84.0311f, 49.8763f)
                lineTo(88.0801f, 53.9407f)
                lineTo(108.348f, 33.5959f)
                lineTo(104.299f, 29.5315f)
                close()
            }
            path(fill = SolidColor(primaryColor)) {
                moveTo(66.4052f, 63.1985f)
                lineTo(18.9973f, 110.787f)
                lineTo(27.1043f, 118.925f)
                lineTo(74.5122f, 71.3364f)
                lineTo(66.4052f, 63.1985f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(26.2421f, 134.097f)
                curveTo(32.2103f, 127.333f, 63.9366f, 90.8947f, 63.9366f, 90.8947f)
                lineTo(46.9174f, 73.8106f)
                curveTo(46.9174f, 73.8106f, 10.6179f, 105.664f, 3.87899f, 111.655f)
                curveTo(-2.24205f, 117.096f, -0.624212f, 123.081f, 5.25479f, 128.982f)
                lineTo(8.98092f, 132.723f)
                curveTo(14.8599f, 138.624f, 20.8217f, 140.248f, 26.2421f, 134.104f)
                verticalLineTo(134.097f)
                close()
            }
            path(fill = SolidColor(primaryColor)) {
                moveTo(96.4244f, 83.4706f)
                curveTo(73.5007f, 83.4706f, 54.851f, 64.7496f, 54.851f, 41.7385f)
                curveTo(54.851f, 18.7273f, 73.5007f, 0f, 96.4244f, 0f)
                curveTo(119.348f, 0f, 137.998f, 18.7209f, 137.998f, 41.7321f)
                curveTo(137.998f, 64.7432f, 119.348f, 83.4641f, 96.4244f, 83.4641f)
                verticalLineTo(83.4706f)
                close()
                moveTo(96.4244f, 11.5088f)
                curveTo(79.8192f, 11.5088f, 66.316f, 25.0699f, 66.316f, 41.7321f)
                curveTo(66.316f, 58.3942f, 79.8256f, 71.9554f, 96.4244f, 71.9554f)
                curveTo(113.023f, 71.9554f, 126.533f, 58.3942f, 126.533f, 41.7321f)
                curveTo(126.533f, 25.0699f, 113.023f, 11.5088f, 96.4244f, 11.5088f)
                close()
            }
            path(fill = SolidColor(tertiaryColor)) {
                moveTo(5.25479f, 128.879f)
                lineTo(8.98092f, 132.619f)
                curveTo(14.8599f, 138.521f, 20.8217f, 140.145f, 26.2421f, 134f)
                curveTo(32.2103f, 127.236f, 63.9366f, 90.7976f, 63.9366f, 90.7976f)
                lineTo(0.394897f, 121.552f)
                curveTo(1.12102f, 123.975f, 2.82166f, 126.436f, 5.25479f, 128.872f)
                verticalLineTo(128.879f)
                close()
            }
        }.build()
    }

    private fun check(color: Color): ImageVector {
        return ImageVector.Builder(
            name = "Check",
            defaultWidth = 72.dp,
            defaultHeight = 78.dp,
            viewportWidth = 72f,
            viewportHeight = 78f
        ).apply {
            path(fill = SolidColor(color)) {
                moveTo(26.5f, 77.1f)
                lineTo(0.81f, 38.21f)
                lineTo(8.32f, 33.25f)
                lineTo(26.3f, 60.47f)
                lineTo(64.08f, 0.19f)
                lineTo(71.71f, 4.97f)
                lineTo(26.5f, 77.1f)
                close()
            }
        }.build()
    }

    private fun errorX(
        primaryColor: Color,
        secondaryColor: Color,
        tertiaryColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "ErrorX",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 80f,
            viewportHeight = 80f
        ).apply {
            path(fill = SolidColor(primaryColor)) {
                moveTo(40f, 0f)
                curveTo(17.921f, 0f, 0f, 17.921f, 0f, 40f)
                reflectiveCurveToRelative(17.921f, 40f, 40f, 40f)
                reflectiveCurveToRelative(40f, -17.921f, 40f, -40f)
                reflectiveCurveTo(62.12f, 0f, 40f, 0f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(58.89f, 22.87f)
                lineToRelative(-1.76f, -1.76f)
                curveToRelative(-1.913f, -1.913f, -4.065f, -1.904f, -5.962f, -0.007f)
                lineTo(21.102f, 51.169f)
                curveToRelative(-1.898f, 1.897f, -1.906f, 4.049f, 0.008f, 5.962f)
                lineToRelative(1.76f, 1.76f)
                curveToRelative(1.913f, 1.913f, 4.064f, 1.905f, 5.961f, 0.008f)
                curveToRelative(1.264f, -1.264f, 28.8f, -28.8f, 30.067f, -30.063f)
                curveToRelative(1.897f, -1.897f, 1.905f, -4.048f, -0.008f, -5.961f)
                verticalLineToRelative(-0.005f)
                close()
            }
            path(fill = SolidColor(tertiaryColor)) {
                moveTo(19.907f, 55.347f)
                lineTo(21f, 57.014f)
                curveToRelative(0.036f, 0.04f, 0.072f, 0.08f, 0.109f, 0.117f)
                lineToRelative(1.76f, 1.76f)
                curveToRelative(1.913f, 1.917f, 4.064f, 1.905f, 5.961f, 0.008f)
                lineToRelative(11.334f, -11.334f)
                lineToRelative(-20.258f, 7.786f)
                verticalLineToRelative(-0.004f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(57.13f, 58.89f)
                lineToRelative(1.76f, -1.76f)
                curveToRelative(1.912f, -1.912f, 1.904f, -4.064f, 0.007f, -5.96f)
                lineTo(28.831f, 21.102f)
                curveToRelative(-1.897f, -1.898f, -4.049f, -1.906f, -5.962f, 0.008f)
                lineToRelative(-1.76f, 1.76f)
                curveToRelative(-1.913f, 1.913f, -1.905f, 4.064f, -0.008f, 5.961f)
                curveToRelative(1.264f, 1.264f, 28.8f, 28.8f, 30.063f, 30.067f)
                curveToRelative(1.897f, 1.897f, 4.048f, 1.905f, 5.961f, -0.008f)
                horizontalLineToRelative(0.004f)
                close()
            }
            path(fill = SolidColor(tertiaryColor)) {
                moveTo(24.653f, 19.908f)
                lineToRelative(-1.667f, 1.094f)
                arcToRelative(3.05f, 3.05f, 0f, false, false, -0.117f, 0.109f)
                lineToRelative(-1.76f, 1.76f)
                curveToRelative(-1.917f, 1.913f, -1.905f, 4.064f, -0.008f, 5.961f)
                lineToRelative(11.334f, 11.334f)
                lineToRelative(-7.786f, -20.258f)
                horizontalLineToRelative(0.004f)
                close()
            }
        }.build()
    }

    private fun ordersEmpty(
        primaryColor: Color,
        secondaryColor: Color,
        tertiaryColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "OrdersEmpty",
            defaultWidth = 88.dp,
            defaultHeight = 88.dp,
            viewportWidth = 88f,
            viewportHeight = 88f
        ).apply {
            path(fill = SolidColor(primaryColor)) {
                moveTo(58.65f, 21.768f)
                curveTo(58.65f, 17.234f, 56.117f, 14.667f, 51.596f, 14.667f)
                horizontalLineTo(7.054f)
                curveTo(2.533f, 14.667f, 0f, 17.234f, 0f, 21.768f)
                verticalLineTo(80.895f)
                curveTo(0f, 85.43f, 2.533f, 87.996f, 7.054f, 87.996f)
                horizontalLineTo(51.6f)
                curveTo(56.121f, 87.996f, 58.654f, 85.43f, 58.654f, 80.895f)
                verticalLineTo(21.768f)
                horizontalLineTo(58.65f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(88.001f, 44f)
                horizontalLineTo(43.988f)
                verticalLineTo(88f)
                horizontalLineTo(88.001f)
                verticalLineTo(44f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(27.831f, 31.449f)
                curveTo(27.831f, 29.892f, 29.855f, 29.371f, 29.855f, 27.77f)
                curveTo(29.855f, 26.169f, 27.831f, 25.647f, 27.831f, 24.087f)
                curveTo(27.831f, 22.526f, 29.391f, 21.048f, 28.271f, 19.927f)
                curveTo(27.151f, 18.807f, 25.676f, 20.355f, 24.116f, 20.355f)
                curveTo(22.556f, 20.355f, 22.039f, 18.33f, 20.434f, 18.33f)
                curveTo(18.83f, 18.33f, 18.32f, 20.355f, 16.76f, 20.355f)
                curveTo(15.201f, 20.355f, 14.687f, 18.33f, 13.087f, 18.33f)
                curveTo(11.486f, 18.33f, 10.965f, 20.355f, 9.405f, 20.355f)
                curveTo(7.845f, 20.355f, 6.366f, 18.807f, 5.25f, 19.927f)
                curveTo(4.134f, 21.048f, 5.69f, 22.522f, 5.69f, 24.087f)
                curveTo(5.69f, 25.651f, 3.666f, 26.164f, 3.666f, 27.77f)
                curveTo(3.666f, 29.375f, 5.686f, 29.888f, 5.69f, 31.449f)
                curveTo(5.69f, 33.005f, 3.666f, 33.526f, 3.666f, 35.127f)
                curveTo(3.666f, 36.729f, 5.69f, 37.25f, 5.69f, 38.81f)
                curveTo(5.69f, 40.371f, 4.13f, 41.85f, 5.25f, 42.97f)
                curveTo(6.37f, 44.09f, 7.845f, 42.542f, 9.405f, 42.542f)
                curveTo(10.965f, 42.542f, 11.482f, 44.567f, 13.087f, 44.567f)
                curveTo(14.691f, 44.567f, 15.201f, 42.542f, 16.76f, 42.542f)
                curveTo(18.32f, 42.542f, 18.834f, 44.567f, 20.434f, 44.567f)
                curveTo(22.035f, 44.567f, 22.556f, 42.542f, 24.116f, 42.542f)
                curveTo(25.676f, 42.542f, 27.155f, 44.09f, 28.271f, 42.97f)
                curveTo(29.391f, 41.85f, 27.831f, 40.375f, 27.831f, 38.81f)
                curveTo(27.831f, 37.246f, 29.855f, 36.733f, 29.855f, 35.127f)
                curveTo(29.855f, 33.522f, 27.835f, 33.009f, 27.831f, 31.449f)
                close()
            }
            path(fill = SolidColor(primaryColor)) {
                moveTo(65.982f, 44f)
                horizontalLineTo(58.65f)
                verticalLineTo(55.945f)
                curveTo(58.65f, 58.117f, 59.77f, 58.667f, 60.711f, 58.667f)
                curveTo(62.32f, 58.667f, 64.422f, 56.642f, 65.982f, 56.642f)
                curveTo(67.542f, 56.642f, 69.639f, 58.667f, 71.252f, 58.667f)
                curveTo(72.197f, 58.667f, 73.313f, 58.117f, 73.313f, 55.945f)
                verticalLineTo(44f)
                horizontalLineTo(65.982f)
                close()
            }
            path(fill = SolidColor(tertiaryColor)) {
                moveTo(43.988f, 44f)
                verticalLineTo(88f)
                horizontalLineTo(26.927f)
                lineTo(43.988f, 44f)
                close()
            }
        }.build()
    }

    private fun bluetoothSettings(
        primaryColor: Color,
        secondaryColor: Color,
        tertiaryColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "BluetoothSettings",
            defaultWidth = 112.dp,
            defaultHeight = 112.dp,
            viewportWidth = 112f,
            viewportHeight = 112f
        ).apply {
            path(fill = SolidColor(primaryColor)) {
                moveTo(81.22f, 35.06f)
                lineTo(72.04f, 33.85f)
                curveTo(71.25f, 30.07f, 69.76f, 26.44f, 67.62f, 23.16f)
                lineTo(73.25f, 15.82f)
                lineTo(65.42f, 7.98f)
                lineTo(58.09f, 13.62f)
                curveTo(54.81f, 11.48f, 51.18f, 9.98f, 47.41f, 9.2f)
                lineTo(46.2f, 0f)
                horizontalLineTo(35.09f)
                lineTo(33.88f, 9.19f)
                curveTo(30.11f, 9.98f, 26.48f, 11.47f, 23.2f, 13.61f)
                lineTo(15.87f, 7.98f)
                lineTo(7.97f, 15.89f)
                lineTo(13.6f, 23.23f)
                curveTo(11.46f, 26.51f, 9.97f, 30.14f, 9.18f, 33.92f)
                lineTo(0f, 35.13f)
                verticalLineTo(46.25f)
                lineTo(9.18f, 47.46f)
                curveTo(9.97f, 51.23f, 11.46f, 54.87f, 13.6f, 58.14f)
                lineTo(7.97f, 65.48f)
                lineTo(15.8f, 73.32f)
                lineTo(23.13f, 67.69f)
                curveTo(26.41f, 69.83f, 30.04f, 71.32f, 33.81f, 72.11f)
                lineTo(35.02f, 81.3f)
                horizontalLineTo(46.12f)
                lineTo(47.33f, 72.11f)
                curveTo(51.11f, 71.32f, 54.74f, 69.83f, 58.01f, 67.69f)
                lineTo(65.34f, 73.32f)
                lineTo(73.24f, 65.41f)
                lineTo(67.62f, 58.07f)
                curveTo(69.75f, 54.79f, 71.25f, 51.16f, 72.03f, 47.38f)
                lineTo(81.21f, 46.17f)
                moveTo(32.38f, 48.89f)
                curveTo(27.54f, 44.04f, 27.54f, 36.2f, 32.38f, 31.36f)
                curveTo(37.23f, 26.51f, 45.06f, 26.51f, 49.89f, 31.36f)
                curveTo(54.74f, 36.21f, 54.74f, 44.05f, 49.89f, 48.89f)
                curveTo(45.05f, 53.74f, 37.22f, 53.74f, 32.38f, 48.89f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(112f, 80.14f)
                lineTo(103.83f, 78.36f)
                curveTo(101.94f, 74.42f, 98.76f, 71.28f, 94.89f, 69.41f)
                lineTo(93.11f, 61.22f)
                horizontalLineTo(80.19f)
                lineTo(78.41f, 69.41f)
                curveTo(74.48f, 71.3f, 71.34f, 74.49f, 69.47f, 78.35f)
                lineTo(61.29f, 80.14f)
                verticalLineTo(93.08f)
                lineTo(69.46f, 94.87f)
                curveTo(71.35f, 98.8f, 74.54f, 101.95f, 78.4f, 103.81f)
                lineTo(80.18f, 112f)
                horizontalLineTo(93.1f)
                lineTo(94.88f, 103.81f)
                curveTo(98.81f, 101.93f, 101.96f, 98.74f, 103.82f, 94.87f)
                lineTo(112f, 93.09f)
                verticalLineTo(80.14f)
                verticalLineTo(80.14f)
                close()
                moveTo(91.75f, 91.71f)
                curveTo(88.93f, 94.53f, 84.37f, 94.53f, 81.56f, 91.71f)
                curveTo(78.74f, 88.9f, 78.74f, 84.33f, 81.56f, 81.51f)
                curveTo(84.37f, 78.7f, 88.93f, 78.7f, 91.75f, 81.51f)
                curveTo(94.56f, 84.33f, 94.56f, 88.9f, 91.75f, 91.71f)
                close()
            }
            path(fill = SolidColor(tertiaryColor)) {
                moveTo(35.02f, 81.31f)
                horizontalLineTo(46.12f)
                lineTo(47.33f, 72.12f)
                curveTo(51.11f, 71.33f, 54.74f, 69.84f, 58.01f, 67.7f)
                lineTo(65.34f, 73.33f)
                lineTo(73.24f, 65.42f)
                lineTo(67.62f, 58.08f)
                curveTo(69.75f, 54.8f, 71.25f, 51.17f, 72.03f, 47.39f)
                lineTo(23.13f, 67.71f)
                curveTo(26.4f, 69.84f, 30.03f, 71.34f, 33.8f, 72.12f)
                lineTo(35.01f, 81.32f)
                lineTo(35.02f, 81.31f)
                close()
            }
        }.build()
    }

    private fun couponsEmpty(
        primaryColor: Color,
        secondaryColor: Color,
        tertiaryColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "CouponsEmpty",
            defaultWidth = 104.dp,
            defaultHeight = 87.dp,
            viewportWidth = 104f,
            viewportHeight = 87f
        ).apply {
            path(fill = SolidColor(primaryColor)) {
                moveTo(98.99f, 48.45f)
                curveTo(98.26f, 46.7f, 95.82f, 47.05f, 95.11f, 45.35f)
                curveTo(94.4f, 43.64f, 96.38f, 42.16f, 95.65f, 40.41f)
                curveTo(94.93f, 38.66f, 92.48f, 39.01f, 91.77f, 37.3f)
                curveTo(91.07f, 35.6f, 93.05f, 34.12f, 92.32f, 32.37f)
                curveTo(91.59f, 30.62f, 89.15f, 30.97f, 88.44f, 29.26f)
                curveTo(87.73f, 27.56f, 89.71f, 26.07f, 88.98f, 24.32f)
                curveTo(88.26f, 22.57f, 85.81f, 22.92f, 85.1f, 21.22f)
                curveTo(84.4f, 19.51f, 86.38f, 18.03f, 85.65f, 16.28f)
                curveTo(84.92f, 14.53f, 82.48f, 14.88f, 81.77f, 13.17f)
                curveTo(81.06f, 11.47f, 83.04f, 9.99f, 82.31f, 8.24f)
                curveTo(81.59f, 6.49f, 79.14f, 6.84f, 78.43f, 5.13f)
                curveTo(77.78f, 3.55f, 79.51f, 1.95f, 79.2f, 0.33f)
                lineTo(0f, 32.97f)
                curveTo(0.73f, 34.72f, 3.17f, 34.37f, 3.88f, 36.08f)
                curveTo(4.59f, 37.78f, 2.61f, 39.26f, 3.33f, 41.01f)
                curveTo(4.06f, 42.76f, 6.51f, 42.41f, 7.21f, 44.12f)
                curveTo(7.92f, 45.82f, 5.94f, 47.31f, 6.67f, 49.06f)
                curveTo(7.4f, 50.8f, 9.84f, 50.46f, 10.55f, 52.16f)
                curveTo(11.26f, 53.87f, 9.28f, 55.35f, 10f, 57.1f)
                curveTo(10.73f, 58.85f, 13.18f, 58.5f, 13.88f, 60.21f)
                curveTo(14.59f, 61.91f, 12.61f, 63.39f, 13.34f, 65.14f)
                curveTo(14.07f, 66.89f, 16.51f, 66.54f, 17.22f, 68.25f)
                curveTo(17.93f, 69.95f, 15.95f, 71.44f, 16.67f, 73.19f)
                curveTo(17.4f, 74.93f, 19.85f, 74.59f, 20.55f, 76.29f)
                curveTo(21.21f, 77.87f, 19.47f, 79.48f, 19.79f, 81.1f)
                lineTo(40.72f, 72.44f)
                lineTo(99f, 48.46f)
                lineTo(98.99f, 48.45f)
                close()
            }
            path(fill = SolidColor(tertiaryColor)) {
                moveTo(19.78f, 81.1f)
                lineTo(40.71f, 72.44f)
                lineTo(75.67f, 58.06f)
                horizontalLineTo(10.77f)
                curveTo(11.8f, 58.8f, 13.34f, 58.92f, 13.88f, 60.21f)
                curveTo(14.59f, 61.92f, 12.6f, 63.4f, 13.33f, 65.15f)
                curveTo(14.06f, 66.9f, 16.5f, 66.55f, 17.21f, 68.25f)
                curveTo(17.92f, 69.96f, 15.94f, 71.44f, 16.67f, 73.19f)
                curveTo(17.4f, 74.94f, 19.84f, 74.59f, 20.55f, 76.3f)
                curveTo(21.2f, 77.88f, 19.47f, 79.48f, 19.79f, 81.1f)
                lineTo(19.78f, 81.1f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(92.23f, 60.99f)
                curveTo(92.23f, 54.49f, 97.5f, 49.23f, 104f, 49.23f)
                verticalLineTo(34.98f)
                horizontalLineTo(25.82f)
                verticalLineTo(49.23f)
                curveTo(32.32f, 49.23f, 37.59f, 54.49f, 37.59f, 60.99f)
                curveTo(37.59f, 67.48f, 32.32f, 72.75f, 25.82f, 72.75f)
                verticalLineTo(86.99f)
                horizontalLineTo(104f)
                verticalLineTo(72.75f)
                curveTo(97.5f, 72.75f, 92.23f, 67.48f, 92.23f, 60.99f)
                close()
            }
            path(fill = SolidColor(primaryColor)) {
                moveTo(50.92f, 54.75f)
                curveTo(50.92f, 51.19f, 53.46f, 48.42f, 57.34f, 48.42f)
                curveTo(61.21f, 48.42f, 63.79f, 51.19f, 63.79f, 54.75f)
                curveTo(63.79f, 58.32f, 61.25f, 61.02f, 57.34f, 61.02f)
                curveTo(53.43f, 61.02f, 50.92f, 58.28f, 50.92f, 54.75f)
                close()
                moveTo(60.37f, 54.75f)
                curveTo(60.37f, 52.66f, 59.08f, 51.38f, 57.33f, 51.38f)
                curveTo(55.58f, 51.38f, 54.33f, 52.66f, 54.33f, 54.75f)
                curveTo(54.33f, 56.84f, 55.62f, 58.05f, 57.33f, 58.05f)
                curveTo(59.04f, 58.05f, 60.37f, 56.76f, 60.37f, 54.75f)
                close()
                moveTo(54.87f, 74.1f)
                lineTo(71.03f, 48.8f)
                horizontalLineTo(73.92f)
                lineTo(57.71f, 74.1f)
                horizontalLineTo(54.86f)
                horizontalLineTo(54.87f)
                close()
                moveTo(64.85f, 68.3f)
                curveTo(64.85f, 64.73f, 67.39f, 61.93f, 71.26f, 61.93f)
                curveTo(75.14f, 61.93f, 77.72f, 64.73f, 77.72f, 68.3f)
                curveTo(77.72f, 71.86f, 75.13f, 74.56f, 71.26f, 74.56f)
                curveTo(67.4f, 74.56f, 64.85f, 71.83f, 64.85f, 68.3f)
                close()
                moveTo(74.3f, 68.3f)
                curveTo(74.3f, 66.21f, 73.01f, 64.92f, 71.26f, 64.92f)
                curveTo(69.51f, 64.92f, 68.26f, 66.21f, 68.26f, 68.3f)
                curveTo(68.26f, 70.39f, 69.52f, 71.6f, 71.26f, 71.6f)
                curveTo(73f, 71.6f, 74.3f, 70.34f, 74.3f, 68.3f)
                close()
            }
        }.build()
    }

    private fun ImageVector.Builder.addCardReaderBlobs(blobColor: Color) {
        path(fill = SolidColor(blobColor)) {
            moveTo(54.6986f, 141.943f)
            curveTo(51.6569f, 143.799f, 48.2843f, 145.052f, 44.6415f, 145.58f)
            curveTo(25.393f, 148.373f, 5.88991f, 129.904f, 1.08011f, 104.328f)
            curveTo(-3.72969f, 78.7526f, 7.97517f, 55.7552f, 27.2236f, 52.9623f)
            curveTo(35.6626f, 51.7378f, 44.1505f, 54.6001f, 51.4277f, 60.3565f)
            curveTo(60.8814f, 22.6925f, 84.173f, -2.94319f, 108.874f, 0.271658f)
            curveTo(126.42f, 2.55524f, 140.716f, 18.8801f, 148.469f, 42.2086f)
            curveTo(160.466f, 33.0717f, 173.517f, 30.0297f, 184.124f, 35.3818f)
            curveTo(203.378f, 45.0968f, 207.285f, 78.7903f, 192.851f, 110.638f)
            curveTo(179.655f, 139.756f, 155.697f, 157.25f, 136.972f, 152.612f)
            curveTo(125.423f, 171.338f, 109.489f, 182.162f, 92.9209f, 180.005f)
            curveTo(76.3635f, 177.85f, 62.6999f, 163.192f, 54.6986f, 141.943f)
            close()
        }
    }

    private fun ImageVector.Builder.addCardReaderBody(
        readerBodyColor: Color,
        readerFaceColor: Color
    ) {
        path(fill = SolidColor(readerBodyColor)) {
            moveTo(61.4512f, 31.2461f)
            lineTo(141.4512f, 31.2461f)
            arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 157.4512f, 47.2461f)
            lineTo(157.4512f, 128.2461f)
            arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 141.4512f, 144.2461f)
            lineTo(61.4512f, 144.2461f)
            arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 45.4512f, 128.2461f)
            lineTo(45.4512f, 47.2461f)
            arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 61.4512f, 31.2461f)
            close()
        }
        path(fill = SolidColor(readerFaceColor)) {
            moveTo(61.4512f, 47.2461f)
            lineTo(141.4512f, 47.2461f)
            arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 157.4512f, 63.2461f)
            lineTo(157.4512f, 128.2461f)
            arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 141.4512f, 144.2461f)
            lineTo(61.4512f, 144.2461f)
            arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 45.4512f, 128.2461f)
            lineTo(45.4512f, 63.2461f)
            arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 61.4512f, 47.2461f)
            close()
        }
    }

    private fun ImageVector.Builder.addNfcSignal(nfcColor: Color) {
        path(
            stroke = SolidColor(nfcColor),
            strokeLineWidth = 4f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round
        ) {
            moveTo(92.4512f, 89.369f)
            curveTo(94.0182f, 93.1521f, 94.142f, 97.5329f, 92.4512f, 101.615f)
            moveTo(99.8422f, 86.3076f)
            curveTo(102.193f, 91.9822f, 102.378f, 98.5534f, 99.8422f, 104.676f)
            moveTo(107.233f, 83.2461f)
            curveTo(110.367f, 90.8122f, 110.615f, 99.5739f, 107.233f, 107.738f)
        }
    }

    private fun ImageVector.Builder.addExclamationMark(exclamationColor: Color) {
        path(fill = SolidColor(exclamationColor)) {
            moveTo(101.451f, 102.468f)
            curveTo(99.5499f, 102.468f, 98.5746f, 101.514f, 98.5252f, 99.6055f)
            lineTo(98.0438f, 80.4351f)
            curveTo(98.0191f, 79.5171f, 98.3154f, 78.7561f, 98.9327f, 78.1521f)
            curveTo(99.5746f, 77.5481f, 100.402f, 77.2461f, 101.414f, 77.2461f)
            curveTo(102.402f, 77.2461f, 103.217f, 77.5602f, 103.859f, 78.1883f)
            curveTo(104.525f, 78.7923f, 104.846f, 79.5533f, 104.822f, 80.4714f)
            lineTo(104.266f, 99.6055f)
            curveTo(104.241f, 101.514f, 103.303f, 102.468f, 101.451f, 102.468f)
            close()
            moveTo(101.451f, 114.246f)
            curveTo(100.365f, 114.246f, 99.4265f, 113.884f, 98.6364f, 113.159f)
            curveTo(97.8462f, 112.41f, 97.4512f, 111.516f, 97.4512f, 110.477f)
            curveTo(97.4512f, 109.438f, 97.8462f, 108.557f, 98.6364f, 107.832f)
            curveTo(99.4265f, 107.083f, 100.365f, 106.708f, 101.451f, 106.708f)
            curveTo(102.538f, 106.708f, 103.476f, 107.071f, 104.266f, 107.796f)
            curveTo(105.056f, 108.52f, 105.451f, 109.414f, 105.451f, 110.477f)
            curveTo(105.451f, 111.54f, 105.044f, 112.434f, 104.229f, 113.159f)
            curveTo(103.439f, 113.884f, 102.513f, 114.246f, 101.451f, 114.246f)
            close()
        }
    }

    private fun ImageVector.Builder.addBatteryIcon(
        batteryColor: Color,
        batteryLevelColor: Color
    ) {
        path(fill = SolidColor(batteryColor)) {
            moveTo(91.4512f, 60.2461f)
            curveTo(89.242f, 60.2461f, 87.4512f, 62.037f, 87.4512f, 64.2461f)
            verticalLineTo(68.2461f)
            horizontalLineTo(80.4512f)
            curveTo(77.6897f, 68.2461f, 75.4512f, 70.4847f, 75.4512f, 73.2461f)
            verticalLineTo(123.246f)
            curveTo(75.4512f, 126.008f, 77.6897f, 128.246f, 80.4512f, 128.246f)
            horizontalLineTo(122.451f)
            curveTo(125.213f, 128.246f, 127.451f, 126.008f, 127.451f, 123.246f)
            verticalLineTo(73.2461f)
            curveTo(127.451f, 70.4847f, 125.213f, 68.2461f, 122.451f, 68.2461f)
            horizontalLineTo(115.451f)
            verticalLineTo(64.2461f)
            curveTo(115.451f, 62.037f, 113.66f, 60.2461f, 111.451f, 60.2461f)
            horizontalLineTo(91.4512f)
            close()
        }
        path(fill = SolidColor(batteryLevelColor)) {
            moveTo(83.4512f, 114.246f)
            lineTo(119.4512f, 114.246f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 122.4512f, 117.246f)
            lineTo(122.4512f, 117.246f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 119.4512f, 120.246f)
            lineTo(83.4512f, 120.246f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 80.4512f, 117.246f)
            lineTo(80.4512f, 117.246f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 83.4512f, 114.246f)
            close()
        }
    }

    private fun cardReaderWithNfc(
        blobColor: Color,
        readerBodyColor: Color,
        readerFaceColor: Color,
        nfcColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "CardReaderWithNfc",
            defaultWidth = 202.dp,
            defaultHeight = 181.dp,
            viewportWidth = 202f,
            viewportHeight = 181f
        ).apply {
            addCardReaderBlobs(blobColor)
            addCardReaderBody(readerBodyColor, readerFaceColor)
            addNfcSignal(nfcColor)
        }.build()
    }

    private fun cardReaderWithNfcDark(
        blobColor: Color,
        readerBodyColor: Color,
        readerFaceColor: Color,
        nfcColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "CardReaderWithNfcDark",
            defaultWidth = 202.dp,
            defaultHeight = 181.dp,
            viewportWidth = 202f,
            viewportHeight = 181f
        ).apply {
            addCardReaderBlobs(blobColor)
            addCardReaderBody(readerBodyColor, readerFaceColor)
            addNfcSignal(nfcColor)
        }.build()
    }

    private fun cardReaderError(
        blobColor: Color,
        readerBodyColor: Color,
        readerFaceColor: Color,
        exclamationColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "CardReaderError",
            defaultWidth = 202.dp,
            defaultHeight = 181.dp,
            viewportWidth = 202f,
            viewportHeight = 181f
        ).apply {
            addCardReaderBlobs(blobColor)
            addCardReaderBody(readerBodyColor, readerFaceColor)
            addExclamationMark(exclamationColor)
        }.build()
    }

    private fun cardReaderErrorDark(
        blobColor: Color,
        readerBodyColor: Color,
        readerFaceColor: Color,
        exclamationColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "CardReaderErrorDark",
            defaultWidth = 202.dp,
            defaultHeight = 181.dp,
            viewportWidth = 202f,
            viewportHeight = 181f
        ).apply {
            addCardReaderBlobs(blobColor)
            addCardReaderBody(readerBodyColor, readerFaceColor)
            addExclamationMark(exclamationColor)
        }.build()
    }

    private fun cardReaderBattery(
        blobColor: Color,
        readerBodyColor: Color,
        readerFaceColor: Color,
        batteryColor: Color,
        batteryLevelColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "CardReaderBattery",
            defaultWidth = 202.dp,
            defaultHeight = 181.dp,
            viewportWidth = 202f,
            viewportHeight = 181f
        ).apply {
            addCardReaderBlobs(blobColor)
            addCardReaderBody(readerBodyColor, readerFaceColor)
            addBatteryIcon(batteryColor, batteryLevelColor)
        }.build()
    }

    private fun cardReaderBatteryDark(
        blobColor: Color,
        readerBodyColor: Color,
        readerFaceColor: Color,
        batteryColor: Color,
        batteryLevelColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "CardReaderBatteryDark",
            defaultWidth = 202.dp,
            defaultHeight = 181.dp,
            viewportWidth = 202f,
            viewportHeight = 181f
        ).apply {
            addCardReaderBlobs(blobColor)
            addCardReaderBody(readerBodyColor, readerFaceColor)
            addBatteryIcon(batteryColor, batteryLevelColor)
        }.build()
    }

    private fun cardReaderNotConnected(
        primaryColor: Color,
        secondaryColor: Color,
        tertiaryColor: Color
    ): ImageVector {
        return ImageVector.Builder(
            name = "CardReaderNotConnected",
            defaultWidth = 169.dp,
            defaultHeight = 169.dp,
            viewportWidth = 169f,
            viewportHeight = 169f
        ).apply {
            path(fill = SolidColor(primaryColor)) {
                moveTo(155.557f, 28.9419f)
                curveTo(155.557f, 20.2518f, 150.755f, 15.3636f, 142.159f, 15.3636f)
                horizontalLineTo(57.5111f)
                curveTo(48.9147f, 15.3636f, 44.1131f, 20.2518f, 44.1131f, 28.9419f)
                verticalLineTo(113.515f)
                curveTo(44.1131f, 122.205f, 48.9147f, 127.094f, 57.5111f, 127.094f)
                horizontalLineTo(142.159f)
                curveTo(150.755f, 127.094f, 155.557f, 122.205f, 155.557f, 113.515f)
                verticalLineTo(45.779f)
                verticalLineTo(28.9419f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(75.3242f, 39.8038f)
                lineTo(80.2033f, 44.692f)
                curveTo(85.2372f, 39.6487f, 92.1298f, 36.5451f, 99.7969f, 36.5451f)
                curveTo(107.464f, 36.5451f, 114.357f, 39.6487f, 119.391f, 44.692f)
                lineTo(124.27f, 39.8038f)
                curveTo(117.997f, 33.519f, 109.323f, 29.6395f, 99.7969f, 29.6395f)
                curveTo(90.2712f, 29.6395f, 81.5973f, 33.519f, 75.3242f, 39.8038f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(85.0825f, 49.6587f)
                lineTo(89.9616f, 54.5469f)
                curveTo(92.4398f, 52.064f, 95.9248f, 50.5122f, 99.7197f, 50.5122f)
                curveTo(103.514f, 50.5122f, 107f, 52.064f, 109.478f, 54.5469f)
                lineTo(114.357f, 49.6587f)
                curveTo(110.562f, 45.8568f, 105.373f, 43.5291f, 99.6422f, 43.5291f)
                curveTo(93.9113f, 43.5291f, 88.7224f, 45.8568f, 84.9276f, 49.6587f)
                horizontalLineTo(85.0825f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(94.9181f, 59.4341f)
                lineTo(99.7971f, 64.3223f)
                lineTo(104.676f, 59.4341f)
                curveTo(103.437f, 58.1926f, 101.733f, 57.4167f, 99.7971f, 57.4167f)
                curveTo(97.861f, 57.4167f, 96.1572f, 58.1926f, 94.9181f, 59.4341f)
                close()
            }
            path(fill = SolidColor(tertiaryColor)) {
                moveTo(44.1131f, 78.266f)
                verticalLineTo(101.566f)
                verticalLineTo(113.515f)
                curveTo(44.1131f, 122.205f, 48.9147f, 127.094f, 57.5111f, 127.094f)
                horizontalLineTo(142.159f)
                lineTo(44.1131f, 78.266f)
                close()
            }
            path(fill = SolidColor(secondaryColor)) {
                moveTo(13.443f, 132.377f)
                curveTo(13.443f, 152.566f, 29.8459f, 169f, 49.9972f, 169f)
                curveTo(70.1484f, 169f, 86.5513f, 152.566f, 86.5513f, 132.377f)
                curveTo(86.5513f, 112.188f, 70.1484f, 95.7545f, 49.9972f, 95.7545f)
                curveTo(29.8459f, 95.7545f, 13.443f, 112.188f, 13.443f, 132.377f)
                close()
            }
            path(fill = SolidColor(primaryColor)) {
                moveTo(64.7339f, 122.501f)
                lineTo(59.8238f, 117.558f)
                lineTo(49.996f, 127.443f)
                lineTo(40.1682f, 117.558f)
                lineTo(35.2582f, 122.493f)
                lineTo(45.0937f, 132.378f)
                lineTo(35.2582f, 142.271f)
                lineTo(40.1682f, 147.206f)
                lineTo(49.996f, 137.313f)
                lineTo(59.8238f, 147.198f)
                lineTo(64.7339f, 142.263f)
                lineTo(54.906f, 132.378f)
                lineTo(64.7339f, 122.501f)
                close()
            }
        }.build()
    }
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalLayoutApi::class)
@WooPosPreview
@Composable
private fun WooPosIconsPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Large.value)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
                maxItemsInEachRow = 4
            ) {
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.NotFound,
                        contentDescription = "NotFound",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.Check,
                        contentDescription = "Check",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.ErrorX,
                        contentDescription = "ErrorX",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.OrdersEmpty,
                        contentDescription = "OrdersEmpty",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.BluetoothSettings,
                        contentDescription = "BluetoothSettings",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.CouponsEmpty,
                        contentDescription = "CouponsEmpty",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.CardReaderScanning,
                        contentDescription = "CardReaderScanning",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.CardReaderFound,
                        contentDescription = "CardReaderFound",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.CardReaderConnecting,
                        contentDescription = "CardReaderConnecting",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.CardReaderSuccess,
                        contentDescription = "CardReaderSuccess",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.CardReaderError,
                        contentDescription = "CardReaderError",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.CardReaderBatteryLow,
                        contentDescription = "CardReaderBatteryLow",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
                Box(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = WooPosIcons.CardReaderNotConnected,
                        contentDescription = "CardReaderNotConnected",
                        modifier = Modifier.size(WooPosComponentSize.Small.value)
                    )
                }
            }
        }
    }
}
