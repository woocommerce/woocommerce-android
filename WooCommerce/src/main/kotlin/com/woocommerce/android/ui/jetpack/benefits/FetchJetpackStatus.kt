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
 * - 404: Jetpack is not installed.
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
    suspend operator fun invoke(): Result<JetpackStatus> {
        return jetpackStore.fetchJetpackUser(selectedSite.get()).let { result ->
            when {
                result.error?.errorCode == NOT_FOUND_STATUS_CODE -> {
                    Result.success(
                        JetpackStatus(
                            isJetpackInstalled = false,
                            isJetpackConnected = false,
                            wpComEmail = null
                        )
                    )
                }

                result.isError -> {
                    Result.failure(OnChangedException(result.error))
                }

                else -> {
                    Result.success(
                        JetpackStatus(
                            isJetpackInstalled = true,
                            isJetpackConnected = result.user!!.isConnected,
                            wpComEmail = result.user!!.wpcomEmail.orNullIfEmpty()
                        )
                    )
                }
            }
        }
    }
}
