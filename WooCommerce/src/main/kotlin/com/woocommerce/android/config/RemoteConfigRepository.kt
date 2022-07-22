package com.woocommerce.android.config

import com.woocommerce.android.experiment.PrologueVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun observePrologueVariant(): Flow<PrologueVariant>
    suspend fun updatePrologueVariantValue(variantValue: String)
}
