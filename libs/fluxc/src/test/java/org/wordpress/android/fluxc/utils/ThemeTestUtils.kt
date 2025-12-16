package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.ThemeModel

fun createTestTheme(
    siteId: Int,
    themeId: String,
    name: String,
    isWpComTheme: Boolean = false,
    active: Boolean = false,
    demoUrl: String? = null
) = ThemeModel(
    siteId = LocalId(siteId),
    themeId = themeId,
    name = name,
    demoUrl = demoUrl,
    active = active,
    isWpComTheme = isWpComTheme
)
