package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import javax.inject.Inject

class CouponsCustomDateRangeDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.COUPONS) dataStore: DataStore<CustomDateRange>
) : CustomDateRangeDataStore(dataStore)
