package org.wordpress.android.fluxc.persistence

import android.content.Context
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class DatabaseTestRule(private val context: Context) : TestWatcher() {

    lateinit var db: WCAndroidDatabase

    override fun starting(description: Description?) {
        db = TestDatabase.provideTestDatabase(context)
            .build()
    }

    override fun finished(description: Description?) {
        db.close()
    }
}
