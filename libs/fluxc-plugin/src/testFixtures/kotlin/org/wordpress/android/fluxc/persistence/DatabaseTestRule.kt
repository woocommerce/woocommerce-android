package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mockito
import org.wordpress.android.fluxc.persistence.converters.CurrencyPositionConverter
import org.wordpress.android.fluxc.persistence.converters.StatsGranularityConverter

class DatabaseTestRule(
    private val appContext: Context,
    private val openHelperFactory: SupportSQLiteOpenHelper.Factory? = null
) : TestWatcher() {

    lateinit var db: WCAndroidDatabase

    override fun starting(description: Description?) {
        val builder = Room.inMemoryDatabaseBuilder(appContext, WCAndroidDatabase::class.java)
            .addTypeConverter(CurrencyPositionConverter(Mockito.mock()))
            .addTypeConverter(StatsGranularityConverter(Mockito.mock()))
            .allowMainThreadQueries()
        openHelperFactory?.let { builder.openHelperFactory(it) }
        db = builder.build()
    }

    override fun finished(description: Description?) {
        db.close()
    }
}
