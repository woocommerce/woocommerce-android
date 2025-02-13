package org.wordpress.android.fluxc.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.module.DatabaseModule
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.CustomerFromAnalyticsDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.dao.ShippingMethodDao
import javax.inject.Inject
import javax.inject.Singleton

@Module(
    includes = [
        DatabaseModule::class
    ]
)
interface WCDatabaseModule {
    companion object {
        @Singleton @Provides fun provideDatabase(context: Context): WCAndroidDatabase {
            return WCAndroidDatabase.buildDb(context)
        }

        @Provides internal fun provideAddonsDao(database: WCAndroidDatabase): AddonsDao {
            return database.addonsDao
        }

        @Provides fun provideOrdersDao(database: WCAndroidDatabase): OrdersDao {
            return database.ordersDao
        }

        @Provides fun provideCouponsDao(database: WCAndroidDatabase): CouponsDao {
            return database.couponsDao
        }

        @Provides fun provideOrderNotesDao(database: WCAndroidDatabase) = database.orderNotesDao

        @Provides fun provideMetaDataDao(database: WCAndroidDatabase) = database.metaDataDao

        @Provides fun provideInboxNotesDao(database: WCAndroidDatabase) = database.inboxNotesDao

        @Provides fun provideTopPerformerProductsDao(database: WCAndroidDatabase) = database.topPerformerProductsDao

        @Provides fun provideTaxBasedOnDao(database: WCAndroidDatabase) = database.taxBasedOnSettingDao

        @Provides fun provideTaxRateDao(database: WCAndroidDatabase) = database.taxRateDao

        @Provides fun provideWooPaymentsDepositsOverviewDao(database: WCAndroidDatabase) =
            database.wooPaymentsDepositsOverviewDao

        /**
         * OrderSqlUtils is a Kotlin object, we can't use [Inject] to inject it.
         */
        @Provides fun provideOrderSqlUtils() = OrderSqlUtils

        /**
         * ProductSqlUtils is a Kotlin object, we can't use [Inject] to inject it.
         */
        @Provides fun provideProductSqlUtils() = ProductSqlUtils

        @Provides fun provideVisitorSummaryStatsDao(database: WCAndroidDatabase) = database.visitorSummaryStatsDao

        @Provides internal fun provideShippingMethodsDao(database: WCAndroidDatabase): ShippingMethodDao {
            return database.shippingMethodDao
        }

        @Provides internal fun provideCustomerFromAnalyticsDao(database: WCAndroidDatabase): CustomerFromAnalyticsDao {
            return database.customerFromAnalyticsDao
        }
    }
    @Binds fun bindTransactionExecutor(database: WCAndroidDatabase): TransactionExecutor
}
