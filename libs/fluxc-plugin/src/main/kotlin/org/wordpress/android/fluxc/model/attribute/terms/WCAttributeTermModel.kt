package org.wordpress.android.fluxc.model.attribute.terms

data class WCAttributeTermModel(
    val remoteId: Int = 0,
    val localSiteId: Int = 0,
    val attributeId: Int = 0,
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val count: Int = 0,
    val menuOrder: Int = 0
)
