package com.woocommerce.android.ui.login.storecreation.profiler

data class ProfilerOptions(
    val aboutMerchant: List<AboutMerchant>,
    val industries: List<Industry>
)

data class AboutMerchant(
    val id: Int,
    val value: String,
    val heading: String,
    val description: String,
    val tracks: String,
    val platforms: List<Platform>?,
)

data class Platform(
    val value: String,
    val label: String
)

data class Industry(
    val id: String,
    val label: String,
    val key: String
)
