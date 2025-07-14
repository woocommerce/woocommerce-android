package com.woocommerce.android.ui.jetpack

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpack.JetpackConnectionData
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

/**
 * Jetpack's `/connection/data` endpoint responses, as outlined from the Jetpack codebase:
 * `projects/packages/connection/tests/php/test-rest-endpoints.php`
 *
 * - 404: Jetpack is not activated.
 * - 403: Jetpack is activated but current user has no permission to get connection data.
 * - 200: Jetpack is activated, connection data is given.
 *
 *  See also https://github.com/Automattic/jetpack/blob/trunk/docs/rest-api.md#get-wp-jsonjetpackv4connectiondata
 *  for full response.
 *
 */
class FetchJetpackStatus @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val wooCommerceStore: WooCommerceStore
) {
    companion object {
        private const val FORBIDDEN_CODE = 403
        private const val NOT_FOUND_CODE = 404
        private const val JETPACK_SLUG = "jetpack"
    }

    sealed interface JetpackStatusFetchResponse {
        data class Success(val status: JetpackStatus) : JetpackStatusFetchResponse
        object ConnectionForbidden : JetpackStatusFetchResponse
    }

    @Suppress("ReturnCount", "NestedBlockDepth")
    suspend operator fun invoke(
        site: SiteModel,
        useApplicationPasswords: Boolean,
        isJetpackInstalled: Boolean? = null
    ): Result<JetpackStatusFetchResponse> {
        return jetpackStore.fetchJetpackConnectionData(
            site = site,
            useApplicationPasswords = useApplicationPasswords
        ).let { userResult ->
            when {
                userResult.error?.errorCode == FORBIDDEN_CODE -> {
                    Result.success(JetpackStatusFetchResponse.ConnectionForbidden)
                }

                userResult.error?.errorCode == NOT_FOUND_CODE -> {
                    Result.success(
                        JetpackStatusFetchResponse.Success(
                            JetpackStatus(
                                isJetpackInstalled = false,
                                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                                    siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                                    blogId = null
                                )
                            )
                        )
                    )
                }

                userResult.isError -> {
                    Result.failure(OnChangedException(userResult.error))
                }

                else -> {
                    val isJetpackInstalled = isJetpackInstalled ?: checkIfJetpackIsInstalled(site).getOrElse {
                        return Result.failure(it)
                    }

                    val jetpackConnectionData = userResult.data!!

                    Result.success(
                        JetpackStatusFetchResponse.Success(
                            JetpackStatus(
                                isJetpackInstalled = isJetpackInstalled,
                                jetpackConnectionStatus = jetpackConnectionData.toConnectionStatus(isJetpackInstalled)
                            )
                        )
                    )
                }
            }
        }
    }

    private suspend fun checkIfJetpackIsInstalled(site: SiteModel): Result<Boolean> {
        return wooCommerceStore.fetchSitePlugins(site)
            .let { pluginResult ->
                when {
                    pluginResult.isError -> {
                        Result.failure(OnChangedException(pluginResult.error))
                    }

                    else -> {
                        Result.success(pluginResult.model!!.any { it.slug == JETPACK_SLUG && it.isActive })
                    }
                }
            }
    }

    private fun JetpackConnectionData.toConnectionStatus(isJetpackInstalled: Boolean): JetpackConnectionStatus {
        return if (currentUser.isConnected) {
            JetpackConnectionStatus.AccountConnected(currentUser.wpcomEmail)
        } else {
            JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = when (isSiteRegistered) {
                    true -> JetpackSiteRegistrationStatus.REGISTERED
                    false -> JetpackSiteRegistrationStatus.NOT_REGISTERED
                    else -> {
                        if (isJetpackInstalled) {
                            // Which means the installed version of Jetpack doesn't support the connection API
                            JetpackSiteRegistrationStatus.UNKNOWN
                        } else {
                            // Infer the site registration status based on whether the site has an owner or not
                            // Discussion:
                            // - If the site has an owner, it means the site is already registered
                            // - If the site doesn't have an owner, while the site may already be registered, it's OK
                            //   to treat it as not registered since we'll register it later when connecting the account
                            //   and there are no accounts that could be affected by the second registration.
                            if (connectionOwner.isNotNullOrEmpty()) {
                                JetpackSiteRegistrationStatus.REGISTERED
                            } else {
                                JetpackSiteRegistrationStatus.NOT_REGISTERED
                            }
                        }
                    }
                },
                blogId = blogId
            )
        }
    }
}
