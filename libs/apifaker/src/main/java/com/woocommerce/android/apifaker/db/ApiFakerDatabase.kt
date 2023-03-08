package com.woocommerce.android.apifaker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response

@Database(
    entities = [
        Request::class, Response::class
    ],
    version = 1
)
@TypeConverters(EndpointTypeConverter::class)
internal abstract class ApiFakerDatabase : RoomDatabase() {
    companion object {
        fun buildDb(applicationContext: Context) = Room
            .databaseBuilder(
                applicationContext, ApiFakerDatabase::class.java, "api-faker-db"
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    abstract val endpointDao: EndpointDao
}
