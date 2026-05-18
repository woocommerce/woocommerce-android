package org.wordpress.android.fluxc.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Kind
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind
import org.wordpress.android.fluxc.persistence.entity.NotificationEntity
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import org.wordpress.android.fluxc.tools.FormattableMeta

class NotificationMapperTest {
    private val mockFormattableContentMapper = mock<FormattableContentMapper>()
    private lateinit var mapper: NotificationMapper

    @Before
    fun setUp() {
        mapper = NotificationMapper(mockFormattableContentMapper)
    }

    // region toEntity
    @Test
    fun `given model with all fields, when toEntity, then maps all fields correctly`() {
        val model = notificationModel()
        whenever(mockFormattableContentMapper.mapFormattableContentListToJson(any()))
            .thenReturn(BODY_JSON, SUBJECT_JSON)
        whenever(mockFormattableContentMapper.mapFormattableMetaToJson(any()))
            .thenReturn(META_JSON)

        val entity = mapper.toEntity(model)

        assertThat(entity.remoteSiteId).isEqualTo(RemoteId(REMOTE_SITE_ID))
        assertThat(entity.remoteNoteId).isEqualTo(RemoteId(REMOTE_NOTE_ID))
        assertThat(entity.noteHash).isEqualTo(NOTE_HASH)
        assertThat(entity.type).isEqualTo(Kind.STORE_ORDER.toString())
        assertThat(entity.subtype).isEqualTo(Subkind.STORE_REVIEW.toString())
        assertThat(entity.read).isEqualTo(READ)
        assertThat(entity.icon).isEqualTo(ICON)
        assertThat(entity.noticon).isEqualTo(NOTICON)
        assertThat(entity.timestamp).isEqualTo(TIMESTAMP)
        assertThat(entity.url).isEqualTo(URL)
        assertThat(entity.title).isEqualTo(TITLE)
        assertThat(entity.formattableBody).isEqualTo(BODY_JSON)
        assertThat(entity.formattableSubject).isEqualTo(SUBJECT_JSON)
        assertThat(entity.formattableMeta).isEqualTo(META_JSON)
    }

    @Test
    fun `given model with null optional fields, when toEntity, then maps nulls correctly`() {
        val model = notificationModel(
            subtype = null,
            icon = null,
            noticon = null,
            timestamp = null,
            url = null,
            title = null,
            body = null,
            subject = null,
            meta = null
        )

        val entity = mapper.toEntity(model)

        assertThat(entity.subtype).isNull()
        assertThat(entity.icon).isNull()
        assertThat(entity.noticon).isNull()
        assertThat(entity.timestamp).isNull()
        assertThat(entity.url).isNull()
        assertThat(entity.title).isNull()
        assertThat(entity.formattableBody).isNull()
        assertThat(entity.formattableSubject).isNull()
        assertThat(entity.formattableMeta).isNull()
    }

    @Test
    fun `given model with null body and subject, when toEntity, then does not call mapper for content list`() {
        val model = notificationModel(body = null, subject = null)

        mapper.toEntity(model)

        verify(mockFormattableContentMapper, never()).mapFormattableContentListToJson(any())
    }

    @Test
    fun `given model with null meta, when toEntity, then does not call mapper for meta`() {
        val model = notificationModel(meta = null)

        mapper.toEntity(model)

        verify(mockFormattableContentMapper, never()).mapFormattableMetaToJson(any())
    }
    // endregion

    // region toDomainModel
    @Test
    fun `given entity with all fields, when toDomainModel, then maps all fields correctly`() {
        val entity = notificationEntity()
        whenever(mockFormattableContentMapper.mapToFormattableContentList(BODY_JSON))
            .thenReturn(listOf(FormattableContent(text = "body")))
        whenever(mockFormattableContentMapper.mapToFormattableContentList(SUBJECT_JSON))
            .thenReturn(listOf(FormattableContent(text = "subject")))
        whenever(mockFormattableContentMapper.mapToFormattableMeta(META_JSON))
            .thenReturn(FormattableMeta())

        val model = mapper.toDomainModel(entity)

        assertThat(model.remoteNoteId).isEqualTo(REMOTE_NOTE_ID)
        assertThat(model.remoteSiteId).isEqualTo(REMOTE_SITE_ID)
        assertThat(model.noteHash).isEqualTo(NOTE_HASH)
        assertThat(model.type).isEqualTo(Kind.STORE_ORDER)
        assertThat(model.subtype).isEqualTo(Subkind.STORE_REVIEW)
        assertThat(model.read).isEqualTo(READ)
        assertThat(model.icon).isEqualTo(ICON)
        assertThat(model.noticon).isEqualTo(NOTICON)
        assertThat(model.timestamp).isEqualTo(TIMESTAMP)
        assertThat(model.url).isEqualTo(URL)
        assertThat(model.title).isEqualTo(TITLE)
        assertThat(model.body).isNotNull
        assertThat(model.subject).isNotNull
        assertThat(model.meta).isNotNull
    }

