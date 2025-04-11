package org.wordpress.android.fluxc.upload;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UploadSqlUtilsTest {
    private Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    // Attempts to insert null then verifies there is no media
    @Test
    public void testInsertNullMedia() {
        assertEquals(0, UploadSqlUtils.insertOrUpdateMedia(null));
        assertEquals(0, WellSql.select(MediaUploadModel.class).getAsCursor().getCount());
    }

    @Test
    public void testInsertMedia() {
        long testId = Math.abs(mRandom.nextLong());
        MediaModel testMedia = UploadTestUtils.getTestMedia(testId);
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia));
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(UploadTestUtils.getTestSite(), testId);
        assertEquals(1, media.size());
        assertNotNull(media.get(0));

        // Store a MediaUploadModel corresponding to this MediaModel
        testMedia = media.get(0);
        MediaUploadModel mediaUploadModel = new MediaUploadModel(testMedia.getId());
        mediaUploadModel.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(testMedia.getId(), mediaUploadModel.getId());
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Update the stored MediaUploadModel, marking it as completed
        mediaUploadModel.setUploadState(MediaUploadModel.COMPLETED);
        mediaUploadModel.setProgress(1F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(testMedia.getId(), mediaUploadModel.getId());
        assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());

        // Deleting the MediaModel should cause the corresponding MediaUploadModel to be deleted also
        MediaSqlUtils.deleteMedia(testMedia);

        media = MediaSqlUtils.getSiteMediaWithId(UploadTestUtils.getTestSite(), testId);
        assertTrue(media.isEmpty());

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNull(mediaUploadModel);
    }

    @Test
    public void testUpdateMediaProgress() {
        long testId = Math.abs(mRandom.nextLong());
        MediaModel testMedia = UploadTestUtils.getTestMedia(testId);
        MediaSqlUtils.insertOrUpdateMedia(testMedia);
        testMedia = MediaSqlUtils.getSiteMediaWithId(UploadTestUtils.getTestSite(), testId).get(0);

        // Store a MediaUploadModel corresponding to this MediaModel
        MediaUploadModel mediaUploadModel = new MediaUploadModel(testMedia.getId());
        mediaUploadModel.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(0.65F, mediaUploadModel.getProgress());

        // Update the progress for the MediaUploadModel
        mediaUploadModel.setProgress(0.87F);
        assertEquals(1, UploadSqlUtils.updateMediaProgressOnly(mediaUploadModel));

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(testMedia.getId(), mediaUploadModel.getId());
        assertEquals(0.87F, mediaUploadModel.getProgress());

        // Attempting to update the progress for a MediaUploadModel that doesn't exist in the db should fail
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(mRandom.nextInt());
        mediaUploadModel2.setProgress(0.45F);
        assertEquals(0, UploadSqlUtils.updateMediaProgressOnly(mediaUploadModel2));
        assertNull(UploadSqlUtils.getMediaUploadModelForLocalId(mediaUploadModel2.getId()));
    }

    @Test
    public void testDeleteMediaUploadModel() {
        MediaModel testMedia1 = UploadTestUtils.getTestMedia(65);
        MediaModel testMedia2 = UploadTestUtils.getTestMedia(35);

        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia1));
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia2));
        List<MediaModel> mediaModels = MediaSqlUtils.getAllSiteMedia(UploadTestUtils.getTestSite());
        assertEquals(2, mediaModels.size());

        // Store MediaUploadModels corresponding to the MediaModels
        testMedia1 = mediaModels.get(0);
        MediaUploadModel mediaUploadModel1 = new MediaUploadModel(testMedia1.getId());
        mediaUploadModel1.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel1);

        testMedia2 = mediaModels.get(1);
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(testMedia2.getId());
        mediaUploadModel2.setProgress(0.35F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel2);

        // Delete one of the MediaUploadModels
        assertEquals(1, UploadSqlUtils.deleteMediaUploadModelWithLocalId(testMedia2.getId()));

        List<MediaUploadModel> mediaUploadModels = WellSql.select(MediaUploadModel.class).getAsModel();
        assertEquals(1, mediaUploadModels.size());
        assertEquals(testMedia1.getId(), mediaUploadModels.get(0).getId());

        // Delete the other MediaUploadModel
        Set<Integer> mediaIdSet = new HashSet<>();
        mediaIdSet.add(testMedia1.getId());
        assertEquals(1, UploadSqlUtils.deleteMediaUploadModelsWithLocalIds(mediaIdSet));

        mediaUploadModels = WellSql.select(MediaUploadModel.class).getAsModel();
        assertEquals(0, mediaUploadModels.size());

        // The corresponding MediaModels should be untouched
        mediaModels = MediaSqlUtils.getAllSiteMedia(UploadTestUtils.getTestSite());
        assertEquals(2, mediaModels.size());
    }

    @Test
    public void testGetMediaUploadModelsForPost() {
        // Check case where there are no matching MediaUploadModels for the post
        assertEquals(0, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());

        // Set up a MediaModel with a local post ID
        long testId = Math.abs(mRandom.nextLong());
        MediaModel testMedia = UploadTestUtils.getTestMedia(testId);
        testMedia.setLocalPostId(98);
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia));

        // Store a MediaUploadModel corresponding to the MediaModel
        MediaUploadModel mediaUploadModel = new MediaUploadModel(testMedia.getId());
        mediaUploadModel.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        // Test retrieving MediaUploadModels by post id
        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());

        // Set up a second MediaModel with a different post ID
        long testId2 = Math.abs(mRandom.nextLong());
        MediaModel testMedia2 = UploadTestUtils.getTestMedia(testId2);
        testMedia2.setLocalPostId(97);
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia2));

        // Results for the first post ID should be unchanged
        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());
        // Expect empty result since we haven't created a MediaUploadModel for this yet
        assertEquals(0, UploadSqlUtils.getMediaUploadModelsForPostId(97).size());

        // Store a MediaUploadModel corresponding to the second MediaModel
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(testMedia2.getId());
        mediaUploadModel2.setProgress(0.66F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel2);

        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());
        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(97).size());

        // Set up a third MediaModel, with the same post ID as the second
        long testId3 = Math.abs(mRandom.nextLong());
        MediaModel testMedia3 = UploadTestUtils.getTestMedia(testId3);
        testMedia3.setLocalPostId(97);
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia3));

        // Store a MediaUploadModel corresponding to the third MediaModel
        MediaUploadModel mediaUploadModel3 = new MediaUploadModel(testMedia3.getId());
        mediaUploadModel3.setProgress(0.67F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel3);

        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());
        assertEquals(2, UploadSqlUtils.getMediaUploadModelsForPostId(97).size());

        // Delete two MediaModels and verify the results
        MediaSqlUtils.deleteMedia(testMedia);
        MediaSqlUtils.deleteMedia(testMedia2);

        assertEquals(0, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());
        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(97).size());
    }
}
