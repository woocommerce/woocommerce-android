package org.wordpress.android.fluxc.persistence

import android.database.Cursor
import android.database.CursorWindow
import android.database.sqlite.SQLiteCursor
import android.os.Build
import android.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

// Mirrors `WooWellSqlConfig.getCursorWindowSize`; only active on API 28+.
internal class LargeCursorWindowOpenHelperFactory(
    private val windowSizeBytes: Long,
    private val delegate: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory()
) : SupportSQLiteOpenHelper.Factory {

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration
    ): SupportSQLiteOpenHelper = OpenHelperWrapper(delegate.create(configuration), windowSizeBytes)

    private class OpenHelperWrapper(
        private val delegate: SupportSQLiteOpenHelper,
        private val windowSizeBytes: Long
    ) : SupportSQLiteOpenHelper by delegate {
        override val writableDatabase: SupportSQLiteDatabase
            get() = DatabaseWrapper(delegate.writableDatabase, windowSizeBytes)

        override val readableDatabase: SupportSQLiteDatabase
            get() = DatabaseWrapper(delegate.readableDatabase, windowSizeBytes)
    }

    private class DatabaseWrapper(
        private val delegate: SupportSQLiteDatabase,
        private val windowSizeBytes: Long
    ) : SupportSQLiteDatabase by delegate {
        override fun query(query: String): Cursor =
            delegate.query(query).withLargerWindow()

        override fun query(query: String, bindArgs: Array<out Any?>): Cursor =
            delegate.query(query, bindArgs).withLargerWindow()

        override fun query(query: SupportSQLiteQuery): Cursor =
            delegate.query(query).withLargerWindow()

        override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor =
            delegate.query(query, cancellationSignal).withLargerWindow()

        private fun Cursor.withLargerWindow(): Cursor = apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && this is SQLiteCursor) {
                window = CursorWindow(null, windowSizeBytes)
            }
        }
    }
}
