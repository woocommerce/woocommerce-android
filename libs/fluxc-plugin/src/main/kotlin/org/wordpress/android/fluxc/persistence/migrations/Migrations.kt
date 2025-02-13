package org.wordpress.android.fluxc.persistence.migrations

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            // language=RoomSql
            execSQL(
                """
                              CREATE TABLE IF NOT EXISTS `OrderEntity` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `localSiteId` INTEGER NOT NULL,
                                `remoteOrderId` INTEGER NOT NULL,
                                `number` TEXT NOT NULL, 
                                `status` TEXT NOT NULL,
                                `currency` TEXT NOT NULL,
                                `orderKey` TEXT NOT NULL,
                                `dateCreated` TEXT NOT NULL,
                                `dateModified` TEXT NOT NULL,
                                `total` TEXT NOT NULL, 
                                `totalTax` TEXT NOT NULL,
                                `shippingTotal` TEXT NOT NULL,
                                `paymentMethod` TEXT NOT NULL,
                                `paymentMethodTitle` TEXT NOT NULL,
                                `datePaid` TEXT NOT NULL,
                                `pricesIncludeTax` INTEGER NOT NULL,
                                `customerNote` TEXT NOT NULL,
                                `discountTotal` TEXT NOT NULL,
                                `discountCodes` TEXT NOT NULL,
                                `refundTotal` REAL NOT NULL,
                                `billingFirstName` TEXT NOT NULL,
                                `billingLastName` TEXT NOT NULL,
                                `billingCompany` TEXT NOT NULL,
                                `billingAddress1` TEXT NOT NULL,
                                `billingAddress2` TEXT NOT NULL,
                                `billingCity` TEXT NOT NULL,
                                `billingState` TEXT NOT NULL,
                                `billingPostcode` TEXT NOT NULL,
                                `billingCountry` TEXT NOT NULL,
                                `billingEmail` TEXT NOT NULL,
                                `billingPhone` TEXT NOT NULL,
                                `shippingFirstName` TEXT NOT NULL,
                                `shippingLastName` TEXT NOT NULL,
                                `shippingCompany` TEXT NOT NULL,
                                `shippingAddress1` TEXT NOT NULL,
                                `shippingAddress2` TEXT NOT NULL,
                                `shippingCity` TEXT NOT NULL,
                                `shippingState` TEXT NOT NULL,
                                `shippingPostcode` TEXT NOT NULL,
                                `shippingCountry` TEXT NOT NULL,
                                `shippingPhone` TEXT NOT NULL,
                                `lineItems` TEXT NOT NULL,
                                `shippingLines` TEXT NOT NULL,
                                `feeLines` TEXT NOT NULL,
                                `metaData` TEXT NOT NULL
                            );
                            """.trimIndent()
            )
            // language=RoomSql
            execSQL(
                """
                            CREATE UNIQUE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_remoteOrderId` 
                            ON `OrderEntity` (`localSiteId`, `remoteOrderId`);
                    """.trimIndent()
            )
        }
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE SSREntity")
    }
}

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE OrderEntity")
            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS OrderEntity (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `localSiteId` INTEGER NOT NULL,
                        `remoteOrderId` INTEGER NOT NULL,
                        `number` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `orderKey` TEXT NOT NULL,
                        `dateCreated` TEXT NOT NULL,
                        `dateModified` TEXT NOT NULL,
                        `total` TEXT NOT NULL,
                        `totalTax` TEXT NOT NULL,
                        `shippingTotal` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `paymentMethodTitle` TEXT NOT NULL,
                        `datePaid` TEXT NOT NULL,
                        `pricesIncludeTax` INTEGER NOT NULL,
                        `customerNote` TEXT NOT NULL,
                        `discountTotal` TEXT NOT NULL,
                        `discountCodes` TEXT NOT NULL,
                        `refundTotal` TEXT NOT NULL,
                        `billingFirstName` TEXT NOT NULL,
                        `billingLastName` TEXT NOT NULL,
                        `billingCompany` TEXT NOT NULL,
                        `billingAddress1` TEXT NOT NULL,
                        `billingAddress2` TEXT NOT NULL,
                        `billingCity` TEXT NOT NULL,
                        `billingState` TEXT NOT NULL,
                        `billingPostcode` TEXT NOT NULL,
                        `billingCountry` TEXT NOT NULL,
                        `billingEmail` TEXT NOT NULL,
                        `billingPhone` TEXT NOT NULL,
                        `shippingFirstName` TEXT NOT NULL,
                        `shippingLastName` TEXT NOT NULL,
                        `shippingCompany` TEXT NOT NULL,
                        `shippingAddress1` TEXT NOT NULL,
                        `shippingAddress2` TEXT NOT NULL,
                        `shippingCity` TEXT NOT NULL,
                        `shippingState` TEXT NOT NULL,
                        `shippingPostcode` TEXT NOT NULL,
                        `shippingCountry` TEXT NOT NULL,
                        `shippingPhone` TEXT NOT NULL,
                        `lineItems` TEXT NOT NULL,
                        `shippingLines` TEXT NOT NULL,
                        `feeLines` TEXT NOT NULL,
                        `metaData` TEXT NOT NULL)
                    """.trimIndent()
            )
            execSQL(
                // language=RoomSql
                """
                            CREATE UNIQUE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_remoteOrderId` 
                            ON `OrderEntity` (`localSiteId`, `remoteOrderId`);
                    """.trimIndent()
            )
        }
    }
}

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE OrderEntity ADD taxLines TEXT NOT NULL DEFAULT ''")
    }
}

internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS OrderNotes (
                siteId INTEGER NOT NULL,
                noteId INTEGER NOT NULL,
                orderId INTEGER NOT NULL,
                dateCreated TEXT,
                note TEXT,
                author TEXT,
                isSystemNote INTEGER NOT NULL,
                isCustomerNote INTEGER NOT NULL,
                PRIMARY KEY(`siteId`, `noteId`))
        """.trimIndent()
        )
    }
}

internal val MIGRATION_8_9 = object : Migration(8, 9) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `Coupons` (`id` INTEGER NOT NULL,
                        `siteId` INTEGER NOT NULL,
                        `code` TEXT,
                        `dateCreated` TEXT,
                        `dateCreatedGmt` TEXT,
                        `dateModified` TEXT,
                        `dateModifiedGmt` TEXT,
                        `discountType` TEXT,
                        `description` TEXT,
                        `dateExpires` TEXT,
                        `dateExpiresGmt` TEXT,
                        `usageCount` INTEGER,
                        `isForIndividualUse` INTEGER,
                        `usageLimit` INTEGER,
                        `usageLimitPerUser` INTEGER,
                        `limitUsageToXItems` INTEGER,
                        `isShippingFree` INTEGER,
                        `areSaleItemsExcluded` INTEGER,
                        `minimumAmount` TEXT,
                        `maximumAmount` TEXT,
                        PRIMARY KEY(`id`,`siteId`))
                        """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `Products` (
                    `id` INTEGER NOT NULL,
                    `siteId` INTEGER NOT NULL,
                    `name` TEXT,
                    `slug` TEXT,
                    `permalink` TEXT,
                    `dateCreated` TEXT,
                    `dateModified` TEXT,
                    `type` TEXT,
                    `status` TEXT,
                    `isFeatured` INTEGER,
                    `catalogVisibility` TEXT,
                    `description` TEXT,
                    `shortDescription` TEXT,
                    `sku` TEXT,
                    `price` TEXT,
                    `regularPrice` TEXT,
                    `salePrice` TEXT,
                    `isOnSale` INTEGER,
                    `totalSales` INTEGER,
                    `isPurchasable` INTEGER,
                    `dateOnSaleFrom` TEXT,
                    `dateOnSaleTo` TEXT,
                    `dateOnSaleFromGmt` TEXT,
                    `dateOnSaleToGmt` TEXT,
                    `isVirtual` INTEGER,
                    `isDownloadable` INTEGER,
                    `downloadLimit` INTEGER,
                    `downloadExpiry` INTEGER,
                    `isSoldIndividually` INTEGER,
                    `externalUrl` TEXT,
                    `buttonText` TEXT,
                    `taxStatus` TEXT,
                    `taxClass` TEXT,
                    `isStockManaged` INTEGER,
                    `stockQuantity` REAL,
                    `stockStatus` TEXT,
                    `backorders` TEXT,
                    `areBackordersAllowed` INTEGER,
                    `isBackordered` INTEGER,
                    `isShippingRequired` INTEGER,
                    `isShippingTaxable` INTEGER,
                    `shippingClass` TEXT,
                    `shippingClassId` INTEGER,
                    `areReviewsAllowed` INTEGER,
                    `averageRating` TEXT,
                    `ratingCount` INTEGER,
                    `parentId` INTEGER,
                    `purchaseNote` TEXT,
                    `menuOrder` INTEGER,
                    PRIMARY KEY(`id`,`siteId`))
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `ProductCategories` (
                `id` INTEGER NOT NULL,
                `siteId` INTEGER NOT NULL,
                `parentId` INTEGER,
                `name` TEXT,
                `slug` TEXT,
                PRIMARY KEY(`id`,`siteId`))
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `CouponEmails` (
                    `couponId` INTEGER NOT NULL, 
                    `siteId` INTEGER NOT NULL, 
                    `email` TEXT NOT NULL, 
                    PRIMARY KEY(`couponId`, `siteId`, `email`), 
                    FOREIGN KEY(`couponId`, `siteId`) 
                    REFERENCES `Coupons`(`id`, `siteId`) ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `CouponsAndProducts` (`couponId` INTEGER NOT NULL,
                    `siteId` INTEGER NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `isExcluded` INTEGER NOT NULL,
                    PRIMARY KEY(`couponId`,`productId`),
                    FOREIGN KEY(`couponId`,`siteId`) 
                    REFERENCES `Coupons`(`id`,`siteId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE ,
                    FOREIGN KEY(`productId`,`siteId`) 
                    REFERENCES `Products`(`id`,`siteId`) ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `CouponsAndProductCategories` (
                    `couponId` INTEGER NOT NULL,
                    `siteId` INTEGER NOT NULL,
                    `productCategoryId` INTEGER NOT NULL,
                    `isExcluded` INTEGER NOT NULL,
                    PRIMARY KEY(`couponId`, `productCategoryId`),
                    FOREIGN KEY(`couponId`, `siteId`) REFERENCES `Coupons`(`id`, `siteId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE ,
                    FOREIGN KEY(`productCategoryId`, `siteId`) 
                    REFERENCES `ProductCategories`(`id`, `siteId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_Coupons_id_siteId` ON `Coupons` (`id`, `siteId`);
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponEmails_couponId_siteId_email` 
                    ON `CouponEmails` (`couponId`, `siteId`, `email`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponsAndProducts_couponId_siteId` 
                    ON `CouponsAndProducts` (`couponId`, `siteId`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponsAndProducts_productId_siteId` 
                    ON `CouponsAndProducts` (`productId`, `siteId`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponsAndProductCategories_couponId_siteId` 
                    ON `CouponsAndProductCategories` (`couponId`, `siteId`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponsAndProductCategories_productCategoryId_siteId` 
                    ON `CouponsAndProductCategories` (`productCategoryId`, `siteId`)
                """.trimIndent()
            )
        }
    }
}

internal val MIGRATION_9_10 = object : Migration(9, 10) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE OrderEntity")
            execSQL(
                // language=RoomSql
                """ 
                        CREATE TABLE IF NOT EXISTS OrderEntity (
                        `localSiteId` INTEGER NOT NULL,
                        `orderId` INTEGER NOT NULL,
                        `number` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `orderKey` TEXT NOT NULL,
                        `dateCreated` TEXT NOT NULL,
                        `dateModified` TEXT NOT NULL,
                        `total` TEXT NOT NULL,
                        `totalTax` TEXT NOT NULL,
                        `shippingTotal` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `paymentMethodTitle` TEXT NOT NULL,
                        `datePaid` TEXT NOT NULL,
                        `pricesIncludeTax` INTEGER NOT NULL,
                        `customerNote` TEXT NOT NULL,
                        `discountTotal` TEXT NOT NULL,
                        `discountCodes` TEXT NOT NULL,
                        `refundTotal` TEXT NOT NULL,
                        `billingFirstName` TEXT NOT NULL,
                        `billingLastName` TEXT NOT NULL,
                        `billingCompany` TEXT NOT NULL,
                        `billingAddress1` TEXT NOT NULL,
                        `billingAddress2` TEXT NOT NULL,
                        `billingCity` TEXT NOT NULL,
                        `billingState` TEXT NOT NULL,
                        `billingPostcode` TEXT NOT NULL,
                        `billingCountry` TEXT NOT NULL,
                        `billingEmail` TEXT NOT NULL,
                        `billingPhone` TEXT NOT NULL,
                        `shippingFirstName` TEXT NOT NULL,
                        `shippingLastName` TEXT NOT NULL,
                        `shippingCompany` TEXT NOT NULL,
                        `shippingAddress1` TEXT NOT NULL,
                        `shippingAddress2` TEXT NOT NULL,
                        `shippingCity` TEXT NOT NULL,
                        `shippingState` TEXT NOT NULL,
                        `shippingPostcode` TEXT NOT NULL,
                        `shippingCountry` TEXT NOT NULL,
                        `shippingPhone` TEXT NOT NULL,
                        `lineItems` TEXT NOT NULL,
                        `shippingLines` TEXT NOT NULL,
                        `feeLines` TEXT NOT NULL,
                        `metaData` TEXT NOT NULL,
                        `taxLines` TEXT NOT NULL,
                         PRIMARY KEY(`localSiteId`, `orderId`)
                        )
                    """.trimIndent()
            )
            execSQL(
                // language=RoomSql
                """
                        CREATE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_orderId` 
                        ON `OrderEntity` (`localSiteId`, `orderId`);
                    """.trimIndent()
            )
        }
    }
}

internal val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE Coupons")
            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `Coupons` (`id` INTEGER NOT NULL,
                        `siteId` INTEGER NOT NULL,
                        `code` TEXT,
                        `amount` TEXT,
                        `dateCreated` TEXT,
                        `dateCreatedGmt` TEXT,
                        `dateModified` TEXT,
                        `dateModifiedGmt` TEXT,
                        `discountType` TEXT,
                        `description` TEXT,
                        `dateExpires` TEXT,
                        `dateExpiresGmt` TEXT,
                        `usageCount` INTEGER,
                        `isForIndividualUse` INTEGER,
                        `usageLimit` INTEGER,
                        `usageLimitPerUser` INTEGER,
                        `limitUsageToXItems` INTEGER,
                        `isShippingFree` INTEGER,
                        `areSaleItemsExcluded` INTEGER,
                        `minimumAmount` TEXT,
                        `maximumAmount` TEXT,
                        PRIMARY KEY(`id`,`siteId`))
                        """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_Coupons_id_siteId` ON `Coupons` (`id`, `siteId`);
                """.trimIndent()
            )
        }
    }
}

internal val MIGRATION_11_12 = object : Migration(11, 12) {
    @Suppress("MaxLineLength")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `InboxNotes` (
                    `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `remoteId` INTEGER NOT NULL,
                    `siteId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `dateCreated` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `source` TEXT,
                    `type` TEXT,
                    `dateReminder` TEXT)
                    """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE UNIQUE INDEX IF NOT EXISTS `index_InboxNotes_remoteId_siteId` ON `InboxNotes` (`remoteId`, `siteId`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `InboxNoteActions` (
                    `remoteId` INTEGER NOT NULL,
                    `inboxNoteLocalId` INTEGER NOT NULL,
                    `siteId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `url` TEXT NOT NULL,
                    `query` TEXT,
                    `status` TEXT,
                    `primary` INTEGER NOT NULL,
                    `actionedText` TEXT,
                    PRIMARY KEY(`remoteId`,`inboxNoteLocalId`),
                    FOREIGN KEY(`inboxNoteLocalId`)
                    REFERENCES `InboxNotes`(`localId`) ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
            )
        }
    }
}

@DeleteTable(tableName = "CouponsAndProducts")
@DeleteTable(tableName = "CouponsAndProductCategories")
internal class AutoMigration13to14 : AutoMigrationSpec

@DeleteTable(tableName = "Products")
@DeleteTable(tableName = "ProductCategories")
internal class AutoMigration14to15 : AutoMigrationSpec

internal val MIGRATION_15_16 = object : Migration(15, 16) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE OrderEntity")
            // language=RoomSql
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS  `OrderEntity` (
                  `localSiteId` INTEGER NOT NULL,
                  `orderId` INTEGER NOT NULL,
                  `number` TEXT NOT NULL,
                  `status` TEXT NOT NULL,
                  `currency` TEXT NOT NULL,
                  `orderKey` TEXT NOT NULL,
                  `dateCreated` TEXT NOT NULL,
                  `dateModified` TEXT NOT NULL,
                  `total` TEXT NOT NULL,
                  `totalTax` TEXT NOT NULL,
                  `shippingTotal` TEXT NOT NULL,
                  `paymentMethod` TEXT NOT NULL,
                  `paymentMethodTitle` TEXT NOT NULL,
                  `datePaid` TEXT NOT NULL,
                  `pricesIncludeTax` INTEGER NOT NULL,
                  `customerNote` TEXT NOT NULL,
                  `discountTotal` TEXT NOT NULL,
                  `discountCodes` TEXT NOT NULL,
                  `refundTotal` TEXT NOT NULL,
                  `billingFirstName` TEXT NOT NULL,
                  `billingLastName` TEXT NOT NULL,
                  `billingCompany` TEXT NOT NULL,
                  `billingAddress1` TEXT NOT NULL,
                  `billingAddress2` TEXT NOT NULL,
                  `billingCity` TEXT NOT NULL,
                  `billingState` TEXT NOT NULL,
                  `billingPostcode` TEXT NOT NULL,
                  `billingCountry` TEXT NOT NULL,
                  `billingEmail` TEXT NOT NULL,
                  `billingPhone` TEXT NOT NULL,
                  `shippingFirstName` TEXT NOT NULL,
                  `shippingLastName` TEXT NOT NULL,
                  `shippingCompany` TEXT NOT NULL,
                  `shippingAddress1` TEXT NOT NULL,
                  `shippingAddress2` TEXT NOT NULL,
                  `shippingCity` TEXT NOT NULL,
                  `shippingState` TEXT NOT NULL,
                  `shippingPostcode` TEXT NOT NULL,
                  `shippingCountry` TEXT NOT NULL,
                  `shippingPhone` TEXT NOT NULL,
                  `lineItems` TEXT NOT NULL,
                  `shippingLines` TEXT NOT NULL,
                  `feeLines` TEXT NOT NULL,
                  `taxLines` TEXT NOT NULL,
                  `metaData` TEXT NOT NULL,
                  `paymentUrl` TEXT NOT NULL DEFAULT '',
                  `isEditable` INTEGER NOT NULL DEFAULT 1,
                  PRIMARY KEY(`localSiteId`, `orderId`)
                )
            """.trimIndent()
            )
            execSQL(
                // language=RoomSql
                """
                    CREATE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_orderId` 
                    ON `OrderEntity` (`localSiteId`, `orderId`);
                """.trimIndent()
            )
        }
    }
}

internal class AutoMigration16to17 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "OrderMetaData", columnName = "displayKey"),
    DeleteColumn(tableName = "OrderMetaData", columnName = "displayValue")
)
internal class AutoMigration17to18 : AutoMigrationSpec

internal class AutoMigration18to19 : AutoMigrationSpec

internal class AutoMigration19to20 : AutoMigrationSpec

internal val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE TopPerformerProducts")
            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `TopPerformerProducts` (
                    `siteId` INTEGER NOT NULL,
                    `datePeriod` TEXT NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `imageUrl` TEXT,
                    `quantity` INTEGER NOT NULL,
                    `currency` TEXT NOT NULL,
                    `total` REAL NOT NULL,
                    `millisSinceLastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`datePeriod`,`productId`,`siteId`)
                    )
                    """.trimIndent()
            )
        }
    }
}

@Suppress("MaxLineLength")
internal val MIGRATION_21_22 = Migration(21, 22) { database ->
    // TopPerformerProducts
    database.execSQL("DROP TABLE `TopPerformerProducts`")
    database.execSQL("CREATE TABLE IF NOT EXISTS `TopPerformerProducts` (`localSiteId` INTEGER NOT NULL, `datePeriod` TEXT NOT NULL, `productId` INTEGER NOT NULL, `name` TEXT NOT NULL, `imageUrl` TEXT, `quantity` INTEGER NOT NULL, `currency` TEXT NOT NULL, `total` REAL NOT NULL, `millisSinceLastUpdated` INTEGER NOT NULL, PRIMARY KEY(`datePeriod`, `productId`, `localSiteId`))")

    // Coupons
    database.execSQL("DROP TABLE `Coupons`")
    database.execSQL("CREATE TABLE IF NOT EXISTS `Coupons` (`id` INTEGER NOT NULL, `localSiteId` INTEGER NOT NULL, `code` TEXT, `amount` TEXT, `dateCreated` TEXT, `dateCreatedGmt` TEXT, `dateModified` TEXT, `dateModifiedGmt` TEXT, `discountType` TEXT, `description` TEXT, `dateExpires` TEXT, `dateExpiresGmt` TEXT, `usageCount` INTEGER, `isForIndividualUse` INTEGER, `usageLimit` INTEGER, `usageLimitPerUser` INTEGER, `limitUsageToXItems` INTEGER, `isShippingFree` INTEGER, `areSaleItemsExcluded` INTEGER, `minimumAmount` TEXT, `maximumAmount` TEXT, `includedProductIds` TEXT, `excludedProductIds` TEXT, `includedCategoryIds` TEXT, `excludedCategoryIds` TEXT, PRIMARY KEY(`id`, `localSiteId`))")
    database.execSQL("DROP TABLE `CouponEmails`")
    database.execSQL("CREATE TABLE IF NOT EXISTS `CouponEmails` (`couponId` INTEGER NOT NULL, `localSiteId` INTEGER NOT NULL, `email` TEXT NOT NULL, PRIMARY KEY(`couponId`, `localSiteId`, `email`), FOREIGN KEY(`couponId`, `localSiteId`) REFERENCES `Coupons`(`id`, `localSiteId`) ON UPDATE NO ACTION ON DELETE CASCADE )")

    // Addons
    database.execSQL("DROP TABLE `GlobalAddonGroupEntity`")
    database.execSQL("CREATE TABLE IF NOT EXISTS `GlobalAddonGroupEntity` (`globalGroupLocalId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `restrictedCategoriesIds` TEXT NOT NULL, `localSiteId` INTEGER NOT NULL)")
    database.execSQL("DROP TABLE `AddonEntity`")
    database.execSQL("CREATE TABLE IF NOT EXISTS `AddonEntity` (`addonLocalId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `globalGroupLocalId` INTEGER, `productRemoteId` INTEGER, `localSiteId` INTEGER, `type` TEXT NOT NULL, `display` TEXT, `name` TEXT NOT NULL, `titleFormat` TEXT NOT NULL, `description` TEXT, `required` INTEGER NOT NULL, `position` INTEGER NOT NULL, `restrictions` TEXT, `priceType` TEXT, `price` TEXT, `min` INTEGER, `max` INTEGER, FOREIGN KEY(`globalGroupLocalId`) REFERENCES `GlobalAddonGroupEntity`(`globalGroupLocalId`) ON UPDATE NO ACTION ON DELETE CASCADE )")

    // Inbox
    database.execSQL("DROP TABLE `InboxNotes`")
    database.execSQL("CREATE TABLE IF NOT EXISTS `InboxNotes` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER NOT NULL, `localSiteId` INTEGER NOT NULL, `name` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `dateCreated` TEXT NOT NULL, `status` TEXT NOT NULL, `source` TEXT, `type` TEXT, `dateReminder` TEXT)")
    database.execSQL("DROP TABLE `InboxNoteActions`")
    database.execSQL("CREATE TABLE IF NOT EXISTS `InboxNoteActions` (`remoteId` INTEGER NOT NULL, `inboxNoteLocalId` INTEGER NOT NULL, `localSiteId` INTEGER NOT NULL, `name` TEXT NOT NULL, `label` TEXT NOT NULL, `url` TEXT NOT NULL, `query` TEXT, `status` TEXT, `primary` INTEGER NOT NULL, `actionedText` TEXT, PRIMARY KEY(`remoteId`, `inboxNoteLocalId`), FOREIGN KEY(`inboxNoteLocalId`) REFERENCES `InboxNotes`(`localId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_InboxNotes_remoteId_localSiteId` ON `InboxNotes` (`remoteId`, `localSiteId`)")

    // Order Notes
    database.execSQL("DROP TABLE `OrderNotes`")
    database.execSQL("CREATE TABLE IF NOT EXISTS `OrderNotes` (`localSiteId` INTEGER NOT NULL, `noteId` INTEGER NOT NULL, `orderId` INTEGER NOT NULL, `dateCreated` TEXT, `note` TEXT, `author` TEXT, `isSystemNote` INTEGER NOT NULL, `isCustomerNote` INTEGER NOT NULL, PRIMARY KEY(`localSiteId`, `noteId`))")

    // Foreign Key missing indices
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_AddonOptionEntity_addonLocalId` ON `AddonOptionEntity` (`addonLocalId`)")
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_AddonEntity_globalGroupLocalId` ON `AddonEntity` (`globalGroupLocalId`)")
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_InboxNoteActions_inboxNoteLocalId` ON `InboxNoteActions` (`inboxNoteLocalId`)")
}
internal class AutoMigration23to24 : AutoMigrationSpec

/**
 * We are storing "receipt_url" into the order metadata. The purpose of this migration
 * is to recreate all of the orders freshly from the API so that the "receipt_url" will be stored
 * in every orders metadata and not just on the newly created ones.
 *
 * We need the "receipt_url" metadata to identify whether the order is an IPP order or not. We
 * use "receipt_url" along with the "paymentMethod" to identify the IPP order.
 */
internal val MIGRATION_22_23 = object : Migration(22, 23) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE OrderEntity")
            // language=RoomSql
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS  `OrderEntity` (
                  `localSiteId` INTEGER NOT NULL,
                  `orderId` INTEGER NOT NULL,
                  `number` TEXT NOT NULL,
                  `status` TEXT NOT NULL,
                  `currency` TEXT NOT NULL,
                  `orderKey` TEXT NOT NULL,
                  `dateCreated` TEXT NOT NULL,
                  `dateModified` TEXT NOT NULL,
                  `total` TEXT NOT NULL,
                  `totalTax` TEXT NOT NULL,
                  `shippingTotal` TEXT NOT NULL,
                  `paymentMethod` TEXT NOT NULL,
                  `paymentMethodTitle` TEXT NOT NULL,
                  `datePaid` TEXT NOT NULL,
                  `pricesIncludeTax` INTEGER NOT NULL,
                  `customerNote` TEXT NOT NULL,
                  `discountTotal` TEXT NOT NULL,
                  `discountCodes` TEXT NOT NULL,
                  `refundTotal` TEXT NOT NULL,
                  `billingFirstName` TEXT NOT NULL,
                  `billingLastName` TEXT NOT NULL,
                  `billingCompany` TEXT NOT NULL,
                  `billingAddress1` TEXT NOT NULL,
                  `billingAddress2` TEXT NOT NULL,
                  `billingCity` TEXT NOT NULL,
                  `billingState` TEXT NOT NULL,
                  `billingPostcode` TEXT NOT NULL,
                  `billingCountry` TEXT NOT NULL,
                  `billingEmail` TEXT NOT NULL,
                  `billingPhone` TEXT NOT NULL,
                  `shippingFirstName` TEXT NOT NULL,
                  `shippingLastName` TEXT NOT NULL,
                  `shippingCompany` TEXT NOT NULL,
                  `shippingAddress1` TEXT NOT NULL,
                  `shippingAddress2` TEXT NOT NULL,
                  `shippingCity` TEXT NOT NULL,
                  `shippingState` TEXT NOT NULL,
                  `shippingPostcode` TEXT NOT NULL,
                  `shippingCountry` TEXT NOT NULL,
                  `shippingPhone` TEXT NOT NULL,
                  `lineItems` TEXT NOT NULL,
                  `shippingLines` TEXT NOT NULL,
                  `feeLines` TEXT NOT NULL,
                  `taxLines` TEXT NOT NULL,
                  `metaData` TEXT NOT NULL,
                  `paymentUrl` TEXT NOT NULL DEFAULT '',
                  `isEditable` INTEGER NOT NULL DEFAULT 1,
                  PRIMARY KEY(`localSiteId`, `orderId`)
                )
            """.trimIndent()
            )
            execSQL(
                // language=RoomSql
                """
                    CREATE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_orderId` 
                    ON `OrderEntity` (`localSiteId`, `orderId`);
                """.trimIndent()
            )
        }
    }
}

/**
 * We are storing "coupon_lines" array into OrderEntity property.
 * The purpose is to allow adding and removing coupon lines creating and editing
 * orders.
 */
internal val MIGRATION_24_25 = object : Migration(24, 25) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE OrderEntity")
            // language=RoomSql
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS  `OrderEntity` (
                  `localSiteId` INTEGER NOT NULL,
                  `orderId` INTEGER NOT NULL,
                  `number` TEXT NOT NULL,
                  `status` TEXT NOT NULL,
                  `currency` TEXT NOT NULL,
                  `orderKey` TEXT NOT NULL,
                  `dateCreated` TEXT NOT NULL,
                  `dateModified` TEXT NOT NULL,
                  `total` TEXT NOT NULL,
                  `totalTax` TEXT NOT NULL,
                  `shippingTotal` TEXT NOT NULL,
                  `paymentMethod` TEXT NOT NULL,
                  `paymentMethodTitle` TEXT NOT NULL,
                  `datePaid` TEXT NOT NULL,
                  `pricesIncludeTax` INTEGER NOT NULL,
                  `customerNote` TEXT NOT NULL,
                  `discountTotal` TEXT NOT NULL,
                  `discountCodes` TEXT NOT NULL,
                  `refundTotal` TEXT NOT NULL,
                  `billingFirstName` TEXT NOT NULL,
                  `billingLastName` TEXT NOT NULL,
                  `billingCompany` TEXT NOT NULL,
                  `billingAddress1` TEXT NOT NULL,
                  `billingAddress2` TEXT NOT NULL,
                  `billingCity` TEXT NOT NULL,
                  `billingState` TEXT NOT NULL,
                  `billingPostcode` TEXT NOT NULL,
                  `billingCountry` TEXT NOT NULL,
                  `billingEmail` TEXT NOT NULL,
                  `billingPhone` TEXT NOT NULL,
                  `shippingFirstName` TEXT NOT NULL,
                  `shippingLastName` TEXT NOT NULL,
                  `shippingCompany` TEXT NOT NULL,
                  `shippingAddress1` TEXT NOT NULL,
                  `shippingAddress2` TEXT NOT NULL,
                  `shippingCity` TEXT NOT NULL,
                  `shippingState` TEXT NOT NULL,
                  `shippingPostcode` TEXT NOT NULL,
                  `shippingCountry` TEXT NOT NULL,
                  `shippingPhone` TEXT NOT NULL,
                  `lineItems` TEXT NOT NULL,
                  `shippingLines` TEXT NOT NULL,
                  `feeLines` TEXT NOT NULL,
                  `taxLines` TEXT NOT NULL,
                  `couponLines` TEXT NOT NULL DEFAULT '',
                  `metaData` TEXT NOT NULL,
                  `paymentUrl` TEXT NOT NULL DEFAULT '',
                  `isEditable` INTEGER NOT NULL DEFAULT 1,
                  PRIMARY KEY(`localSiteId`, `orderId`)
                )
            """.trimIndent()
            )
            execSQL(
                // language=RoomSql
                """
                    CREATE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_orderId`
                    ON `OrderEntity` (`localSiteId`, `orderId`);
                """.trimIndent()
            )
        }
    }
}

