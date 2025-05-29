package org.wordpress.android.fluxc.list

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.wordpress.android.fluxc.model.list.ListDescriptor

internal class ListDescriptorUnitTestCase<T : ListDescriptor>(
    val typeIdentifierReason: String,
    val uniqueIdentifierReason: String,
    val descriptor1: T,
    val descriptor2: T,
    val shouldHaveSameTypeIdentifier: Boolean,
    val shouldHaveSameUniqueIdentifier: Boolean
) {
    fun testTypeIdentifier() {
        if (shouldHaveSameTypeIdentifier) {
            assertSameTypeIdentifiers(
                    reason = typeIdentifierReason,
                    descriptor1 = descriptor1,
                    descriptor2 = descriptor2
            )
        } else {
            assertDifferentTypeIdentifiers(
                    reason = typeIdentifierReason,
                    descriptor1 = descriptor1,
                    descriptor2 = descriptor2
            )
        }
    }

    fun testUniqueIdentifier() {
        if (shouldHaveSameUniqueIdentifier) {
            assertSameUniqueIdentifiers(
                    reason = uniqueIdentifierReason,
                    descriptor1 = descriptor1,
                    descriptor2 = descriptor2
            )
        } else {
            assertDifferentUniqueIdentifiers(
                    reason = uniqueIdentifierReason,
                    descriptor1 = descriptor1,
                    descriptor2 = descriptor2
            )
        }
    }
}

fun assertSameTypeIdentifiers(reason: String, descriptor1: ListDescriptor, descriptor2: ListDescriptor) {
    assertThat(reason, descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
}

fun assertDifferentTypeIdentifiers(reason: String, descriptor1: ListDescriptor, descriptor2: ListDescriptor) {
    assertThat(reason, descriptor1.typeIdentifier, not(equalTo(descriptor2.typeIdentifier)))
}

fun assertSameUniqueIdentifiers(reason: String, descriptor1: ListDescriptor, descriptor2: ListDescriptor) {
    assertThat(reason, descriptor1.uniqueIdentifier, equalTo(descriptor2.uniqueIdentifier))
}

fun assertDifferentUniqueIdentifiers(reason: String, descriptor1: ListDescriptor, descriptor2: ListDescriptor) {
    assertThat(reason, descriptor1.uniqueIdentifier, not(equalTo(descriptor2.uniqueIdentifier)))
}
