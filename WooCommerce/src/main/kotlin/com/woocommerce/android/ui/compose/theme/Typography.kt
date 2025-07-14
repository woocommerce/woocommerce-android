@file:OptIn(ExperimentalTextApi::class)

package com.woocommerce.android.ui.compose.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val fontName = GoogleFont("Roboto")
val robotoFamily = FontFamily(
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium)
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
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
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
        lineHeight = 16.sp,
    ),
    overline = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 16.sp
    )
)
