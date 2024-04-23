package com.woocommerce.android.ui.mystore.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import javax.inject.Inject

class TopPerformersCustomDateRangeDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.TOP_PERFORMER_PRODUCTS) dataStore: DataStore<CustomDateRange>
) : CustomDateRangeDataStore(dataStore)
