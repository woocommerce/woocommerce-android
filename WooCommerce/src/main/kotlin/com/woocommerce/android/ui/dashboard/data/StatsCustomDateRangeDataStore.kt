package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import javax.inject.Inject

class StatsCustomDateRangeDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.DASHBOARD_STATS) dataStore: DataStore<CustomDateRange>
) : CustomDateRangeDataStore(dataStore)
