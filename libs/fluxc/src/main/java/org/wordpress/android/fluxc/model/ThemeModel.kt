package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import java.io.Serializable

@Entity(
    tableName = "ThemeModel",
    primaryKeys = ["siteId", "themeId", "isWpComTheme"]
)
data class ThemeModel(
    val siteId: LocalId,
    val themeId: String,
    val name: String,
    val demoUrl: String?,
    val active: Boolean,
    val isWpComTheme: Boolean
) : Serializable {
    companion object {
        private const val serialVersionUID = 5966516212440517166L
    }
}
