package com.woocommerce.android.ui.jitm

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JitmStore

@OptIn(ExperimentalCoroutinesApi::class)
class JitmStoreWrapperTest : BaseUnitTest() {
    private val realStore = mock<JitmStore>()

    @Test
    fun `given testing mode is enabled, when fetchJitmMessage is called, real store is not called`() =
        testBlocking {
            // GIVEN
            val site = mock<SiteModel>()
            val messagePath = "messagePath"
            val query = "query"
            val jsonFile = "jsonFileName"
            val wrapperData = mock<JitmStoreWrapperData> {
                on { isTestingModeEnabled }.thenReturn(true)
                on { jsonFileName }.thenReturn(jsonFile)
            }
            val jsonReader: JitmStoreWrapperJsonReader = mock {
                on { parseJsonFile(jsonFile) }.thenReturn(emptyArray())
            }
            val wrapper = JitmStoreWrapper(
                realStore = realStore,
                wrapperData = wrapperData,
                jsonReader = jsonReader,
            )

            // WHEN
            wrapper.fetchJitmMessage(site, messagePath, query)

            // THEN
            verify(realStore, never()).fetchJitmMessage(site, messagePath, query)
        }

    @Test
    fun `given testing mode is disabled, when fetchJitmMessage is called, real store is called`() =
        testBlocking {
            // GIVEN
            val site = mock<SiteModel>()
            val messagePath = "messagePath"
            val query = "query"
            val wrapperData = mock<JitmStoreWrapperData> {
                on { isTestingModeEnabled }.thenReturn(false)
            }
            val wrapper = JitmStoreWrapper(
                realStore = realStore,
                wrapperData = wrapperData,
                jsonReader = mock(),
            )

            // WHEN
            wrapper.fetchJitmMessage(site, messagePath, query)

            // THEN
            verify(realStore).fetchJitmMessage(site, messagePath, query)
        }

    @Test
    fun `given testing mode is enabled, when dismissJitmMessage is called, real store is not called`() =
        testBlocking {
            // GIVEN
            val site = mock<SiteModel>()
            val jitmId = "jitmId"
            val featureClass = "featureClass"
            val wrapperData = mock<JitmStoreWrapperData> {
                on { isTestingModeEnabled }.thenReturn(true)
            }
            val wrapper = JitmStoreWrapper(
                realStore = realStore,
                wrapperData = wrapperData,
                jsonReader = mock(),
            )

            // WHEN
            wrapper.dismissJitmMessage(site, jitmId, featureClass)

            // THEN
            verify(realStore, never()).dismissJitmMessage(site, jitmId, featureClass)
        }

    @Test
    fun `given testing mode is disabled, when dismissJitmMessage is called, real store is called`() =
        testBlocking {
            // GIVEN
            val site = mock<SiteModel>()
            val jitmId = "jitmId"
            val featureClass = "featureClass"
            val wrapperData = mock<JitmStoreWrapperData> {
                on { isTestingModeEnabled }.thenReturn(false)
            }
            val wrapper = JitmStoreWrapper(
                realStore = realStore,
                wrapperData = wrapperData,
                jsonReader = mock()
            )

            // WHEN
            wrapper.dismissJitmMessage(site, jitmId, featureClass)

            // THEN
            verify(realStore).dismissJitmMessage(site, jitmId, featureClass)
        }
}
