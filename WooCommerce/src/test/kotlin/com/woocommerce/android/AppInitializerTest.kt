package com.woocommerce.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.view.WindowManager
import com.woocommerce.android.tracker.SendTelemetry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class AppInitializerTest {

    private lateinit var sut: AppInitializer

    private val sendTelemetry: SendTelemetry = mock()

    private val defaultSharedPreferences: SharedPreferences = mock {
        on { edit() } doReturn mock()
    }

    private val packageInfo: PackageInfo = PackageInfo().apply {
        firstInstallTime = 2
    }

    private val packageManager: PackageManager = mock {
        on { getPackageInfo(any<String>(), any<Int>()) } doReturn packageInfo
        on { checkPermission(any(), any()) } doReturn PackageManager.PERMISSION_GRANTED
        on { getApplicationInfo(any(), any()) } doReturn ApplicationInfo().apply { packageName = "" }
        on { getApplicationLabel(any()) } doReturn ""
    }

    private val windowManager: WindowManager = mock {
        on { defaultDisplay } doReturn mock()
    }

    private val resources: Resources = mock {
        on { configuration } doReturn Configuration().apply { orientation = Configuration.ORIENTATION_PORTRAIT }
    }

    private val applicationContext: Context = mock {
        on { getSharedPreferences(any(), any()) } doReturn defaultSharedPreferences
        on { packageName } doReturn ""
        on { packageManager } doReturn packageManager
        on { applicationInfo } doReturn ApplicationInfo().apply { packageName = "" }
        on { getSystemService(Context.WINDOW_SERVICE) } doReturn windowManager
        on { resources } doReturn resources
    }

    private val application: Application = mock {
        on { applicationContext } doReturn applicationContext
        on { getSharedPreferences(any(), any()) } doReturn defaultSharedPreferences
        on { packageName } doReturn ""
        on { packageManager } doReturn packageManager
    }

    @Before
    fun setUp() {
        sut = AppInitializer().apply {
            dispatcher = mock()
        }
        AppPrefs.init(application)
    }

    @Test
    fun `Should send telemetry on initialization`() = runBlockingTest {
        sut.init(application)

        verify(sendTelemetry).invoke(BuildConfig.VERSION_NAME, testSiteModel)
    }

    private companion object {
        val testSiteModel = SiteModel()
    }
}