    @Test
    fun `given entity with null optional fields, when toDomainModel, then maps nulls correctly`() {
        val entity = notificationEntity(
            subtype = null,
            icon = null,
            noticon = null,
            timestamp = null,
            url = null,
            title = null,
            formattableBody = null,
            formattableSubject = null,
            formattableMeta = null
        )

        val model = mapper.toDomainModel(entity)

        assertThat(model.subtype).isNull()
        assertThat(model.icon).isNull()
        assertThat(model.noticon).isNull()
        assertThat(model.timestamp).isNull()
        assertThat(model.url).isNull()
        assertThat(model.title).isNull()
        assertThat(model.body).isNull()
        assertThat(model.subject).isNull()
        assertThat(model.meta).isNull()
    }

    @Test
    fun `given entity with null formattable content, when toDomainModel, then does not call mapper for content list`() {
        val entity = notificationEntity(formattableBody = null, formattableSubject = null)

        mapper.toDomainModel(entity)

        verify(mockFormattableContentMapper, never()).mapToFormattableContentList(any())
    }

    @Test
    fun `given entity with null formattableMeta, when toDomainModel, then does not call mapper for meta`() {
        val entity = notificationEntity(formattableMeta = null)

        mapper.toDomainModel(entity)

        verify(mockFormattableContentMapper, never()).mapToFormattableMeta(any())
    }
    // endregion

    // region Kind conversion
    @Test
    fun `given entity with store_order type, when toDomainModel, then maps to STORE_ORDER kind`() {
        val entity = notificationEntity(type = "STORE_ORDER")

        val model = mapper.toDomainModel(entity)

        assertThat(model.type).isEqualTo(Kind.STORE_ORDER)
    }

    @Test
    fun `given entity with lowercase type, when toDomainModel, then maps to correct kind`() {
        val entity = notificationEntity(type = "store_order")

        val model = mapper.toDomainModel(entity)

        assertThat(model.type).isEqualTo(Kind.STORE_ORDER)
    }

    @Test
    fun `given entity with store_stock type, when toDomainModel, then maps to STORE_STOCK kind`() {
        val entity = notificationEntity(type = "store_stock")

        val model = mapper.toDomainModel(entity)

        assertThat(model.type).isEqualTo(Kind.STORE_STOCK)
    }

    @Test
    fun `given entity with unknown type, when toDomainModel, then maps to UNKNOWN kind`() {
        val entity = notificationEntity(type = "some_unknown_type")

        val model = mapper.toDomainModel(entity)

        assertThat(model.type).isEqualTo(Kind.UNKNOWN)
    }

    @Test
    fun `given model with COMMENT kind, when toEntity, then maps type string correctly`() {
        val model = notificationModel(type = Kind.COMMENT)

        val entity = mapper.toEntity(model)

        assertThat(entity.type).isEqualTo("COMMENT")
    }
    // endregion

    // region Subkind conversion
    @Test
    fun `given entity with store_review subtype, when toDomainModel, then maps to STORE_REVIEW`() {
        val entity = notificationEntity(subtype = "STORE_REVIEW")

        val model = mapper.toDomainModel(entity)

        assertThat(model.subtype).isEqualTo(Subkind.STORE_REVIEW)
    }

    @Test
    fun `given entity with lowercase subtype, when toDomainModel, then maps to correct subkind`() {
        val entity = notificationEntity(subtype = "store_review")

        val model = mapper.toDomainModel(entity)

        assertThat(model.subtype).isEqualTo(Subkind.STORE_REVIEW)
    }

    @Test
    fun `given entity with unknown subtype, when toDomainModel, then maps to UNKNOWN`() {
        val entity = notificationEntity(subtype = "some_unknown_subtype")

        val model = mapper.toDomainModel(entity)

        assertThat(model.subtype).isEqualTo(Subkind.UNKNOWN)
    }

