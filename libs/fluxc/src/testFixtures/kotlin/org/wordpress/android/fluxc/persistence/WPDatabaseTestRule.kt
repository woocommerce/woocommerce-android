package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Room
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class WPDatabaseTestRule(private val appContext: Context) : TestWatcher() {
    lateinit var db: WPAndroidDatabase

    override fun starting(description: Description?) {
        db = Room.inMemoryDatabaseBuilder(appContext, WPAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    override fun finished(description: Description?) {
        db.close()
    }
}
