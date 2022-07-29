package com.woocommerce.android.config

import com.woocommerce.android.experiment.PrologueVariant
import com.woocommerce.android.experiment.SiteLoginExperiment.SiteLoginVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun observePrologueVariant(): Flow<PrologueVariant>
    suspend fun updatePrologueVariantValue(variantValue: String)

    fun observeSiteLoginVariant(): Flow<SiteLoginVariant>
    suspend fun updateSiteLoginVariantValue(variantValue: String)
}