    @Test
    fun `given model with NONE subkind, when toEntity, then maps subtype string correctly`() {
        val model = notificationModel(subtype = Subkind.NONE)

        val entity = mapper.toEntity(model)

        assertThat(entity.subtype).isEqualTo("NONE")
    }

    @Test
    fun `given model with null subkind, when toEntity, then subtype is null`() {
        val model = notificationModel(subtype = null)

        val entity = mapper.toEntity(model)

        assertThat(entity.subtype).isNull()
    }
    // endregion

    // region Round-trip conversion
    @Test
    fun `given model, when converted to entity and back, then preserves required fields`() {
        val original = notificationModel(
            body = null,
            subject = null,
            meta = null
        )

        val entity = mapper.toEntity(original)
        val restored = mapper.toDomainModel(entity)

        assertThat(restored.remoteNoteId).isEqualTo(original.remoteNoteId)
        assertThat(restored.remoteSiteId).isEqualTo(original.remoteSiteId)
        assertThat(restored.noteHash).isEqualTo(original.noteHash)
        assertThat(restored.type).isEqualTo(original.type)
        assertThat(restored.read).isEqualTo(original.read)
    }
    // endregion

    /* HELPER */

    @Suppress("LongParameterList")
    private fun notificationModel(
        remoteNoteId: Long = REMOTE_NOTE_ID,
        remoteSiteId: Long = REMOTE_SITE_ID,
        noteHash: Long = NOTE_HASH,
        type: Kind = Kind.STORE_ORDER,
        subtype: Subkind? = Subkind.STORE_REVIEW,
        read: Boolean = READ,
        icon: String? = ICON,
        noticon: String? = NOTICON,
        timestamp: String? = TIMESTAMP,
        url: String? = URL,
        title: String? = TITLE,
        body: List<FormattableContent>? = listOf(FormattableContent(text = "body")),
        subject: List<FormattableContent>? = listOf(FormattableContent(text = "subject")),
        meta: FormattableMeta? = FormattableMeta()
    ) = NotificationModel(
        remoteNoteId = remoteNoteId,
        remoteSiteId = remoteSiteId,
        noteHash = noteHash,
        type = type,
        subtype = subtype,
        read = read,
        icon = icon,
        noticon = noticon,
        timestamp = timestamp,
        url = url,
        title = title,
        body = body,
        subject = subject,
        meta = meta
    )

    @Suppress("LongParameterList")
    private fun notificationEntity(
        remoteSiteId: RemoteId = RemoteId(REMOTE_SITE_ID),
        remoteNoteId: RemoteId = RemoteId(REMOTE_NOTE_ID),
        noteHash: Long = NOTE_HASH,
        type: String = Kind.STORE_ORDER.toString(),
        subtype: String? = Subkind.STORE_REVIEW.toString(),
        read: Boolean = READ,
        icon: String? = ICON,
        noticon: String? = NOTICON,
        timestamp: String? = TIMESTAMP,
        url: String? = URL,
        title: String? = TITLE,
        formattableBody: String? = BODY_JSON,
        formattableSubject: String? = SUBJECT_JSON,
        formattableMeta: String? = META_JSON
    ) = NotificationEntity(
        remoteSiteId = remoteSiteId,
        remoteNoteId = remoteNoteId,
        noteHash = noteHash,
        type = type,
        subtype = subtype,
        read = read,
        icon = icon,
        noticon = noticon,
        timestamp = timestamp,
        url = url,
        title = title,
        formattableBody = formattableBody,
        formattableSubject = formattableSubject,
        formattableMeta = formattableMeta
    )

    companion object {
        private const val REMOTE_NOTE_ID = 12345L
        private const val REMOTE_SITE_ID = 67890L
        private const val NOTE_HASH = 111222333L
        private const val READ = false
        private const val ICON = "https://example.com/icon.png"
        private const val NOTICON = "\uf300"
        private const val TIMESTAMP = "2024-01-15T10:30:00Z"
        private const val URL = "https://example.com/notification"
        private const val TITLE = "New Order"
        private const val BODY_JSON = """[{"text":"body"}]"""
        private const val SUBJECT_JSON = """[{"text":"subject"}]"""
        private const val META_JSON = """{"ids":{"order":123}}"""
    }
}
