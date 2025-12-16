package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "ThemeEntity",
    primaryKeys = ["siteId", "themeId", "isWpComTheme"]
)
data class ThemeModel(
    val siteId: LocalId,
    val themeId: String,
    val name: String,
    val demoUrl: String?,
    val active: Boolean,
    val isWpComTheme: Boolean
)
