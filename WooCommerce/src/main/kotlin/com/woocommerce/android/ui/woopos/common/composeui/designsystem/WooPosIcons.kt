@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.woopos.common.composeui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
object WooPosIcons {
    val NotFound: ImageVector
        @Composable
        get() = notFound(
            primaryColor = MaterialTheme.colorScheme.secondary,
            secondaryColor = MaterialTheme.colorScheme.primary,
            tertiaryColor = MaterialTheme.colorScheme.inverseSurface
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
            tertiaryColor = MaterialTheme.colorScheme.inverseSurface
        )

    val OrdersEmpty: ImageVector
        @Composable
        get() = ordersEmpty(
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            tertiaryColor = MaterialTheme.colorScheme.inverseSurface
        )

    val BluetoothSettings: ImageVector
        @Composable
        get() = bluetoothSettings(
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            tertiaryColor = MaterialTheme.colorScheme.inverseSurface
        )

    val Exit: ImageVector
        @Composable
        get() = exit(
            color = Color.White
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

    private fun exit(color: Color): ImageVector {
        return ImageVector.Builder(
            name = "Exit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(color)) {
                moveTo(10.09f, 15.59f)
                lineTo(11.5f, 17f)
                lineToRelative(5f, -5f)
                lineToRelative(-5f, -5f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(12.67f, 11f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(9.67f)
                lineToRelative(-2.58f, 2.59f)
                close()
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(2f)
                verticalLineTo(5f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(14f)
                horizontalLineTo(5f)
                verticalLineToRelative(-4f)
                horizontalLineTo(3f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
            }
        }.build()
    }
}
