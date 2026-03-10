package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.persistence.entity.NotificationEntity
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import javax.inject.Inject

internal class NotificationMapper @Inject constructor(
    private val formattableContentMapper: FormattableContentMapper
) {
    fun toEntity(model: NotificationModel): NotificationEntity {
        return NotificationEntity(
            remoteSiteId = RemoteId(model.remoteSiteId),
            remoteNoteId = RemoteId(model.remoteNoteId),
            noteHash = model.noteHash,
            type = model.type.toString(),
            subtype = model.subtype?.toString(),
            read = model.read,
            icon = model.icon,
            noticon = model.noticon,
            timestamp = model.timestamp,
            url = model.url,
            title = model.title,
            formattableBody = model.body?.let {
                formattableContentMapper.mapFormattableContentListToJson(it)
            },
            formattableSubject = model.subject?.let {
                formattableContentMapper.mapFormattableContentListToJson(it)
            },
            formattableMeta = model.meta?.let {
                formattableContentMapper.mapFormattableMetaToJson(it)
            }
        )
    }

    fun toDomainModel(entity: NotificationEntity): NotificationModel {
        return NotificationModel(
            remoteNoteId = entity.remoteNoteId.value,
            remoteSiteId = entity.remoteSiteId.value,
            noteHash = entity.noteHash,
            type = NotificationModel.Kind.fromString(entity.type),
            subtype = entity.subtype?.let { NotificationModel.Subkind.fromString(it) },
            read = entity.read,
            icon = entity.icon,
            noticon = entity.noticon,
            timestamp = entity.timestamp,
            url = entity.url,
            title = entity.title,
            body = entity.formattableBody?.let {
                formattableContentMapper.mapToFormattableContentList(it)
            },
            subject = entity.formattableSubject?.let {
                formattableContentMapper.mapToFormattableContentList(it)
            },
            meta = entity.formattableMeta?.let {
                formattableContentMapper.mapToFormattableMeta(it)
            }
        )
    }
}
