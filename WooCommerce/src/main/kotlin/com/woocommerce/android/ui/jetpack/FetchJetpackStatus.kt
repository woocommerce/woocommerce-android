package com.woocommerce.android.ui.jetpack

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.JetpackStore
import javax.inject.Inject

private const val NOT_FOUND_STATUS_CODE = 404

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
                            isJetpackConnected = false
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
                            isJetpackConnected = result.user!!.isConnected
                        )
                    )
                }
            }
        }
    }
}
