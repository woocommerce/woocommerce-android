package org.wordpress.android.fluxc.persistence

import androidx.room.migration.AutoMigrationSpec
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
        listOf<AutoMigrationSpec>()
    )

    @Test
    fun testRoomDBSchemasUnchanged() {
        val context = InstrumentationRegistry.getInstrumentation().context
        for(i in 3..WC_DATABASE_VERSION){
            val currentHash = MigrationSchemasUtils.getIdentityHash(i, context)
            val expectedHash = MigrationSchemasUtils.DB_HASHES.getValue(
                MigrationSchemasUtils.getDBHashKey(i)
            )
            assertThat(currentHash).isEqualTo(expectedHash)
        }
    }

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

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
