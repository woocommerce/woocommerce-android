package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature
import org.wordpress.android.fluxc.persistence.entity.AppVersionTargets
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementFeatureEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementWithFeatures

internal object WhatsNewMapper {
    fun fromDomainModel(model: WhatsNewAnnouncementModel): WhatsNewAnnouncementWithFeatures {
        val announcementEntity = WhatsNewAnnouncementEntity(
            announcementId = RemoteId(model.announcementVersion.toLong()),
            minimumAppVersion = model.minimumAppVersion,
            maximumAppVersion = model.maximumAppVersion,
            appVersionTargets = AppVersionTargets(model.appVersionTargets),
            detailsUrl = model.detailsUrl,
            localized = model.isLocalized
        )

        val featureEntities = model.features.mapNotNull { feature ->
            if (feature.title != null) {
                WhatsNewAnnouncementFeatureEntity(
                    announcementId = RemoteId(model.announcementVersion.toLong()),
                    title = feature.title,
                    subtitle = feature.subtitle,
                    iconUrl = feature.iconUrl,
                    iconBase64 = feature.iconBase64
                )
            } else {
                null
            }
        }

        return WhatsNewAnnouncementWithFeatures(
            announcement = announcementEntity,
            features = featureEntities
        )
    }

    fun toDomainModel(entity: WhatsNewAnnouncementWithFeatures): WhatsNewAnnouncementModel {
        return WhatsNewAnnouncementModel(
            announcementVersion = entity.announcement.announcementId.value.toInt(),
            minimumAppVersion = entity.announcement.minimumAppVersion,
            maximumAppVersion = entity.announcement.maximumAppVersion,
            appVersionTargets = entity.announcement.appVersionTargets.value,
            detailsUrl = entity.announcement.detailsUrl,
            isLocalized = entity.announcement.localized,
            features = entity.features.map { featureToDomainModel(it) }
        )
    }

    private fun featureToDomainModel(feature: WhatsNewAnnouncementFeatureEntity): WhatsNewAnnouncementFeature {
        return WhatsNewAnnouncementFeature(
            title = feature.title,
            subtitle = feature.subtitle,
            iconBase64 = feature.iconBase64,
            iconUrl = feature.iconUrl
        )
    }
}