/**
 * We are storing "needs_payment" and "needs_processing" into OrderEntity property.
 * This information will make rendering the UI easier.
 */
internal val MIGRATION_27_28 = object : Migration(27, 28) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE OrderEntity")
            // language=RoomSql
            execSQL(
                /* sql = */ """
                CREATE TABLE IF NOT EXISTS  `OrderEntity` (
                  `localSiteId` INTEGER NOT NULL,
                  `orderId` INTEGER NOT NULL,
                  `number` TEXT NOT NULL,
                  `status` TEXT NOT NULL,
                  `currency` TEXT NOT NULL,
                  `orderKey` TEXT NOT NULL,
                  `dateCreated` TEXT NOT NULL,
                  `dateModified` TEXT NOT NULL,
                  `total` TEXT NOT NULL,
                  `totalTax` TEXT NOT NULL,
                  `shippingTotal` TEXT NOT NULL,
                  `paymentMethod` TEXT NOT NULL,
                  `paymentMethodTitle` TEXT NOT NULL,
                  `datePaid` TEXT NOT NULL,
                  `pricesIncludeTax` INTEGER NOT NULL,
                  `customerNote` TEXT NOT NULL,
                  `discountTotal` TEXT NOT NULL,
                  `discountCodes` TEXT NOT NULL,
                  `refundTotal` TEXT NOT NULL,
                  `billingFirstName` TEXT NOT NULL,
                  `billingLastName` TEXT NOT NULL,
                  `billingCompany` TEXT NOT NULL,
                  `billingAddress1` TEXT NOT NULL,
                  `billingAddress2` TEXT NOT NULL,
                  `billingCity` TEXT NOT NULL,
                  `billingState` TEXT NOT NULL,
                  `billingPostcode` TEXT NOT NULL,
                  `billingCountry` TEXT NOT NULL,
                  `billingEmail` TEXT NOT NULL,
                  `billingPhone` TEXT NOT NULL,
                  `shippingFirstName` TEXT NOT NULL,
                  `shippingLastName` TEXT NOT NULL,
                  `shippingCompany` TEXT NOT NULL,
                  `shippingAddress1` TEXT NOT NULL,
                  `shippingAddress2` TEXT NOT NULL,
                  `shippingCity` TEXT NOT NULL,
                  `shippingState` TEXT NOT NULL,
                  `shippingPostcode` TEXT NOT NULL,
                  `shippingCountry` TEXT NOT NULL,
                  `shippingPhone` TEXT NOT NULL,
                  `lineItems` TEXT NOT NULL,
                  `shippingLines` TEXT NOT NULL,
                  `feeLines` TEXT NOT NULL,
                  `taxLines` TEXT NOT NULL,
                  `couponLines` TEXT NOT NULL DEFAULT '',
                  `metaData` TEXT NOT NULL,
                  `paymentUrl` TEXT NOT NULL DEFAULT '',
                  `isEditable` INTEGER NOT NULL DEFAULT 1,
                  `needsPayment` INTEGER,
                  `needsProcessing` INTEGER,
                  PRIMARY KEY(`localSiteId`, `orderId`)
                )
            """.trimIndent()
            )
            execSQL(
                // language=RoomSql
                """
                    CREATE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_orderId`
                    ON `OrderEntity` (`localSiteId`, `orderId`);
                """.trimIndent()
            )
        }
    }
}

