package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.ThemeModel

data class Theme(
    val id: String,
    val name: String,
    val demoUrl: String?
)

fun ThemeModel.toAppModel(): Theme {
    return Theme(
        id = themeId,
        name = name,
        demoUrl = demoUrl
    )
}
