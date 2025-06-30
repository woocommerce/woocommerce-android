package org.wordpress.android.fluxc.model.user

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient.UserApiResponse

object WCUserMapper {
    fun map(user: UserApiResponse, siteModel: SiteModel): WCUserModel {
        return WCUserModel(
            remoteUserId = RemoteId(user.id),
            username = user.username ?: "",
            firstName = user.firstName ?: "",
            lastName = user.lastName ?: "",
            email = user.email ?: "",
            roles = user.roles.toString(),
            localSiteId = siteModel.localId()
        )
    }
}
