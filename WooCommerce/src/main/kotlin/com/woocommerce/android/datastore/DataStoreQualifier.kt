package com.woocommerce.android.datastore

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DataStoreQualifier(val value: DataStoreType)
