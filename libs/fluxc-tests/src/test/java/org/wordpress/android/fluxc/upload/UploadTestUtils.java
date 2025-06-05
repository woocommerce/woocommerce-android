package org.wordpress.android.fluxc.upload;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;

class UploadTestUtils {
    private static final int TEST_LOCAL_SITE_ID = 42;

    static MediaModel getTestMedia(long mediaId) {
        return new MediaModel(
                TEST_LOCAL_SITE_ID,
                mediaId
        );
    }

    static SiteModel getTestSite() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(TEST_LOCAL_SITE_ID);
        return siteModel;
    }
}
