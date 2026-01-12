package org.wordpress.android.fluxc.media;

// COMMENTED OUT: MediaSqlUtils no longer exists - needs migration to Room

/*

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;

import com.wellsql.generated.MediaModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.utils.MimeType.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class MediaSqlUtilsTest {
    private static final int TEST_LOCAL_SITE_ID = 42;
    private static final int SMALL_TEST_POOL = 10;

    private final Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.getApplication().getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, MediaModel.class);
        WellSql.init(config);
        config.reset();
    }

    // Inserts a media item with various known fields then retrieves and validates those fields
    @Test
    public void testInsertMedia() {
        long testId = Math.abs(mRandom.nextLong());
        String testTitle = getTestString();
        String testDescription = getTestString();
        String testCaption = getTestString();
        MediaModel testMedia = getTestMedia(testId, testTitle, testDescription, testCaption);
        assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(testMedia));
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), testId);
        assertThat(media).hasSize(1);
        assertThat(media.get(0)).isNotNull();
        assertThat(media.get(0).getMediaId()).isEqualTo(testId);
        assertThat(media.get(0).getTitle()).isEqualTo(testTitle);
        assertThat(media.get(0).getDescription()).isEqualTo(testDescription);
        assertThat(media.get(0).getCaption()).isEqualTo(testCaption);
    }

    // Inserts media of multiple MIME types then retrieves only images and verifies
    @Test
    public void testGetSiteImages() {
        List<Long> imageIds = new ArrayList<>(SMALL_TEST_POOL);
        List<Long> videoIds = new ArrayList<>(SMALL_TEST_POOL);
        for (int i = 0; i < imageIds.size(); ++i) {
            imageIds.add(mRandom.nextLong());
            videoIds.add(mRandom.nextLong());
            MediaModel image = getTestMedia(imageIds.get(i));
            image.setMimeType("image/jpg");
            MediaModel video = getTestMedia(videoIds.get(i));
            video.setMimeType("video/mp4");
            assertThat(MediaSqlUtils.insertOrUpdateMedia(image)).isEqualTo(0);
            assertThat(MediaSqlUtils.insertOrUpdateMedia(video)).isEqualTo(0);
        }
        List<MediaModel> images = MediaSqlUtils.getSiteImages(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID));
        assertThat(imageIds.size()).isEqualTo(images.size());
        for (int i = 0; i < imageIds.size(); ++i) {
            assertThat(images.get(0).getMimeType().contains(Type.IMAGE.getValue())).isTrue();
            assertThat(imageIds).contains(images.get(i).getMediaId());
        }
    }

    // Inserts many images then retrieves all images with a supplied exclusion filter
    @Test
    public void testGetSiteImagesExclusionFilter() {
        long[] imageIds = insertImageTestItems();
        List<Long> exclusion = new ArrayList<>();
        for (int i = 0; i < SMALL_TEST_POOL; i += 2) {
            exclusion.add(imageIds[i]);
        }
        List<MediaModel> includedImages = MediaSqlUtils
                .getSiteImagesExcluding(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), exclusion);
        assertThat(includedImages).hasSize(SMALL_TEST_POOL - exclusion.size());
        for (int i = 0; i < includedImages.size(); ++i) {
            assertThat(exclusion).doesNotContain(includedImages.get(i).getMediaId());
        }
    }

    // Utilities

    private long[] insertImageTestItems() {
        long[] testItemIds = new long[MediaSqlUtilsTest.SMALL_TEST_POOL];
        for (int i = 0; i < MediaSqlUtilsTest.SMALL_TEST_POOL; ++i) {
            testItemIds[i] = Math.abs(mRandom.nextInt());
            MediaModel image = getTestMedia(testItemIds[i]);
            image.setMimeType("image/jpg");
            image.setUploadState(MediaUploadState.UPLOADED);
            assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(image));
        }
        return testItemIds;
    }

    private MediaModel getTestMedia(long mediaId) {
        return new MediaModel(
                TEST_LOCAL_SITE_ID,
                mediaId
        );
    }

    private MediaModel getTestMedia(long mediaId, String title, String description, String caption) {
        MediaModel media = new MediaModel(
                TEST_LOCAL_SITE_ID,
                mediaId
        );
        media.setTitle(title);
        media.setDescription(description);
        media.setCaption(caption);
        return media;
    }

    private String getTestString() {
        return "BaseTestString-" + mRandom.nextInt();
    }

    private SiteModel getTestSiteWithLocalId(int localSiteId) {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(localSiteId);
        return siteModel;
    }
}
*/
