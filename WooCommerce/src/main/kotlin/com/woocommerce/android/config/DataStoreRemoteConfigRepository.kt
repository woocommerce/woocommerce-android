package com.woocommerce.android.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.experiment.PrologueVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreRemoteConfigRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.REMOTE_CONFIG) private val dataStore: DataStore<Preferences>
) : RemoteConfigRepository {
    companion object {
        const val PROLOGUE_VARIANT_KEY = "prologue_variant_key"
    }

    override fun observePrologueVariant(): Flow<PrologueVariant> {
        return dataStore.data.map { preferences ->
            PrologueVariant.valueOf(
                preferences[stringPreferencesKey(PROLOGUE_VARIANT_KEY)] ?: PrologueVariant.CONTROL.value
            )
        }
    }

    override suspend fun updatePrologueVariantValue(variantValue: String) {
        dataStore.edit { trackerPreferences ->
            trackerPreferences[stringPreferencesKey(PROLOGUE_VARIANT_KEY)] = variantValue
        }
    }
}
