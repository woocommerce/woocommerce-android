package com.woocommerce.android.util

import com.google.gson.Gson
import org.wordpress.android.util.AppLog
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object UnitTestUtils {
    fun <T> String.jsonFileAs(clazz: Class<T>) =
        getStringFromResourceFile(
            this
        )?.let { Gson().fromJson(it, clazz) }

    private fun getStringFromResourceFile(
        fileName: String
    ) = try {
        this::class.java.classLoader?.getResourceAsStream(fileName)
            ?.let { BufferedReader(InputStreamReader(it, "UTF-8")) }
            ?.let { bufferedReader ->
                val buffer = StringBuilder()
                var lineString: String?
                while (bufferedReader.readLine().also { lineString = it } != null) {
                    buffer.append(lineString)
                }
                bufferedReader.close()
                buffer.toString()
            }
    } catch (e: IOException) {
        AppLog.e(AppLog.T.TESTS, "Could not load response JSON file.")
        null
    }
}

