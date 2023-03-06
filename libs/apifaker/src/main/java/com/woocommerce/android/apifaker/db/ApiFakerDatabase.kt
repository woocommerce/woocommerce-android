package com.woocommerce.android.apifaker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.woocommerce.android.apifaker.models.Endpoint
import com.woocommerce.android.apifaker.models.FakeResponse

@Database(
    entities = [
        Endpoint::class, FakeResponse::class
    ],
    version = 1
)
internal abstract class ApiFakerDatabase : RoomDatabase() {
    companion object {
        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
            applicationContext,
            ApiFakerDatabase::class.java,
            "api-faker-db"
        ).build()
    }

    abstract val endpointDao: EndpointDao
}