/**
 * OrderEntity contains a new `customerId` field tagged with `ColumnInfo` annotation
 * requiring a proper migration to it.
 */
internal val MIGRATION_30_31 = object : Migration(30, 31) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE OrderEntity")
            // language=RoomSql
            execSQL(
                /* sql = */ """
                CREATE TABLE IF NOT EXISTS  `OrderEntity` (
                  `localSiteId` INTEGER NOT NULL,
                  `orderId` INTEGER NOT NULL,
                  `number` TEXT NOT NULL,
                  `status` TEXT NOT NULL,
                  `currency` TEXT NOT NULL,
                  `orderKey` TEXT NOT NULL,
                  `dateCreated` TEXT NOT NULL,
                  `dateModified` TEXT NOT NULL,
                  `total` TEXT NOT NULL,
                  `totalTax` TEXT NOT NULL,
                  `shippingTotal` TEXT NOT NULL,
                  `paymentMethod` TEXT NOT NULL,
                  `paymentMethodTitle` TEXT NOT NULL,
                  `datePaid` TEXT NOT NULL,
                  `pricesIncludeTax` INTEGER NOT NULL,
                  `customerNote` TEXT NOT NULL,
                  `discountTotal` TEXT NOT NULL,
                  `discountCodes` TEXT NOT NULL,
                  `refundTotal` TEXT NOT NULL,
                  `billingFirstName` TEXT NOT NULL,
                  `billingLastName` TEXT NOT NULL,
                  `billingCompany` TEXT NOT NULL,
                  `billingAddress1` TEXT NOT NULL,
                  `billingAddress2` TEXT NOT NULL,
                  `billingCity` TEXT NOT NULL,
                  `billingState` TEXT NOT NULL,
                  `billingPostcode` TEXT NOT NULL,
                  `billingCountry` TEXT NOT NULL,
                  `billingEmail` TEXT NOT NULL,
                  `billingPhone` TEXT NOT NULL,
                  `shippingFirstName` TEXT NOT NULL,
                  `shippingLastName` TEXT NOT NULL,
                  `shippingCompany` TEXT NOT NULL,
                  `shippingAddress1` TEXT NOT NULL,
                  `shippingAddress2` TEXT NOT NULL,
                  `shippingCity` TEXT NOT NULL,
                  `shippingState` TEXT NOT NULL,
                  `shippingPostcode` TEXT NOT NULL,
                  `shippingCountry` TEXT NOT NULL,
                  `shippingPhone` TEXT NOT NULL,
                  `lineItems` TEXT NOT NULL,
                  `shippingLines` TEXT NOT NULL,
                  `feeLines` TEXT NOT NULL,
                  `taxLines` TEXT NOT NULL,
                  `couponLines` TEXT NOT NULL DEFAULT '',
                  `metaData` TEXT NOT NULL,
                  `paymentUrl` TEXT NOT NULL DEFAULT '',
                  `isEditable` INTEGER NOT NULL DEFAULT 1,
                  `needsPayment` INTEGER,
                  `needsProcessing` INTEGER,
                  `customerId` INTEGER NOT NULL DEFAULT 0,
                  PRIMARY KEY(`localSiteId`, `orderId`)
                )
            """.trimIndent()
            )
            execSQL(
                // language=RoomSql
                """
                    CREATE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_orderId`
                    ON `OrderEntity` (`localSiteId`, `orderId`);
                """.trimIndent()
            )
        }
    }
}

