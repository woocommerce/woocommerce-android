package com.woocommerce.android.ui.mystore.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import javax.inject.Inject

class StatsCustomDateRangeDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.DASHBOARD_STATS) dataStore: DataStore<CustomDateRange>
) : CustomDateRangeDataStore(dataStore)
