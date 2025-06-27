package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.mockito.Mockito
import org.wordpress.android.fluxc.persistence.converters.CurrencyPositionConverter

object TestDatabase {
    fun provideTestDatabase(appContext: Context): RoomDatabase.Builder<WCAndroidDatabase> {
        return Room.inMemoryDatabaseBuilder(appContext, WCAndroidDatabase::class.java)
            .addTypeConverter(CurrencyPositionConverter(Mockito.mock()))
            .allowMainThreadQueries()
    }
}
