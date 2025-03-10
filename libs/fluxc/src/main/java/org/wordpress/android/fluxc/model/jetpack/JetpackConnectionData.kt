package org.wordpress.android.fluxc.model.jetpack


data class JetpackConnectionData(
    val currentUser: JetpackUser,
    val blogId: Long?,
    val isSiteRegistered: Boolean?,
    val connectionOwner: String?
)

data class JetpackUser(
    val isConnected: Boolean,
    val isMaster: Boolean,
    val username: String,
    val wpcomId: Long,
    val wpcomUsername: String,
    val wpcomEmail: String
)
