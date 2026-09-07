package org.wordpress.android.fluxc.di

import android.content.Context
import android.database.sqlite.SQLiteDiskIOException
import dagger.Binds
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.module.DatabaseModule
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.converters.CurrencyPositionConverter
import org.wordpress.android.fluxc.persistence.converters.StatsGranularityConverter
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.CustomerFromAnalyticsDao
import org.wordpress.android.fluxc.persistence.dao.FilterHistoryDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.dao.ShippingMethodDao
import org.wordpress.android.fluxc.persistence.dao.SupportChatBookmarkDao
import org.wordpress.android.fluxc.persistence.dao.WooPushNotificationPreferencesDao
import org.wordpress.android.util.AppLog
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Module(
    includes = [
        DatabaseModule::class
    ]
)
interface WCDatabaseModule {
    companion object {
        @Singleton
        @Provides
        fun provideDatabase(
            context: Context,
            currencyPositionConverter: CurrencyPositionConverter,
            statsGranularityConverter: StatsGranularityConverter,
        ): WCAndroidDatabase {
            return try {
                WCAndroidDatabase.buildDb(
                    context,
                    currencyPositionConverter,
                    statsGranularityConverter,
                )
            } catch (e: SQLiteDiskIOException) {
                AppLog.e(
                    AppLog.T.DB,
                    "Database open failed due to disk I/O error, freeing space and retrying: $e"
                )
                clearLogFiles(context)
                try {
                    WCAndroidDatabase.buildDb(
                        context,
                        currencyPositionConverter,
                        statsGranularityConverter,
                    )
                } catch (retryException: SQLiteDiskIOException) {
                    AppLog.e(
                        AppLog.T.DB,
                        "Database retry failed after clearing logs: $retryException"
                    )
                    throw retryException
                }
            }
        }

        private fun clearLogFiles(context: Context) {
            val logsDir = File(context.filesDir, "logs")
            logsDir.listFiles { file -> file.isFile && file.name.startsWith("log_") }
                ?.forEach { it.delete() }
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
         * ProductSqlUtils is a Kotlin object, we can't use [Inject] to inject it.
         */
        @Provides internal fun provideProductSqlUtils() = ProductSqlUtils

        @Provides fun provideVisitorSummaryStatsDao(database: WCAndroidDatabase) = database.visitorSummaryStatsDao

        @Provides internal fun provideShippingMethodsDao(database: WCAndroidDatabase): ShippingMethodDao {
            return database.shippingMethodDao
        }

        @Provides internal fun provideCustomerFromAnalyticsDao(database: WCAndroidDatabase): CustomerFromAnalyticsDao {
            return database.customerFromAnalyticsDao
        }

        @Provides internal fun provideCustomerDao(database: WCAndroidDatabase) = database.customerDao

        @Provides internal fun provideProductsDao(database: WCAndroidDatabase) = database.productsDao

        @Provides internal fun provideProductVariationsDao(database: WCAndroidDatabase) = database.productVariationsDao

        @Provides internal fun provideProductCategoriesDao(database: WCAndroidDatabase) = database.productCategoriesDao

        @Provides internal fun provideProductTagsDao(database: WCAndroidDatabase) = database.productTagsDao

        @Provides
        internal fun provideProductShippingClassesDao(database: WCAndroidDatabase) = database.productShippingClassesDao

        @Provides internal fun provideProductReviewsDao(database: WCAndroidDatabase) = database.productReviewsDao

        @Provides internal fun provideProductSettingsDao(database: WCAndroidDatabase) = database.productSettingsDao

        @Provides internal fun provideUserDao(database: WCAndroidDatabase) = database.userDao

        @Provides internal fun provideLocationsDao(database: WCAndroidDatabase) = database.locationsDao

        @Provides internal fun provideOrderShipmentProvidersDao(database: WCAndroidDatabase) = database.orderShipmentProvidersDao

        @Provides internal fun provideOrderShipmentTrackingDao(database: WCAndroidDatabase) = database.orderShipmentTrackingDao

        @Provides internal fun provideTaxClassDao(database: WCAndroidDatabase) = database.taxClassDao

        @Provides internal fun provideWCSettingsDao(database: WCAndroidDatabase) = database.settingsDao

        @Provides internal fun provideRefundDao(database: WCAndroidDatabase) = database.refundDao

        @Provides internal fun provideOrderSummaryDao(database: WCAndroidDatabase) = database.orderSummaryDao

        @Provides internal fun provideOrderStatusDao(database: WCAndroidDatabase) = database.orderStatusDao

        @Provides internal fun provideWooShippingDao(database: WCAndroidDatabase) = database.wooShippingDao

        @Provides internal fun provideGlobalAttributesDao(database: WCAndroidDatabase) = database.globalAttributesDao

        @Provides internal fun providePosProductsDao(database: WCAndroidDatabase) = database.posProductsDao

        @Provides internal fun providePosVariationsDao(database: WCAndroidDatabase) = database.posVariationsDao

        @Provides internal fun providePosSearchableFtsDao(database: WCAndroidDatabase) = database.posSearchableFtsDao

        @Provides internal fun provideGatewaysDao(database: WCAndroidDatabase) = database.gatewaysDao

        @Provides internal fun provideNewVisitorStatsDao(database: WCAndroidDatabase) = database.newVisitorStatsDao

        @Provides internal fun provideRevenueStatsDao(database: WCAndroidDatabase) = database.revenueStatsDao

        @Provides internal fun provideShippingLabelDao(database: WCAndroidDatabase) = database.shippingLabelDao

        @Provides internal fun provideShippingLabelCreationEligibilityDao(database: WCAndroidDatabase) =
            database.shippingLabelCreationEligibilityDao

        @Provides
        internal fun provideWooPushNotificationPreferencesDao(
            database: WCAndroidDatabase
        ): WooPushNotificationPreferencesDao = database.wooPushNotificationPreferencesDao

        @Provides fun provideSupportChatBookmarkDao(database: WCAndroidDatabase): SupportChatBookmarkDao {
            return database.supportChatBookmarkDao
        }

        @Provides fun provideFilterHistoryDao(database: WCAndroidDatabase): FilterHistoryDao {
            return database.filterHistoryDao
        }

        @Provides fun provideAnalyticsScheduledImportDao(database: WCAndroidDatabase) =
            database.analyticsScheduledImportDao
    }
    @Binds fun bindTransactionExecutor(database: WCAndroidDatabase): TransactionExecutor
}
