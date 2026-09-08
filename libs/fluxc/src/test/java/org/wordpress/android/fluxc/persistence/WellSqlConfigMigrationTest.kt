package org.wordpress.android.fluxc.persistence

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.yarolegovich.wellsql.WellTableManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(RobolectricTestRunner::class)
class WellSqlConfigMigrationTest {
    @Test
    fun `given existing site row, when upgrading from 243, then preserve it with unknown HTTPS state`() {
        val database = SQLiteDatabase.create(null).apply {
            execSQL("CREATE TABLE SiteModel (_id INTEGER PRIMARY KEY AUTOINCREMENT, URL TEXT)")
            execSQL("INSERT INTO SiteModel (URL) VALUES ('http://test.com')")
        }
        val config = WellSqlConfig(ApplicationProvider.getApplicationContext())

        config.onUpgrade(database, mock<WellTableManager>(), 243, 245)

        database.query(
            "SiteModel",
            arrayOf("URL", "HTTPS_CONFIGURATION_STATE"),
            null,
            null,
            null,
            null,
            null,
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("http://test.com")
            assertThat(cursor.getInt(1)).isEqualTo(SiteModel.HTTPS_CONFIGURATION_UNKNOWN)
        }
        assertThat(config.dbVersion).isEqualTo(245)
    }
}
