package org.wordpress.android.fluxc.model.gateways

data class WCGatewayModel(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val isEnabled: Boolean,
    val methodTitle: String,
    val methodDescription: String,
    val features: List<String>
)
