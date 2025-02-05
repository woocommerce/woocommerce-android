package org.wordpress.android.fluxc.wc.order

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.list.ListDescriptorUnitTestCase
import org.wordpress.android.fluxc.list.post.LIST_DESCRIPTOR_TEST_FIRST_MOCK_SITE_LOCAL_SITE_ID
import org.wordpress.android.fluxc.list.post.LIST_DESCRIPTOR_TEST_QUERY_1
import org.wordpress.android.fluxc.list.post.LIST_DESCRIPTOR_TEST_QUERY_2
import org.wordpress.android.fluxc.list.post.LIST_DESCRIPTOR_TEST_SECOND_MOCK_SITE_LOCAL_SITE_ID
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor

private typealias WCOrderListDescriptorTestCase = ListDescriptorUnitTestCase<WCOrderListDescriptor>

@RunWith(Parameterized::class)
internal class WCOrderListDescriptorTest(
    private val testCase: WCOrderListDescriptorTestCase
) {
    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): List<WCOrderListDescriptorTestCase> {
            val mockSite = mock<SiteModel>()
            val mockSite2 = mock<SiteModel>()
            whenever(mockSite.id).thenReturn(LIST_DESCRIPTOR_TEST_FIRST_MOCK_SITE_LOCAL_SITE_ID)
            whenever(mockSite2.id).thenReturn(LIST_DESCRIPTOR_TEST_SECOND_MOCK_SITE_LOCAL_SITE_ID)
            return listOf(
                    // Same site
                    WCOrderListDescriptorTestCase(
                            typeIdentifierReason = "Same sites should have same type identifier",
                            uniqueIdentifierReason = "Same sites should have same unique identifier",
                            descriptor1 = WCOrderListDescriptor(site = mockSite),
                            descriptor2 = WCOrderListDescriptor(site = mockSite),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = true
                    ),
                    // Different site
                    WCOrderListDescriptorTestCase(
                            typeIdentifierReason = "Different sites should have different type identifiers",
                            uniqueIdentifierReason = "Different sites should have different unique identifiers",
                            descriptor1 = WCOrderListDescriptor(site = mockSite),
                            descriptor2 = WCOrderListDescriptor(site = mockSite2),
                            shouldHaveSameTypeIdentifier = false,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    // Different status filters
                    WCOrderListDescriptorTestCase(
                            typeIdentifierReason = "Different status filters should have same type identifiers",
                            uniqueIdentifierReason = "Different status filters should have different " +
                                    "unique identifiers",
                            descriptor1 = WCOrderListDescriptor(
                                    site = mockSite,
                                    statusFilter = LIST_DESCRIPTOR_TEST_QUERY_1
                            ),
                            descriptor2 = WCOrderListDescriptor(
                                    site = mockSite,
                                    statusFilter = LIST_DESCRIPTOR_TEST_QUERY_2
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    // Different search query
                    WCOrderListDescriptorTestCase(
                            typeIdentifierReason = "Different search queries should have same type identifiers",
                            uniqueIdentifierReason = "Different search queries should have different " +
                                    "unique identifiers",
                            descriptor1 = WCOrderListDescriptor(
                                    site = mockSite,
                                    searchQuery = LIST_DESCRIPTOR_TEST_QUERY_1
                            ),
                            descriptor2 = WCOrderListDescriptor(
                                    site = mockSite,
                                    statusFilter = LIST_DESCRIPTOR_TEST_QUERY_2
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    )
            )
        }
    }

    @Test
    fun `test type identifier`() {
        testCase.testTypeIdentifier()
    }

    @Test
    fun `test unique identifier`() {
        testCase.testUniqueIdentifier()
    }
}
