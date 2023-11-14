@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.helpers

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.fasterxml.jackson.databind.util.ISO8601Utils
import com.github.jknack.handlebars.Options
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.DateOffset
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.HandlebarsHelper
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.woocommerce.android.di.AndroidNotifier
import com.woocommerce.android.di.AssetFileSource
import org.apache.commons.lang3.LocaleUtils
import org.junit.Before
import org.junit.Rule
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

open class TestBase(failOnUnmatchedWireMockRequests: Boolean = true) {
    protected lateinit var appContext: Application

    companion object {
        val wireMockPort = 8080
    }

    @Before
    open fun setup() {
        appContext = getInstrumentation().targetContext.applicationContext as Application
    }

    @Rule @JvmField
    val wireMockRule = WireMockRule(
        options().port(wireMockPort)
            .fileSource(AssetFileSource(getInstrumentation().context.assets))
            .extensions(ResponseTemplateTransformer(true, "fnow", UnlocalizedDateHelper()))
            .notifier(AndroidNotifier()),
        failOnUnmatchedWireMockRequests
    )
}

internal class UnlocalizedDateHelper : HandlebarsHelper<Any?>() {
    @Throws(IOException::class)
    override fun apply(context: Any?, options: Options): Any {
        val format: String? = options.hash("format", null)
        val offset: String? = options.hash("offset", null)
        val timezone: String? = options.hash("timezone", null)
        val localeCode: String? = options.hash("locale", "en_US_POSIX")
        var date = Date()
        if (offset != null) {
            date = DateOffset(offset).shift(date)
        }
        var locale: Locale = Locale.getDefault()
        if (localeCode != null) {
            locale = LocaleUtils.toLocale(localeCode)
        }
        return LocaleAwareRenderableDate(date, format, timezone, locale)
    }
}

internal class LocaleAwareRenderableDate(date: Date, format: String?, timezone: String?, locale: Locale?) {
    private val mDate: Date
    private val mFormat: String?
    private val mTimezoneName: String?
    private val mLocale: Locale?
    override fun toString(): String {
        if (mFormat != null) {
            if (mFormat == "epoch") {
                return java.lang.String.valueOf(mDate.getTime())
            }
            return if (mFormat == "unix") {
                java.lang.String.valueOf(mDate.getTime() / DIVIDE_MILLISECONDS_TO_SECONDS)
            } else formatCustom()
        }
        return if (mTimezoneName != null) ISO8601Utils.format(
            mDate,
            false,
            TimeZone.getTimeZone(mTimezoneName)
        ) else ISO8601Utils.format(mDate, false)
    }

    private fun formatCustom(): String {
        val dateFormat = SimpleDateFormat(mFormat, mLocale)
        if (mTimezoneName != null) {
            val zone: TimeZone = TimeZone.getTimeZone(mTimezoneName)
            dateFormat.setTimeZone(zone)
        }
        return dateFormat.format(mDate)
    }

    companion object {
        private const val DIVIDE_MILLISECONDS_TO_SECONDS = 1000L
    }

    init {
        mDate = date
        mFormat = format
        mTimezoneName = timezone
        mLocale = locale
    }
}
