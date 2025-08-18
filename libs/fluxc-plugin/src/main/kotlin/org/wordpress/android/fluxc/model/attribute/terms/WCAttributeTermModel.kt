package org.wordpress.android.fluxc.model.attribute.terms

data class WCAttributeTermModel(
    val remoteId: Int,
    val localSiteId: Int,
    val attributeId: Int,
    val name: String,
    val slug: String,
    val description: String,
    val count: Int,
    val menuOrder: Int,
)
