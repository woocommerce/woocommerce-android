package org.wordpress.android.fluxc.persistence

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_6_7
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_71_72
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_79_80
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_7_8
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_8_9
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_9_10

@RunWith(AndroidJUnit4::class)
class MigrationTests {
    @Rule
    @JvmField
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WCAndroidDatabase::class.java,
        emptyList()
    )

    @Test
    fun testMigrate3To4() {
        helper.apply {
            createDatabase(TEST_DB, 3).close()
            runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
        }
    }

    @Test
    fun testMigrate4to5() {
        helper.apply {
            createDatabase(TEST_DB, 4).close()
            runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)
        }
    }

    @Test
    fun testMigrate5to6() {
        helper.apply {
            createDatabase(TEST_DB, 5).close()
            runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)
        }
    }

    @Test
    fun testMigrate6to7() {
        helper.apply {
            createDatabase(TEST_DB, 6).apply {
                execSQL(
                    // language=RoomSql
                    """
                            INSERT INTO OrderEntity VALUES(1, 2, 3, '123', 'processing', '$', 'key', 'date of creation', 'date of modification', '123', '456', '789', 'card', 'by card', 'date paid', 1, 'sample customer note', '213', 'CODE', 123, 'billing first name', 'billing last name', 'billing company', 'billing address1', 'billing address2', 'billing city', 'billing state', 'billing postcode', 'billing country', 'billing email', 'billing phone', 'shipping first name', 'shipping last name', 'shipping company', 'shipping address1', 'shipping address2', 'shipping city', 'shipping state', 'shipping postcode', 'shipping country', 'shipping phone', 'line items', 'shipping lines', 'fee lines', 'meta data')
                        """.trimIndent()
                )
            }.close()

            val migratedDb = runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

            migratedDb.query(
                // language=RoomSql
                """
                        SELECT * FROM OrderEntity
                    """.trimIndent()
            )
        }
    }

    @Test
    fun testMigration7to8() {
        helper.apply {
            createDatabase(TEST_DB, 7).close()
            runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)
        }
    }

    @Test
    fun testMigration8to9() {
        helper.apply {
            createDatabase(TEST_DB, 8).close()
            runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)
        }
    }

    @Test
    fun testMigrate9to10() {
        helper.apply {
            createDatabase(TEST_DB, 9).close()
            runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)
        }
    }

    @Test
    fun testMigrate10to11() {
        helper.apply {
            createDatabase(TEST_DB, 10).close()
            runMigrationsAndValidate(TEST_DB, 11, true, MIGRATION_10_11)
        }
    }

    @Test
    fun testMigrate11to12() {
        helper.apply {
            createDatabase(TEST_DB, 11).close()
            runMigrationsAndValidate(TEST_DB, 12, true, MIGRATION_11_12)
        }
    }

    @Test
    fun testMigrate12to13() {
        helper.apply {
            createDatabase(TEST_DB, 12).close()
            runMigrationsAndValidate(TEST_DB, 13, true)
        }
    }

    @Test
    fun testMigrate15to16() {
        helper.apply {
            createDatabase(TEST_DB, 15).apply {
                execSQL(
                    // language=RoomSql
                    """
                    INSERT INTO OrderEntity VALUES(1, 2, 3, '123', 'processing', '$', 'key', 'date of creation', 'date of modification', '123', '456', '789', 'card', 'by card', 'date paid', 1, 'sample customer note', '213', 'CODE', 123, 'billing first name', 'billing last name', 'billing company', 'billing address1', 'billing address2', 'billing city', 'billing state', 'billing postcode', 'billing country', 'billing email', 'billing phone', 'shipping first name', 'shipping last name', 'shipping company', 'shipping address1', 'shipping address2', 'shipping city', 'shipping state', 'shipping postcode', 'shipping country', 'shipping phone', 'line items', 'shipping lines', 'fee lines', 'meta data', 'payment url')
                    """.trimIndent()
                )
            }.close()

            val migratedDb = runMigrationsAndValidate(TEST_DB, 16, true, MIGRATION_15_16)
            val cursor = migratedDb.query(
                // language=RoomSql
                """
                        SELECT * FROM OrderEntity
                    """.trimIndent()
            )
            // Ensure we delete all saved OrderEntities and use the API as the source of true
            assertThat(cursor.count).isEqualTo(0)
            cursor.close()
        }
    }

    @Test
    fun testMigrate20to21() {
        helper.apply {
            createDatabase(TEST_DB, 20).apply {
                execSQL(
                    // language=RoomSql
                    """
                    INSERT INTO TopPerformerProducts VALUES(
                        202934350,
                        "2022-10-01T00:00:00-2022-10-31T23:59:59",
                        78,
                        "WooCommerce Tote Bag",
                        "https://samplesite.com/awesomeproduct.jpg",
                        2,
                        "/$",
                        11.0,
                        1666727639491
                    )
                    """.trimIndent()
                )
            }.close()

            val migratedDb = runMigrationsAndValidate(TEST_DB, 21, true, MIGRATION_20_21)
            val cursor = migratedDb.query(
                // language=RoomSql
                """
                        SELECT * FROM TopPerformerProducts
                    """.trimIndent()
            )
            // Ensure we delete all saved TopPerformerProducts
            assertThat(cursor.count).isEqualTo(0)
            cursor.close()
        }
    }

    @Test
    fun testMigrate21to22() {
        helper.apply {
            createDatabase(TEST_DB, 21).close()
            runMigrationsAndValidate(TEST_DB, 22, true, MIGRATION_21_22)
        }
    }

    @Test
    fun testMigrate22to23() {
        helper.apply {
            createDatabase(TEST_DB, 22).close()
            runMigrationsAndValidate(TEST_DB, 23, true, MIGRATION_22_23)
        }
    }

    @Test
    fun testMigrate23to24() {
        helper.apply {
            createDatabase(TEST_DB, 23).close()
            runMigrationsAndValidate(TEST_DB, 24, false)
        }
    }

    @Test
    fun testMigrate24to25() {
        helper.apply {
            createDatabase(TEST_DB, 24).close()
            runMigrationsAndValidate(TEST_DB, 25, true, MIGRATION_24_25)
        }
    }

    @Test
    fun testMigrate25to26() {
        helper.apply {
            createDatabase(TEST_DB, 25).close()
            runMigrationsAndValidate(TEST_DB, 26, false)
        }
    }

    @Test
    fun testMigrate26to27() {
        helper.apply {
            createDatabase(TEST_DB, 26).close()
            runMigrationsAndValidate(TEST_DB, 27, false)
        }
    }

    @Test
    fun testMigrate27to28() {
        helper.apply {
            createDatabase(TEST_DB, 27).close()
            runMigrationsAndValidate(TEST_DB, 28, true, MIGRATION_27_28)
        }
    }

    @Test
    fun testMigration28to29() {
        helper.apply {
            createDatabase(TEST_DB, 28).close()
            runMigrationsAndValidate(TEST_DB, 29, false)
        }
    }

    @Test
    fun testMigration29to30() {
        helper.apply {
            createDatabase(TEST_DB, 29).close()
            runMigrationsAndValidate(TEST_DB, 30, false)
        }
    }

    @Test
    fun testMigration30to31() {
        helper.apply {
            createDatabase(TEST_DB, 30).close()
            runMigrationsAndValidate(TEST_DB, 31, false, MIGRATION_30_31)
        }
    }

    @Test
    fun testMigration31to32() {
        helper.apply {
            createDatabase(TEST_DB, 31).close()
            runMigrationsAndValidate(TEST_DB, 32, false, MIGRATION_31_32)
        }
    }

    @Test
    fun testMigration32to33() {
        helper.apply {
            createDatabase(TEST_DB, 32).close()
            runMigrationsAndValidate(TEST_DB, 33, false)
        }
    }

    @Test
    fun testMigration33to34() {
        helper.apply {
            createDatabase(TEST_DB, 33).close()
            runMigrationsAndValidate(TEST_DB, 34, false)
        }
    }

    @Test
    fun testMigration34to35() {
        helper.apply {
            createDatabase(TEST_DB, 34).close()
            runMigrationsAndValidate(TEST_DB, 35, false)
        }
    }

    @Test
    fun testMigration35to36() {
        helper.apply {
            createDatabase(TEST_DB, 35).close()
            runMigrationsAndValidate(TEST_DB, 36, false)
        }
    }

    @Test
    fun testMigration36to37() {
        helper.apply {
            createDatabase(TEST_DB, 35).close()
            runMigrationsAndValidate(TEST_DB, 36, false)
        }
    }

    @Suppress("LongMethod")
    @Test
    fun testMigration71to72_deduplicates_and_builds_stableId() {
        // 1) Create a v71 database and seed legacy rows, including duplicates
        helper.createDatabase(TEST_DB, 71).apply {
            // Insert two duplicate registered customers (same siteId + remoteCustomerId), differing only by id and username
            execSQL(
                """
                INSERT INTO CustomerEntity (
                  id, localSiteId, remoteCustomerId, avatarUrl, dateCreated, dateCreatedGmt, dateModified, dateModifiedGmt,
                  email, firstName, isPayingCustomer, lastName, role, username,
                  billingAddress1, billingAddress2, billingCity, billingCompany, billingCountry, billingEmail, billingFirstName,
                  billingLastName, billingPhone, billingPostcode, billingState,
                  shippingAddress1, shippingAddress2, shippingCity, shippingCompany, shippingCountry, shippingFirstName,
                  shippingLastName, shippingPostcode, shippingState, analyticsCustomerId
                ) VALUES (
                  100, 10, 123, '', '', '', '', '',
                  'a@example.com', 'A', 0, 'B', '', 'user_low',
                  '', '', '', '', '', '', '',
                  '', '', '', '',
                  '', '', '', '', '', '',
                  '', '', '', NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO CustomerEntity (
                  id, localSiteId, remoteCustomerId, avatarUrl, dateCreated, dateCreatedGmt, dateModified, dateModifiedGmt,
                  email, firstName, isPayingCustomer, lastName, role, username,
                  billingAddress1, billingAddress2, billingCity, billingCompany, billingCountry, billingEmail, billingFirstName,
                  billingLastName, billingPhone, billingPostcode, billingState,
                  shippingAddress1, shippingAddress2, shippingCity, shippingCompany, shippingCountry, shippingFirstName,
                  shippingLastName, shippingPostcode, shippingState, analyticsCustomerId
                ) VALUES (
                  200, 10, 123, '', '', '', '', '',
                  'a2@example.com', 'A2', 1, 'B2', '', 'user_high',
                  '', '', '', '', '', '', '',
                  '', '', '', '',
                  '', '', '', '', '', '',
                  '', '', '', NULL
                )
                """.trimIndent()
            )

            // Insert one analytics/guest row (remoteCustomerId = 0, analyticsCustomerId > 0)
            execSQL(
                """
                INSERT INTO CustomerEntity (
                  id, localSiteId, remoteCustomerId, avatarUrl, dateCreated, dateCreatedGmt, dateModified, dateModifiedGmt,
                  email, firstName, isPayingCustomer, lastName, role, username,
                  billingAddress1, billingAddress2, billingCity, billingCompany, billingCountry, billingEmail, billingFirstName,
                  billingLastName, billingPhone, billingPostcode, billingState,
                  shippingAddress1, shippingAddress2, shippingCity, shippingCompany, shippingCountry, shippingFirstName,
                  shippingLastName, shippingPostcode, shippingState, analyticsCustomerId
                ) VALUES (
                  300, 10, 0, '', '', '', '', '',
                  'guest@example.com', 'G', 0, 'U', '', 'guest',
                  '', '', '', '', '', '', '',
                  '', '', '', '',
                  '', '', '', '', '', '',
                  '', '', '', 555
                )
                """.trimIndent()
            )

            // Insert invalid row (remoteCustomerId = 0, analyticsCustomerId = NULL)
            execSQL(
                """
                INSERT INTO CustomerEntity (
                  id, localSiteId, remoteCustomerId, avatarUrl, dateCreated, dateCreatedGmt, dateModified, dateModifiedGmt,
                  email, firstName, isPayingCustomer, lastName, role, username,
                  billingAddress1, billingAddress2, billingCity, billingCompany, billingCountry, billingEmail, billingFirstName,
                  billingLastName, billingPhone, billingPostcode, billingState,
                  shippingAddress1, shippingAddress2, shippingCity, shippingCompany, shippingCountry, shippingFirstName,
                  shippingLastName, shippingPostcode, shippingState, analyticsCustomerId
                ) VALUES (
                  400, 10, 0, '', '', '', '', '',
                  'a2@example.com', 'A2', 1, 'B2', '', 'user_high',
                  '', '', '', '', '', '', '',
                  '', '', '', '',
                  '', '', '', '', '', '',
                  '', '', '', NULL
                )
                """.trimIndent()
            )
        }.close()

        // 2) Run the migration and validate
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 72, true, MIGRATION_71_72)

        // 3) Query results and assert deduplication and stableId correctness
        migratedDb.query(
            """
            SELECT stableId, localSiteId, remoteCustomerId, analyticsCustomerId, username
            FROM CustomerEntity
            ORDER BY stableId
            """.trimIndent()
        ).use { cursor ->
            assertThat(cursor.count).isEqualTo(2)

            // Move to first row (analytics guest): stableId should be site:10|analytics:555
            cursor.moveToFirst()
            val stableId1 = cursor.getString(0)
            val siteId1 = cursor.getInt(1)
            val remoteId1 = cursor.getLong(2)
            val analyticsId1 = if (!cursor.isNull(3)) cursor.getLong(3) else null
            val username1 = cursor.getString(4)
            assertThat(stableId1).isEqualTo("analytics:555")
            assertThat(siteId1).isEqualTo(10)
            assertThat(remoteId1).isEqualTo(0)
            assertThat(analyticsId1).isEqualTo(555)
            assertThat(username1).isEqualTo("guest")

            // Move to second row (registered user deduped): stableId should be site:10|wp:123 and username from higher id row
            cursor.moveToNext()
            val stableId2 = cursor.getString(0)
            val siteId2 = cursor.getInt(1)
            val remoteId2 = cursor.getLong(2)
            val analyticsId2 = if (!cursor.isNull(3)) cursor.getLong(3) else null
            val username2 = cursor.getString(4)
            assertThat(stableId2).isEqualTo("wp:123")
            assertThat(siteId2).isEqualTo(10)
            assertThat(remoteId2).isEqualTo(123)
            assertThat(analyticsId2).isNull()
            // We kept the row with MAX(id)=200, so its username should be 'user_high'
            assertThat(username2).isEqualTo("user_high")

            // Verify the invalid row was filtered out
            assertThat(cursor.moveToNext()).isFalse
        }
    }

    @Test
    fun testMigration79to80_addsUserIdColumn_and_backfillsFromCustomerId() {
        helper.createDatabase(TEST_DB, 79).apply {
            execSQL(
                """
                INSERT INTO Bookings (
                    id, localSiteId, start, end, allDay, status, cost, currency,
                    customerId, productId, resourceId, dateCreated, dateModified,
                    googleCalendarEventId, orderId, orderItemId, parentId,
                    personCounts, localTimezone, customerNote, attendanceStatus,
                    note, location,
                    order_status, order_product_name,
                    order_customer_billingFirstName, order_customer_billingLastName,
                    order_customer_billingCompany, order_customer_billingAddress1,
                    order_customer_billingAddress2, order_customer_billingCity,
                    order_customer_billingState, order_customer_billingPostcode,
                    order_customer_billingCountry, order_customer_billingEmail,
                    order_customer_billingPhone,
                    order_payment_paymentMethodId, order_payment_paymentMethodTitle,
                    order_payment_subtotal, order_payment_subtotalTax,
                    order_payment_total, order_payment_totalTax
                ) VALUES (
                    1, 10, 1000, 2000, 0, 'confirmed', '50.0', 'USD',
                    42, 100, 200, 1000, 2000,
                    '', 300, 400, 0,
                    '', '', '', '',
                    '', '',
                    '', '',
                    '', '',
                    '', '',
                    '', '',
                    '', '',
                    '', '',
                    '',
                    '', '',
                    '', '',
                    '', ''
                )
                """.trimIndent()
            )
        }.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 80, true, MIGRATION_79_80)

        migratedDb.query("SELECT userId, customerId FROM Bookings WHERE id = 1").use { cursor ->
            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            val userId = cursor.getLong(0)
            val customerId = cursor.getLong(1)
            assertThat(userId).isEqualTo(42)
            assertThat(customerId).isEqualTo(42)
        }
    }

    @Test
    fun testMigration79to80_guestBooking_userIdDefaultsToZero() {
        helper.createDatabase(TEST_DB, 79).apply {
            execSQL(
                """
                INSERT INTO Bookings (
                    id, localSiteId, start, end, allDay, status, cost, currency,
                    customerId, productId, resourceId, dateCreated, dateModified,
                    googleCalendarEventId, orderId, orderItemId, parentId,
                    personCounts, localTimezone, customerNote, attendanceStatus,
                    note, location,
                    order_status, order_product_name,
                    order_customer_billingFirstName, order_customer_billingLastName,
                    order_customer_billingCompany, order_customer_billingAddress1,
                    order_customer_billingAddress2, order_customer_billingCity,
                    order_customer_billingState, order_customer_billingPostcode,
                    order_customer_billingCountry, order_customer_billingEmail,
                    order_customer_billingPhone,
                    order_payment_paymentMethodId, order_payment_paymentMethodTitle,
                    order_payment_subtotal, order_payment_subtotalTax,
                    order_payment_total, order_payment_totalTax
                ) VALUES (
                    2, 10, 1000, 2000, 0, 'confirmed', '50.0', 'USD',
                    0, 100, 200, 1000, 2000,
                    '', 300, 400, 0,
                    '', '', '', '',
                    '', '',
                    '', '',
                    '', '',
                    '', '',
                    '', '',
                    '', '',
                    '', '',
                    '',
                    '', '',
                    '', '',
                    '', ''
                )
                """.trimIndent()
            )
        }.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 80, true, MIGRATION_79_80)

        migratedDb.query("SELECT userId, customerId FROM Bookings WHERE id = 2").use { cursor ->
            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            val userId = cursor.getLong(0)
            val customerId = cursor.getLong(1)
            assertThat(userId).isEqualTo(0)
            assertThat(customerId).isEqualTo(0)
        }
    }

    @Test
    fun testMigration83to84_addsSupportChatBookmarkEntity() {
        helper.createDatabase(TEST_DB, 83).close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 84, true)

        migratedDb.execSQL(
            """
            INSERT INTO SupportChatBookmarkEntity (
                chatId, localSiteId, remoteSiteId, botSlug, title, createdAt, updatedAt
            ) VALUES (
                1234, 10, 20, 'woo-workflow-support_mobile_inapp_all_users', 'Order help', 1000, 2000
            )
            """.trimIndent()
        )

        migratedDb.query(
            """
            SELECT chatId, localSiteId, remoteSiteId, botSlug, title, createdAt, updatedAt
            FROM SupportChatBookmarkEntity
            WHERE chatId = 1234
            """.trimIndent()
        ).use { cursor ->
            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getLong(0)).isEqualTo(1234)
            assertThat(cursor.getInt(1)).isEqualTo(10)
            assertThat(cursor.getLong(2)).isEqualTo(20)
            assertThat(cursor.getString(3)).isEqualTo("woo-workflow-support_mobile_inapp_all_users")
            assertThat(cursor.getString(4)).isEqualTo("Order help")
            assertThat(cursor.getLong(5)).isEqualTo(1000)
            assertThat(cursor.getLong(6)).isEqualTo(2000)
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
