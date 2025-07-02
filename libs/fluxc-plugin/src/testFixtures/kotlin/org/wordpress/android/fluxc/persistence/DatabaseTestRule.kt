package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Room
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mockito
import org.wordpress.android.fluxc.persistence.converters.CurrencyPositionConverter

class DatabaseTestRule(private val appContext: Context) : TestWatcher() {

    lateinit var db: WCAndroidDatabase

    override fun starting(description: Description?) {
        db = Room.inMemoryDatabaseBuilder(appContext, WCAndroidDatabase::class.java)
            .addTypeConverter(CurrencyPositionConverter(Mockito.mock()))
            .allowMainThreadQueries()
            .build()
    }

    override fun finished(description: Description?) {
        db.close()
    }
}
