package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.model.settings.AnalyticsScheduledImportSettingEntity
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelCreationEligibility
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.model.taxes.TaxBasedOnSettingEntity
import org.wordpress.android.fluxc.model.taxes.TaxRateEntity
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.persistence.converters.BigDecimalConverter
import org.wordpress.android.fluxc.persistence.converters.CurrencyPositionConverter
import org.wordpress.android.fluxc.persistence.converters.LocalIdConverter
import org.wordpress.android.fluxc.persistence.converters.LongListConverter
import org.wordpress.android.fluxc.persistence.converters.RemoteIdConverter
import org.wordpress.android.fluxc.persistence.converters.StatsGranularityConverter
import org.wordpress.android.fluxc.persistence.converters.StringListConverter
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.AnalyticsScheduledImportDao
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.CustomerDao
import org.wordpress.android.fluxc.persistence.dao.CustomerFromAnalyticsDao
import org.wordpress.android.fluxc.persistence.dao.GatewaysDao
import org.wordpress.android.fluxc.persistence.dao.GlobalAttributesDao
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.persistence.dao.LocationsDao
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.dao.NewVisitorStatsDao
import org.wordpress.android.fluxc.persistence.dao.OrderNotesDao
import org.wordpress.android.fluxc.persistence.dao.OrderShipmentProvidersDao
import org.wordpress.android.fluxc.persistence.dao.OrderShipmentTrackingDao
import org.wordpress.android.fluxc.persistence.dao.OrderStatusDao
import org.wordpress.android.fluxc.persistence.dao.OrderSummaryDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.dao.ProductReviewsDao
import org.wordpress.android.fluxc.persistence.dao.ProductSettingsDao
import org.wordpress.android.fluxc.persistence.dao.ProductShippingClassesDao
import org.wordpress.android.fluxc.persistence.dao.ProductTagsDao
import org.wordpress.android.fluxc.persistence.dao.ProductVariationsDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.dao.RefundDao
import org.wordpress.android.fluxc.persistence.dao.RevenueStatsDao
import org.wordpress.android.fluxc.persistence.dao.SettingsDao
import org.wordpress.android.fluxc.persistence.dao.ShippingLabelCreationEligibilityDao
import org.wordpress.android.fluxc.persistence.dao.ShippingLabelDao
import org.wordpress.android.fluxc.persistence.dao.ShippingMethodDao
import org.wordpress.android.fluxc.persistence.dao.SupportChatBookmarkDao
import org.wordpress.android.fluxc.persistence.dao.TaxBasedOnDao
import org.wordpress.android.fluxc.persistence.dao.TaxClassDao
import org.wordpress.android.fluxc.persistence.dao.TaxRateDao
import org.wordpress.android.fluxc.persistence.dao.TopPerformerProductsDao
import org.wordpress.android.fluxc.persistence.dao.UserDao
import org.wordpress.android.fluxc.persistence.dao.VisitorSummaryStatsDao
import org.wordpress.android.fluxc.persistence.dao.WooPaymentsDepositsOverviewDao
import org.wordpress.android.fluxc.persistence.dao.WooPushNotificationPreferencesDao
import org.wordpress.android.fluxc.persistence.dao.WooShippingDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosSearchableFtsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CustomerFromAnalyticsEntity
import org.wordpress.android.fluxc.persistence.entity.GatewayEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.persistence.entity.RefundEntity
import org.wordpress.android.fluxc.persistence.entity.ShippingMethodEntity
import org.wordpress.android.fluxc.persistence.entity.SupportChatBookmarkEntity
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.persistence.entity.VisitorSummaryStatsEntity
import org.wordpress.android.fluxc.persistence.entity.WCSettingsModel
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsBalanceEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsOverviewEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsManualDepositEntity
import org.wordpress.android.fluxc.persistence.entity.WooPushNotificationPreferencesEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingLabelEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingShipmentEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosSearchableFtsEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration13to14
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration14to15
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration16to17
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration17to18
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration18to19
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration19to20
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration23to24
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration32to33
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration37to38
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration89to90
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_10_11
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_11_12
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_15_16
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_20_21
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_21_22
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_22_23
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_24_25
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_27_28
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_30_31
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_31_32
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_3_4
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_4_5
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_5_6
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_62_63
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_6_7
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_71_72
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_77_78
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_79_80
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_7_8
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_8_9
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_9_10

const val WC_DATABASE_VERSION = 91

// Matches the CursorWindow size used by WooWellSqlConfig; raises SQLite's ~2 MB default on API 28+.
@Suppress("MagicNumber")
private val CURSOR_WINDOW_SIZE_BYTES = 1024L * 1024L * 10L

