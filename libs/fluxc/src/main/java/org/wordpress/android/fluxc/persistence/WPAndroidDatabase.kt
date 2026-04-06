package org.wordpress.android.fluxc.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlag
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeObjectivesDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeObjectivesDao.BlazeCampaignObjectiveEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDeviceEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingLanguageEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingTopicEntity
import org.wordpress.android.fluxc.persistence.converters.AppVersionTargetsConverter
import org.wordpress.android.fluxc.persistence.converters.LocalIdConverter
import org.wordpress.android.fluxc.persistence.converters.RemoteIdConverter
import org.wordpress.android.fluxc.persistence.coverters.StringListConverter
import org.wordpress.android.fluxc.persistence.dao.AccountDao
import org.wordpress.android.fluxc.persistence.dao.ListDao
import org.wordpress.android.fluxc.persistence.dao.NotificationDao
import org.wordpress.android.fluxc.persistence.dao.SiteDao
import org.wordpress.android.fluxc.persistence.dao.ThemeDao
import org.wordpress.android.fluxc.persistence.dao.WhatsNewDao
import org.wordpress.android.fluxc.persistence.domains.DomainDao
import org.wordpress.android.fluxc.persistence.domains.DomainDao.DomainEntity
import org.wordpress.android.fluxc.persistence.entity.AccountEntity
import org.wordpress.android.fluxc.persistence.entity.NotificationEntity
import org.wordpress.android.fluxc.persistence.entity.SiteEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementFeatureEntity

@Database(
        version = 38,
        entities = [
            AccountEntity::class,
            FeatureFlag::class,
            DomainEntity::class,
            BlazeCampaignEntity::class,
            BlazeCampaignObjectiveEntity::class,
            BlazeTargetingLanguageEntity::class,
            BlazeTargetingDeviceEntity::class,
            BlazeTargetingTopicEntity::class,
            ThemeModel::class,
            WhatsNewAnnouncementEntity::class,
            WhatsNewAnnouncementFeatureEntity::class,
            SitePluginModel::class,
            ListModel::class,
            ListItemModel::class,
            NotificationEntity::class,
            SiteEntity::class,
        ],
        autoMigrations = [
            AutoMigration(from = 11, to = 12),
            AutoMigration(from = 12, to = 13),
            AutoMigration(from = 13, to = 14),
            AutoMigration(from = 16, to = 17),
            AutoMigration(from = 17, to = 18),
            AutoMigration(from = 22, to = 23),
            AutoMigration(from = 23, to = 24),
            AutoMigration(from = 24, to = 25),
            AutoMigration(from = 25, to = 26, spec = AutoMigration25to26::class),
            AutoMigration(from = 27, to = 28),
            AutoMigration(from = 28, to = 29),
            AutoMigration(from = 29, to = 30, spec = AutoMigration29to30::class),
            AutoMigration(from = 30, to = 31),
            AutoMigration(from = 31, to = 32),
            AutoMigration(from = 32, to = 33),
            AutoMigration(from = 33, to = 34),
            AutoMigration(from = 34, to = 35),
            AutoMigration(from = 35, to = 36),
            AutoMigration(from = 36, to = 37),
        ]
)
@TypeConverters(
    value = [
        StringListConverter::class,
        LocalIdConverter::class,
        AppVersionTargetsConverter::class,
        RemoteIdConverter::class
    ]
)
abstract class WPAndroidDatabase : RoomDatabase() {
    internal abstract fun accountDao(): AccountDao

    abstract fun featureFlagConfigDao(): FeatureFlagConfigDao

    abstract fun domainDao(): DomainDao

    abstract fun blazeCampaignsDao(): BlazeCampaignsDao

    abstract fun blazeTargetingDao(): BlazeTargetingDao

    abstract fun blazeObjectivesDao(): BlazeObjectivesDao

    internal abstract fun listDao(): ListDao

    internal abstract fun themeDao(): ThemeDao

    internal abstract fun whatsNewDao(): WhatsNewDao

    internal abstract fun notificationDao(): NotificationDao

    abstract fun sitePluginDao(): SitePluginDao

