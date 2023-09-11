package com.woocommerce.android.ui.compose.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R.font

val robotoFamily = FontFamily(
    Font(font.roboto_light_ttf, FontWeight.Light),
    Font(font.roboto_ttf, FontWeight.Normal),
    Font(font.roboto_medium_ttf, FontWeight.Medium)
)

val WooTypography = Typography(
    defaultFontFamily = robotoFamily,
    h1 = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 96.sp,
        lineHeight = 112.sp
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 60.sp,
        lineHeight = 72.sp
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 56.sp
    ),
    h4 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 36.sp
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 24.sp
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp
    ),
    subtitle1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    subtitle2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 24.sp
    ),
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    button = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    overline = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 16.sp
    )
)