@Database(
    version = WC_DATABASE_VERSION,
    entities = [
        AddonEntity::class,
        AddonOptionEntity::class,
        CouponEntity::class,
        CouponEmailEntity::class,
        GlobalAddonGroupEntity::class,
        OrderNoteEntity::class,
        OrderEntity::class,
        RefundEntity::class,
        MetaDataEntity::class,
        InboxNoteEntity::class,
        InboxNoteActionEntity::class,
        TopPerformerProductEntity::class,
        TaxBasedOnSettingEntity::class,
        TaxRateEntity::class,
        WooPaymentsDepositsOverviewEntity::class,
        WooPaymentsDepositEntity::class,
        WooPaymentsManualDepositEntity::class,
        WooPaymentsBalanceEntity::class,
        VisitorSummaryStatsEntity::class,
        ShippingMethodEntity::class,
        CustomerFromAnalyticsEntity::class,
        WCProductModel::class,
        WooPosProductEntity::class,
        WooPosVariationEntity::class,
        WooPosSearchableFtsEntity::class,
        WCProductCategoryModel::class,
        WCProductVariationModel::class,
        WCProductTagModel::class,
        WCProductShippingClassModel::class,
        WCProductReviewModel::class,
        WCProductSettingsModel::class,
        WCCustomerModel::class,
        WCLocationModel::class,
        WCOrderShipmentProviderModel::class,
        WCOrderShipmentTrackingModel::class,
        WCUserModel::class,
        WCTaxClassModel::class,
        WCSettingsModel::class,
        WCOrderSummaryModel::class,
        WCOrderStatusModel::class,
        WooShippingLabelEntity::class,
        WooShippingShipmentEntity::class,
        WooShippingPackagesEntity::class,
        WCGlobalAttributeModel::class,
        GatewayEntity::class,
        WCNewVisitorStatsModel::class,
        WCRevenueStatsModel::class,
        WCShippingLabelModel::class,
        WCShippingLabelCreationEligibility::class,
        WooPushNotificationPreferencesEntity::class,
        SupportChatBookmarkEntity::class,
        AnalyticsScheduledImportSettingEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14, spec = AutoMigration13to14::class),
        AutoMigration(from = 14, to = 15, spec = AutoMigration14to15::class),
        AutoMigration(from = 16, to = 17, spec = AutoMigration16to17::class),
        AutoMigration(from = 17, to = 18, spec = AutoMigration17to18::class),
        AutoMigration(from = 18, to = 19, spec = AutoMigration18to19::class),
        AutoMigration(from = 19, to = 20, spec = AutoMigration19to20::class),
        AutoMigration(from = 23, to = 24, spec = AutoMigration23to24::class),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 28, to = 29),
        AutoMigration(from = 29, to = 30),
        AutoMigration(from = 31, to = 32),
        AutoMigration(from = 32, to = 33, spec = AutoMigration32to33::class),
        AutoMigration(from = 33, to = 34),
        AutoMigration(from = 34, to = 35),
        AutoMigration(from = 35, to = 36),
        AutoMigration(from = 36, to = 37),
        AutoMigration(from = 37, to = 38, spec = AutoMigration37to38::class),
        AutoMigration(from = 38, to = 39),
        AutoMigration(from = 39, to = 40),
        AutoMigration(from = 40, to = 41),
        AutoMigration(from = 41, to = 42),
        AutoMigration(from = 42, to = 43),
        AutoMigration(from = 43, to = 44),
        AutoMigration(from = 44, to = 45),
        AutoMigration(from = 45, to = 46),
        AutoMigration(from = 46, to = 47),
        AutoMigration(from = 47, to = 48),
        AutoMigration(from = 48, to = 49),
        AutoMigration(from = 49, to = 50),
        AutoMigration(from = 50, to = 51),
        AutoMigration(from = 51, to = 52),
        AutoMigration(from = 52, to = 53),
        AutoMigration(from = 53, to = 54),
        AutoMigration(from = 54, to = 55),
        AutoMigration(from = 55, to = 56),
        AutoMigration(from = 56, to = 57),
        AutoMigration(from = 57, to = 58),
        AutoMigration(from = 58, to = 59),
        AutoMigration(from = 59, to = 60),
        AutoMigration(from = 60, to = 61),
        AutoMigration(from = 61, to = 62),
        AutoMigration(from = 63, to = 64),
        AutoMigration(from = 64, to = 65),
        AutoMigration(from = 65, to = 66),
        AutoMigration(from = 66, to = 67),
        AutoMigration(from = 67, to = 68),
        AutoMigration(from = 68, to = 69),
        AutoMigration(from = 69, to = 70),
        AutoMigration(from = 70, to = 71),
        AutoMigration(from = 72, to = 73),
        AutoMigration(from = 73, to = 74),
        AutoMigration(from = 74, to = 75),
        AutoMigration(from = 75, to = 76),
        AutoMigration(from = 76, to = 77),
        AutoMigration(from = 78, to = 79),
        AutoMigration(from = 80, to = 81),
        AutoMigration(from = 81, to = 82),
        AutoMigration(from = 82, to = 83),
        AutoMigration(from = 83, to = 84),
        AutoMigration(from = 84, to = 85),
        AutoMigration(from = 85, to = 86),
        AutoMigration(from = 86, to = 87),
        AutoMigration(from = 87, to = 88),
        AutoMigration(from = 88, to = 89),
        AutoMigration(from = 89, to = 90, spec = AutoMigration89to90::class),
        AutoMigration(from = 90, to = 91),
    ]
)
@TypeConverters(
    value = [
        LocalIdConverter::class,
        LongListConverter::class,
        StringListConverter::class,
        RemoteIdConverter::class,
        BigDecimalConverter::class,
        CurrencyPositionConverter::class,
        StatsGranularityConverter::class,
    ]
)
abstract class WCAndroidDatabase : RoomDatabase(), TransactionExecutor {
    abstract val addonsDao: AddonsDao
    abstract val ordersDao: OrdersDao
    abstract val orderNotesDao: OrderNotesDao
    abstract val metaDataDao: MetaDataDao
    abstract val couponsDao: CouponsDao
    abstract val inboxNotesDao: InboxNotesDao
    abstract val topPerformerProductsDao: TopPerformerProductsDao
    abstract val taxBasedOnSettingDao: TaxBasedOnDao
    abstract val taxRateDao: TaxRateDao
    abstract val wooPaymentsDepositsOverviewDao: WooPaymentsDepositsOverviewDao
    abstract val visitorSummaryStatsDao: VisitorSummaryStatsDao
    abstract val shippingMethodDao: ShippingMethodDao
    abstract val customerFromAnalyticsDao: CustomerFromAnalyticsDao
    internal abstract val locationsDao: LocationsDao
    internal abstract val orderShipmentProvidersDao: OrderShipmentProvidersDao
    internal abstract val orderShipmentTrackingDao: OrderShipmentTrackingDao
    internal abstract val customerDao: CustomerDao
    internal abstract val productsDao: ProductsDao
    internal abstract val posProductsDao: WooPosProductsDao
    internal abstract val posVariationsDao: WooPosVariationsDao
    internal abstract val posSearchableFtsDao: WooPosSearchableFtsDao
    internal abstract val productVariationsDao: ProductVariationsDao
    internal abstract val productCategoriesDao: ProductCategoriesDao
    internal abstract val productTagsDao: ProductTagsDao
    internal abstract val productShippingClassesDao: ProductShippingClassesDao
    internal abstract val productReviewsDao: ProductReviewsDao
    internal abstract val productSettingsDao: ProductSettingsDao
    internal abstract val taxClassDao: TaxClassDao
    internal abstract val userDao: UserDao
    internal abstract val settingsDao: SettingsDao
    internal abstract val refundDao: RefundDao
    internal abstract val orderSummaryDao: OrderSummaryDao
    internal abstract val orderStatusDao: OrderStatusDao
    abstract val wooShippingDao: WooShippingDao
    internal abstract val globalAttributesDao: GlobalAttributesDao
    internal abstract val gatewaysDao: GatewaysDao
    internal abstract val newVisitorStatsDao: NewVisitorStatsDao
    internal abstract val revenueStatsDao: RevenueStatsDao
    internal abstract val shippingLabelDao: ShippingLabelDao
    internal abstract val shippingLabelCreationEligibilityDao: ShippingLabelCreationEligibilityDao
    internal abstract val wooPushNotificationPreferencesDao: WooPushNotificationPreferencesDao
    abstract val supportChatBookmarkDao: SupportChatBookmarkDao
    abstract val analyticsScheduledImportDao: AnalyticsScheduledImportDao

