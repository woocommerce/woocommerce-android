package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import javax.inject.Inject

class SalesByChannelCustomDateRangeDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.SALES_BY_CHANNEL) dataStore: DataStore<CustomDateRange>
) : CustomDateRangeDataStore(dataStore)