/**
 * We are storing "giftCards" into OrderEntity property as a new extensions support.
 */
internal val MIGRATION_31_32 = object : Migration(31, 32) {
    @Suppress("LongMethod")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("DROP TABLE OrderEntity")
            // language=RoomSql
            execSQL(
                /* sql = */ """
                CREATE TABLE IF NOT EXISTS  `OrderEntity` (
                  `localSiteId` INTEGER NOT NULL,
                  `orderId` INTEGER NOT NULL,
                  `number` TEXT NOT NULL,
                  `status` TEXT NOT NULL,
                  `currency` TEXT NOT NULL,
                  `orderKey` TEXT NOT NULL,
                  `dateCreated` TEXT NOT NULL,
                  `dateModified` TEXT NOT NULL,
                  `total` TEXT NOT NULL,
                  `totalTax` TEXT NOT NULL,
                  `shippingTotal` TEXT NOT NULL,
                  `paymentMethod` TEXT NOT NULL,
                  `paymentMethodTitle` TEXT NOT NULL,
                  `datePaid` TEXT NOT NULL,
                  `pricesIncludeTax` INTEGER NOT NULL,
                  `customerNote` TEXT NOT NULL,
                  `discountTotal` TEXT NOT NULL,
                  `discountCodes` TEXT NOT NULL,
                  `refundTotal` TEXT NOT NULL,
                  `billingFirstName` TEXT NOT NULL,
                  `billingLastName` TEXT NOT NULL,
                  `billingCompany` TEXT NOT NULL,
                  `billingAddress1` TEXT NOT NULL,
                  `billingAddress2` TEXT NOT NULL,
                  `billingCity` TEXT NOT NULL,
                  `billingState` TEXT NOT NULL,
                  `billingPostcode` TEXT NOT NULL,
                  `billingCountry` TEXT NOT NULL,
                  `billingEmail` TEXT NOT NULL,
                  `billingPhone` TEXT NOT NULL,
                  `shippingFirstName` TEXT NOT NULL,
                  `shippingLastName` TEXT NOT NULL,
                  `shippingCompany` TEXT NOT NULL,
                  `shippingAddress1` TEXT NOT NULL,
                  `shippingAddress2` TEXT NOT NULL,
                  `shippingCity` TEXT NOT NULL,
                  `shippingState` TEXT NOT NULL,
                  `shippingPostcode` TEXT NOT NULL,
                  `shippingCountry` TEXT NOT NULL,
                  `shippingPhone` TEXT NOT NULL,
                  `lineItems` TEXT NOT NULL,
                  `shippingLines` TEXT NOT NULL,
                  `feeLines` TEXT NOT NULL,
                  `taxLines` TEXT NOT NULL,
                  `couponLines` TEXT NOT NULL DEFAULT '',
                  `metaData` TEXT NOT NULL,
                  `paymentUrl` TEXT NOT NULL DEFAULT '',
                  `isEditable` INTEGER NOT NULL DEFAULT 1,
                  `needsPayment` INTEGER,
                  `needsProcessing` INTEGER,
                  `customerId` INTEGER NOT NULL DEFAULT 0,
                  `giftCardCode` TEXT NOT NULL DEFAULT '',
                  `giftCardAmount` TEXT NOT NULL DEFAULT '',
                  PRIMARY KEY(`localSiteId`, `orderId`)
                )
            """.trimIndent()
            )
            execSQL(
                // language=RoomSql
                """
                    CREATE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_orderId`
                    ON `OrderEntity` (`localSiteId`, `orderId`);
                """.trimIndent()
            )
        }
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "WooPaymentsBalance", columnName = "transactionIds"),
    DeleteColumn(tableName = "WooPaymentsBalance", columnName = "depositsCount"),
)
internal class AutoMigration32to33 : AutoMigrationSpec

@RenameColumn.Entries(
    RenameColumn(tableName = "OrderMetaData", fromColumnName = "orderId", toColumnName = "parentItemId"),
)
@DeleteColumn.Entries(
    DeleteColumn(tableName = "OrderMetaData", columnName = "isDisplayable")
)
@RenameTable.Entries(
    RenameTable(fromTableName = "OrderMetaData", toTableName = "MetaData")
)
internal class AutoMigration37to38 : AutoMigrationSpec