    companion object {
        fun buildDb(
            applicationContext: Context,
            currencyPositionConverter: CurrencyPositionConverter,
            statsGranularityConverter: StatsGranularityConverter,
        ) = Room.databaseBuilder(
            applicationContext,
            WCAndroidDatabase::class.java,
            "wc-android-database"
        ).openHelperFactory(LargeCursorWindowOpenHelperFactory(CURSOR_WINDOW_SIZE_BYTES))
            .allowMainThreadQueries()
            .addTypeConverter(currencyPositionConverter)
            .addTypeConverter(statsGranularityConverter)
            .fallbackToDestructiveMigrationOnDowngrade()
            .fallbackToDestructiveMigrationFrom(1, 2)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .addMigrations(MIGRATION_7_8)
            .addMigrations(MIGRATION_8_9)
            .addMigrations(MIGRATION_9_10)
            .addMigrations(MIGRATION_10_11)
            .addMigrations(MIGRATION_11_12)
            .addMigrations(MIGRATION_15_16)
            .addMigrations(MIGRATION_20_21)
            .addMigrations(MIGRATION_21_22)
            .addMigrations(MIGRATION_22_23)
            .addMigrations(MIGRATION_24_25)
            .addMigrations(MIGRATION_27_28)
            .addMigrations(MIGRATION_30_31)
            .addMigrations(MIGRATION_31_32)
            .addMigrations(MIGRATION_62_63)
            .addMigrations(MIGRATION_71_72)
            .addMigrations(MIGRATION_77_78)
            .addMigrations(MIGRATION_79_80)
            .build()
    }

    override suspend fun <R> executeInTransaction(block: suspend () -> R): R =
        withTransaction(block)

    override fun <R> runInTransaction(block: () -> R): R = runInTransaction(block)
}
