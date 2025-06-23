package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserMapper
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient
import org.wordpress.android.fluxc.persistence.dao.UserDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCUserStore @Inject constructor(
    private val restClient: WCUserRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val userDao: UserDao
) {
    suspend fun fetchUserRole(site: SiteModel): WooResult<WCUserModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchUserInfo") {
            val response = restClient.fetchUserInfo(site)
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val user = WCUserMapper.map(response.result, site)
                    userDao.upsertUser(user)
                    WooResult(user)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun getUserByEmail(site: SiteModel, email: String) = userDao.getUserBySiteAndEmail(site.localId(), email)
}
