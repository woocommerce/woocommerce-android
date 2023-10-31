package com.woocommerce.android.ui.jetpack.benefits

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

/**
 * First, a list of plugins is fetched from the site. If Jetpack is not installed, then the
 * [JetpackStatus] is returned with `isJetpackInstalled` set to `false`. We cannot use the 404 NOT FOUND response
 * because a site may have Jetpack Connection Package installed, but not Jetpack itself.
 *
 * Meaning for Jetpack's `/connection/data` endpoint responses, as outlined from the Jetpack codebase:
 * `projects/packages/connection/tests/php/test-rest-endpoints.php`
 *
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
        private const val JETPACK_SLUG = "jetpack"
    }

    enum class JetpackStatusFetchResponse {
        SUCCESS, NOT_FOUND, FORBIDDEN
    }

    @Suppress("ReturnCount")
    suspend operator fun invoke(): Result<Pair<JetpackStatus, JetpackStatusFetchResponse>> {
        wooCommerceStore.fetchSitePlugins(selectedSite.get()).let { result ->
            when {
                result.isError -> {
                    return Result.failure(OnChangedException(result.error))
                }
                else -> {
                    if (result.model!!.none { it.slug == JETPACK_SLUG && it.isActive }) {
                        return Result.success(
                            Pair(
                                JetpackStatus(
                                    isJetpackInstalled = false,
                                    isJetpackConnected = false,
                                    wpComEmail = null
                                ),
                                JetpackStatusFetchResponse.NOT_FOUND
                            )
                        )
                    }
                }
            }
        }

        return jetpackStore.fetchJetpackUser(selectedSite.get(), useApplicationPasswords = true).let { result ->
            when {
                result.error?.errorCode == FORBIDDEN_CODE -> {
                    Result.success(
                        Pair(
                            JetpackStatus(
                                isJetpackInstalled = true,
                                isJetpackConnected = false,
                                wpComEmail = null
                            ),
                            JetpackStatusFetchResponse.FORBIDDEN
                        )
                    )
                }

                result.isError -> {
                    Result.failure(OnChangedException(result.error))
                }

                else -> {
                    Result.success(
                        Pair(
                            JetpackStatus(
                                isJetpackInstalled = true,
                                isJetpackConnected = result.user!!.isConnected,
                                wpComEmail = result.user!!.wpcomEmail.orNullIfEmpty()
                            ),
                            JetpackStatusFetchResponse.SUCCESS
                        )
                    )
                }
            }
        }
    }
}