    abstract fun siteDao(): SiteDao

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        const val WP_DB_NAME = "wp-android-database"

        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
            applicationContext,
            WPAndroidDatabase::class.java,
            WP_DB_NAME
        )
                .fallbackToDestructiveMigration()
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_14_15)
                .addMigrations(MIGRATION_15_16)
                .addMigrations(MIGRATION_18_19)
                .addMigrations(MIGRATION_19_20)
                .addMigrations(MIGRATION_20_21)
                .addMigrations(MIGRATION_26_27)
                .addMigrations(migration37To38(applicationContext))
                .build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `PlanOffers` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`internalPlanId` INTEGER NOT NULL, " +
                            "`name` TEXT, " +
                            "`shortName` TEXT, " +
                            "`tagline` TEXT, " +
                            "`description` TEXT, " +
                            "`icon` TEXT" +
                            ")"
                    )
                    execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_PlanOffers_internalPlanId` " +
                            "ON `PlanOffers` (`internalPlanId`)"
                    )
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `PlanOfferIds` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`productId` INTEGER NOT NULL, " +
                            "`internalPlanId` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`internalPlanId`) REFERENCES `PlanOffers`(`internalPlanId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE" +
                            ")"
                    )
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `PlanOfferFeatures` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`internalPlanId` INTEGER NOT NULL, " +
                            "`stringId` TEXT, " +
                            "`name` TEXT, " +
                            "`description` TEXT, " +
                            "FOREIGN KEY(`internalPlanId`) REFERENCES `PlanOffers`(`internalPlanId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE" +
                            ")"
                    )
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `Comments` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`remoteCommentId` INTEGER NOT NULL, " +
                            "`remotePostId` INTEGER NOT NULL, " +
                            "`remoteParentCommentId` INTEGER NOT NULL, " +
                            "`localSiteId` INTEGER NOT NULL, " +
                            "`remoteSiteId` INTEGER NOT NULL, " +
                            "`authorUrl` TEXT, " +
                            "`authorName` TEXT, " +
                            "`authorEmail` TEXT, " +
                            "`authorProfileImageUrl` TEXT, " +
                            "`postTitle` TEXT, " +
                            "`status` TEXT, " +
                            "`datePublished` TEXT, " +
                            "`publishedTimestamp` INTEGER NOT NULL, " +
                            "`content` TEXT, " +
                            "`url` TEXT, " +
                            "`hasParent` INTEGER NOT NULL, " +
                            "`parentId` INTEGER NOT NULL, " +
                            "`iLike` INTEGER NOT NULL)"
                    )
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE BloggingReminders ADD COLUMN hour INTEGER DEFAULT 10 NOT NULL")
                    execSQL("ALTER TABLE BloggingReminders ADD COLUMN minute INTEGER DEFAULT 0 NOT NULL")
                }
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("DROP TABLE Comments")
                    execSQL(
                        "CREATE TABLE `Comments` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`remoteCommentId` INTEGER NOT NULL, " +
                            "`remotePostId` INTEGER NOT NULL, " +
                            "`localSiteId` INTEGER NOT NULL, " +
                            "`remoteSiteId` INTEGER NOT NULL, " +
                            "`authorUrl` TEXT, " +
                            "`authorName` TEXT, " +
                            "`authorEmail` TEXT, " +
                            "`authorProfileImageUrl` TEXT, " +
                            "`authorId` INTEGER NOT NULL , " +
                            "`postTitle` TEXT, " +
                            "`status` TEXT, " +
                            "`datePublished` TEXT, " +
                            "`publishedTimestamp` INTEGER NOT NULL, " +
                            "`content` TEXT, " +
                            "`url` TEXT, " +
                            "`hasParent` INTEGER NOT NULL, " +
                            "`parentId` INTEGER NOT NULL, " +
                            "`iLike` INTEGER NOT NULL)"
                    )
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "ALTER TABLE BloggingReminders ADD COLUMN isPromptRemindersOptedIn" +
                            " INTEGER DEFAULT 0 NOT NULL"
                    )
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14,15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "DROP TABLE IF EXISTS `BlazeStatus`"
                    )
                }
            }
        }

        val MIGRATION_15_16 = object : Migration(15,16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "DROP TABLE IF EXISTS `BlazeStatus`"
                    )
                }
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("DROP TABLE IF EXISTS `BlazeCampaigns`")
                    execSQL("DELETE FROM `BlazeCampaignsPagination`")
                    execSQL("CREATE TABLE `BlazeCampaigns` (" +
                        "`siteId` INTEGER NOT NULL, " +
                        "`campaignId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`imageUrl` TEXT, " +
                        "`startDate` TEXT NOT NULL, " +
                        "`endDate` TEXT, " +
                        "`uiStatus` TEXT NOT NULL, " +
                        "`budgetCents` INTEGER NOT NULL, " +
                        "`impressions` INTEGER NOT NULL, " +
                        "`clicks` INTEGER NOT NULL, " +
                        "PRIMARY KEY (`siteId`, `campaignId`)" +
                        ")"
                    )
                    execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_BlazeCampaigns_siteId` " +
                            "ON `BlazeCampaigns` (`siteId`)"
                    )
                }
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("DROP TABLE IF EXISTS `BlazeCampaigns`")
                    execSQL("DELETE FROM `BlazeCampaignsPagination`")
                    execSQL("CREATE TABLE `BlazeCampaigns` (" +
                            "`siteId` INTEGER NOT NULL, " +
                            "`campaignId` INTEGER NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`imageUrl` TEXT, " +
                            "`createdAt` TEXT NOT NULL, " +
                            "`endDate` TEXT, " +
                            "`uiStatus` TEXT NOT NULL, " +
                            "`budgetCents` INTEGER NOT NULL, " +
                            "`impressions` INTEGER NOT NULL, " +
                            "`clicks` INTEGER NOT NULL, " +
                            "PRIMARY KEY (`siteId`, `campaignId`)" +
                            ")"
                    )
                    execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_BlazeCampaigns_siteId` " +
                                "ON `BlazeCampaigns` (`siteId`)"
                    )
                }
            }
        }
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "ALTER TABLE `BlazeCampaigns` ADD COLUMN `targetUrn` TEXT"
                    )
                }
            }
        }
        // Room column name → WellSQL column name.
        // Each line is one reviewable mapping; SQL is derived from this map at runtime.
        // CRITICAL: "id" → "_id" preserves localSiteId so WC table foreign keys stay valid.
        // Columns here follow SiteEntity field order. INTEGER = boolean/numeric, TEXT = string.
        private val SITE_INTEGER_COLUMNS = linkedMapOf(
            "id"                         to "_id",         // Primary key — always non-null
            "siteId"                     to "SITE_ID",
            "isWPCom"                    to "IS_WPCOM",
            "isWPComAtomic"              to "IS_WPCOM_ATOMIC",
            "publishedStatus"            to "PUBLISHED_STATUS",
            "origin"                     to "ORIGIN",
            "selfHostedSiteId"           to "SELF_HOSTED_SITE_ID",
            "isJetpackInstalled"         to "IS_JETPACK_INSTALLED",
            "isJetpackConnected"         to "IS_JETPACK_CONNECTED",
            "isJetpackCPConnected"       to "IS_JETPACK_CP_CONNECTED",
            "isWpComStore"               to "IS_WP_COM_STORE",
            "hasWooCommerce"             to "HAS_WOO_COMMERCE",
            "isPrivate"                  to "IS_PRIVATE",
            "planId"                     to "PLAN_ID",
            "hasCapabilityManageOptions" to "HAS_CAPABILITY_MANAGE_OPTIONS",
            "canBlaze"                   to "CAN_BLAZE",
            "isGardenSite"               to "IS_GARDEN_SITE",
        )

        private val SITE_TEXT_COLUMNS = linkedMapOf(
            "url"                              to "URL",
            "adminUrl"                         to "ADMIN_URL",
            "loginUrl"                         to "LOGIN_URL",
            "name"                             to "NAME",
            "timezone"                         to "TIMEZONE",
            "username"                         to "USERNAME",
            "password"                         to "PASSWORD",
            "xmlRpcUrl"                        to "XMLRPC_URL",
            "wpApiRestUrl"                     to "WP_API_REST_URL",
            "email"                            to "EMAIL",
            "displayName"                      to "DISPLAY_NAME",
            "jetpackVersion"                   to "JETPACK_VERSION",
            "jetpackUserEmail"                 to "JETPACK_USER_EMAIL",
            "planShortName"                    to "PLAN_SHORT_NAME",
            "planProductSlug"                  to "PLAN_PRODUCT_SLUG",
            "activeJetpackConnectionPlugins"   to "ACTIVE_JETPACK_CONNECTION_PLUGINS",
            "jetpackModules"                   to "JETPACK_MODULES",
            "applicationPasswordsAuthorizeUrl" to "APPLICATION_PASSWORDS_AUTHORIZE_URL",
            "planActiveFeatures"               to "PLAN_ACTIVE_FEATURES",
            "gardenName"                       to "GARDEN_NAME",
            "gardenPartner"                    to "GARDEN_PARTNER",
        )

        fun migration37To38(context: Context) = object : Migration(37, 38) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `SiteEntity` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "`siteId` INTEGER NOT NULL," +
                        "`url` TEXT NOT NULL," +
                        "`adminUrl` TEXT NOT NULL," +
                        "`loginUrl` TEXT NOT NULL," +
                        "`name` TEXT NOT NULL," +
                        "`isWPCom` INTEGER NOT NULL," +
                        "`isWPComAtomic` INTEGER NOT NULL," +
                        "`publishedStatus` INTEGER NOT NULL," +
                        "`timezone` TEXT NOT NULL," +
                        "`origin` INTEGER NOT NULL," +
                        "`selfHostedSiteId` INTEGER NOT NULL," +
                        "`username` TEXT NOT NULL," +
                        "`password` TEXT NOT NULL," +
                        "`xmlRpcUrl` TEXT NOT NULL," +
                        "`wpApiRestUrl` TEXT NOT NULL," +
                        "`email` TEXT NOT NULL," +
                        "`displayName` TEXT NOT NULL," +
                        "`isJetpackInstalled` INTEGER NOT NULL," +
                        "`isJetpackConnected` INTEGER NOT NULL," +
                        "`isJetpackCPConnected` INTEGER NOT NULL," +
                        "`jetpackVersion` TEXT NOT NULL," +
                        "`jetpackUserEmail` TEXT NOT NULL," +
                        "`isWpComStore` INTEGER NOT NULL," +
                        "`hasWooCommerce` INTEGER NOT NULL," +
                        "`isPrivate` INTEGER NOT NULL," +
                        "`planId` INTEGER NOT NULL," +
                        "`planShortName` TEXT NOT NULL," +
                        "`planProductSlug` TEXT NOT NULL," +
                        "`hasCapabilityManageOptions` INTEGER NOT NULL," +
                        "`activeJetpackConnectionPlugins` TEXT NOT NULL," +
                        "`jetpackModules` TEXT NOT NULL," +
                        "`applicationPasswordsAuthorizeUrl` TEXT NOT NULL," +
                        "`canBlaze` INTEGER NOT NULL," +
                        "`planActiveFeatures` TEXT NOT NULL," +
                        "`isGardenSite` INTEGER NOT NULL," +
                        "`gardenName` TEXT NOT NULL," +
                        "`gardenPartner` TEXT NOT NULL" +
                        ")"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_SiteEntity_siteId_url` " +
                        "ON `SiteEntity` (`siteId`, `url`)"
                )

                val wellSqlFile = context.getDatabasePath("wp-fluxc")
                if (!wellSqlFile.exists()) return

                database.execSQL("ATTACH DATABASE '${wellSqlFile.absolutePath}' AS wellsql")
                try {
                    val tableExists = database.query(
                        "SELECT name FROM wellsql.sqlite_master WHERE type='table' AND name='SiteModel'"
                    ).use { it.count > 0 }
                    if (!tableExists) return

                    // Build SELECT expressions: INTEGER cols default to 0, TEXT cols to ''
                    val allColumns = (SITE_INTEGER_COLUMNS + SITE_TEXT_COLUMNS)
                    val roomCols = allColumns.keys.joinToString(", ") { "`$it`" }
                    val wellSqlExprs = buildString {
                        SITE_INTEGER_COLUMNS.entries.forEachIndexed { i, (_, wellSqlCol) ->
                            if (i > 0) append(", ")
                            append("COALESCE($wellSqlCol, 0)")
                        }
                        SITE_TEXT_COLUMNS.entries.forEach { (_, wellSqlCol) ->
                            append(", COALESCE($wellSqlCol, '')")
                        }
                    }
                    database.execSQL(
                        "INSERT OR IGNORE INTO SiteEntity ($roomCols) " +
                            "SELECT $wellSqlExprs FROM wellsql.SiteModel"
                    )
                } finally {
                    try { database.execSQL("DETACH DATABASE wellsql") } catch (_: Exception) {}
                }
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("DROP TABLE IF EXISTS `BlazeCampaigns`")
                    execSQL("DROP TABLE IF EXISTS `BlazeCampaignsPagination`")
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `BlazeCampaigns` (" +
                            "`siteId` INTEGER NOT NULL, " +
                            "`campaignId` TEXT NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`imageUrl` TEXT, " +
                            "`startTime` TEXT NOT NULL, " +
                            "`durationInDays` INTEGER NOT NULL, " +
                            "`uiStatus` TEXT NOT NULL, " +
                            "`impressions` INTEGER NOT NULL, " +
                            "`clicks` INTEGER NOT NULL, " +
                            "`targetUrn` TEXT, " +
                            "`totalBudget` REAL NOT NULL, " +
                            "`spentBudget` REAL NOT NULL, " +
                            "PRIMARY KEY (`siteId`, `campaignId`)" +
                            ")"
                    )
                    execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_BlazeCampaigns_siteId` " +
                                "ON `BlazeCampaigns` (`siteId`)"
                    )
                }
            }
        }
    }
}

@DeleteTable.Entries(
    DeleteTable(tableName = "BlazeAdSuggestions")
)
internal class AutoMigration25to26 : AutoMigrationSpec

@DeleteTable.Entries(
    DeleteTable(tableName = "BloggingPrompts"),
    DeleteTable(tableName = "BloggingReminders"),
    DeleteTable(tableName = "Comments"),
    DeleteTable(tableName = "DashboardCards"),
    DeleteTable(tableName = "JetpackCPConnectedSites"),
    DeleteTable(tableName = "JetpackSocial"),
    DeleteTable(tableName = "PlanOfferFeatures"),
    DeleteTable(tableName = "PlanOfferIds"),
    DeleteTable(tableName = "PlanOffers"),
    DeleteTable(tableName = "RemoteConfigurations"),
)
internal class AutoMigration29to30 : AutoMigrationSpec
