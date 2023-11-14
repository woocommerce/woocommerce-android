package com.woocommerce.android.ui.jetpack.benefits

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
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
    private val selectedSite: SelectedSite,
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
    suspend operator fun invoke(): Result<JetpackStatusFetchResponse> {
        return jetpackStore.fetchJetpackUser(selectedSite.get(), useApplicationPasswords = true).let { userResult ->
            when {
                userResult.error?.errorCode == FORBIDDEN_CODE -> {
                    Result.success(JetpackStatusFetchResponse.ConnectionForbidden)
                }

                userResult.error?.errorCode == NOT_FOUND_CODE -> {
                    Result.success(
                        JetpackStatusFetchResponse.Success(
                            JetpackStatus(
                                isJetpackInstalled = false,
                                isJetpackConnected = false,
                                wpComEmail = null
                            )
                        )
                    )
                }

                userResult.isError -> {
                    Result.failure(OnChangedException(userResult.error))
                }

                else -> {
                    val isJetpackInstalled = wooCommerceStore.fetchSitePlugins(selectedSite.get()).let { pluginResult ->
                        when {
                            pluginResult.isError -> {
                                return Result.failure(OnChangedException(pluginResult.error))
                            }
                            else -> {
                                pluginResult.model!!.any { it.slug == JETPACK_SLUG && it.isActive }
                            }
                        }
                    }

                    Result.success(
                        JetpackStatusFetchResponse.Success(
                            JetpackStatus(
                                isJetpackInstalled = isJetpackInstalled,
                                isJetpackConnected = userResult.user!!.isConnected,
                                wpComEmail = userResult.user!!.wpcomEmail.orNullIfEmpty()
                            )
                        )
                    )
                }
            }
        }
    }
}
