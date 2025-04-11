package org.wordpress.android.fluxc.upload;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;

class UploadTestUtils {
    private static final int TEST_LOCAL_SITE_ID = 42;

    static MediaModel getTestMedia(long mediaId) {
        return new MediaModel(
                TEST_LOCAL_SITE_ID,
                mediaId
        );
    }

    static MediaModel getLocalTestMedia() {
        return new MediaModel(
                TEST_LOCAL_SITE_ID,
                0
        );
    }

    static SiteModel getTestSite() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(TEST_LOCAL_SITE_ID);
        return siteModel;
    }

    static MediaUploadModel getMediaUploadModelForMediaModel(MediaModel mediaModel) {
        return UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
    }
}
