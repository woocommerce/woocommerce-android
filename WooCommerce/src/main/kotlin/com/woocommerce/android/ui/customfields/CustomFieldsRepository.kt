package com.woocommerce.android.ui.customfields

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.store.MetaDataStore
import javax.inject.Inject

class CustomFieldsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val metaDataStore: MetaDataStore
) {
    fun observeDisplayableCustomFields(
        parentItemId: Long,
    ) = metaDataStore.observeDisplayableMetaData(selectedSite.get(), parentItemId)

    suspend fun refreshCustomFields(
        parentItemId: Long,
        parentItemType: MetaDataParentItemType
    ): Result<Unit> {
        return metaDataStore.refreshMetaData(
            site = selectedSite.get(),
            parentItemId = parentItemId,
            parentItemType = parentItemType
        ).let {
            if (it.isError) {
                WooLog.w(WooLog.T.CUSTOM_FIELDS, "Failed to refresh custom fields: ${it.error}")
                Result.failure(WooException(it.error))
            } else {
                WooLog.d(WooLog.T.CUSTOM_FIELDS, "Successfully refreshed custom fields")
                Result.success(Unit)
            }
        }
    }

    suspend fun hasDisplayableCustomFields(
        parentItemId: Long,
    ) = metaDataStore.hasDisplayableMetaData(selectedSite.get(), parentItemId)
}
