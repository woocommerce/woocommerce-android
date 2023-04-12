package com.woocommerce.android.ui.jetpack.benefits

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.JetpackStore
import javax.inject.Inject

/**
 * Meaning for Jetpack's `/connection/data` endpoint responses, as outlined from the Jetpack codebase:
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
private const val NOT_FOUND_STATUS_CODE = 404
private const val FORBIDDEN_CODE = 403

class FetchJetpackStatus @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val selectedSite: SelectedSite
) {
    enum class JetpackStatusFetchResponse {
        SUCCESS, NOT_FOUND, FORBIDDEN
    }
    suspend operator fun invoke(): Result<Pair<JetpackStatus, JetpackStatusFetchResponse>> {
        return jetpackStore.fetchJetpackUser(selectedSite.get()).let { result ->
            when {
                result.error?.errorCode == NOT_FOUND_STATUS_CODE -> {
                    Result.success(
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
