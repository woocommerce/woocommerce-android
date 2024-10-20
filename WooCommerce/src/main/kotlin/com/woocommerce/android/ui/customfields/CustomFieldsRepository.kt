package com.woocommerce.android.ui.customfields

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import org.wordpress.android.fluxc.store.MetaDataStore
import javax.inject.Inject

class CustomFieldsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val metaDataStore: MetaDataStore
) {
    fun observeDisplayableCustomFields(
        parentItemId: Long,
    ): Flow<List<CustomField>> = metaDataStore.observeDisplayableMetaData(selectedSite.get(), parentItemId)

    suspend fun getDisplayableCustomFields(
        parentItemId: Long,
    ): List<CustomField> = metaDataStore.getDisplayableMetaData(selectedSite.get(), parentItemId)

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

    suspend fun updateCustomFields(request: UpdateMetadataRequest): Result<Unit> {
        return metaDataStore.updateMetaData(selectedSite.get(), request).let {
            if (it.isError) {
                WooLog.w(WooLog.T.CUSTOM_FIELDS, "Failed to update custom fields: ${it.error}")
                Result.failure(WooException(it.error))
            } else {
                WooLog.d(WooLog.T.CUSTOM_FIELDS, "Successfully updated custom fields")
                Result.success(Unit)
            }
        }
    }

    suspend fun hasDisplayableCustomFields(
        parentItemId: Long,
    ) = metaDataStore.hasDisplayableMetaData(selectedSite.get(), parentItemId)

    suspend fun getCustomFieldById(
        parentItemId: Long,
        customFieldId: Long,
    ): CustomField? = metaDataStore.getMetaDataById(
        site = selectedSite.get(),
        parentItemId = parentItemId,
        metaDataId = customFieldId
    )
}
