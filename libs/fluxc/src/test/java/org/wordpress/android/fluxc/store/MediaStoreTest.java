package org.wordpress.android.fluxc.store;

import static junit.framework.Assert.*;
import static org.wordpress.android.fluxc.media.MediaTestUtils.generateMediaFromPath;
import static org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpapi.media.WooMediaNetwork;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.MimeType;

import java.io.File;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MediaStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final RemoteMediaCache mRemoteMediaCache = new RemoteMediaCache();
    private final MediaCacheOperations mMediaCacheOperations = new MediaCacheOperations(mRemoteMediaCache);
    private final WooMediaNetwork mWooMediaNetwork = Mockito.mock(WooMediaNetwork.class);

    private static class FakeMediaIdGenerator implements MediaIdGenerator {
        private int nextId = 1;
        @Override
        public @NotNull LocalId generate(@NotNull String filePath) {
            return new LocalId(nextId++);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    private final MediaStore mMediaStore = new MediaStore(new Dispatcher(),
            mWooMediaNetwork,
            mRemoteMediaCache,
            mMediaCacheOperations,
            new FakeMediaIdGenerator()
    );

    @Test
    public void testGetSiteImages() {
        final String testVideoPath = "/test/test_video.mp4";
        final String testImagePath = "/test/test_image.jpg";
        final int testSiteId = 55;
        final long testVideoId = 987;
        final long testImageId = 654;

        // insert media of different types
        MediaModel videoMedia = generateMediaFromPath(testSiteId, testVideoId, testVideoPath);
        assertTrue(MediaUtils.isVideoMimeType(videoMedia.getMimeType()));
        MediaModel imageMedia = generateMediaFromPath(testSiteId, testImageId, testImagePath);
        assertTrue(MediaUtils.isImageMimeType(imageMedia.getMimeType()));
        mRemoteMediaCache.addOrUpdate(testSiteId, videoMedia);
        mRemoteMediaCache.addOrUpdate(testSiteId, imageMedia);

        final List<MediaModel> storeImages = mMediaStore.getSiteImages(getTestSiteWithLocalId(testSiteId));
        assertNotNull(storeImages);
        assertEquals(1, storeImages.size());
        assertEquals(testImageId, storeImages.get(0).getMediaId());
        assertTrue(MediaUtils.isImageMimeType(storeImages.get(0).getMimeType()));
    }

    @Test
    public void testSearchSiteImages() {
        final String testImagePath = "/test/test_image.jpg";
        final String testVideoPath = "/test/test_video.mp4";
        final String testAudioPath = "/test/test_audio.mp3";

        final int testSiteId = 55;
        final long testImageId = 654;
        final long testVideoId = 987;
        final long testAudioId = 540;

        // generate media of different types
        MediaModel imageMedia = generateMediaFromPath(testSiteId, testImageId, testImagePath,
                "Awesome Image", "This is an image test", null);
        assertTrue(MediaUtils.isImageMimeType(imageMedia.getMimeType()));

        MediaModel videoMedia = generateMediaFromPath(testSiteId, testVideoId, testVideoPath,
                "Video Title", null, "Test Caption");
        assertTrue(MediaUtils.isVideoMimeType(videoMedia.getMimeType()));

        MediaModel audioMedia = generateMediaFromPath(testSiteId, testAudioId, testAudioPath,
                null, "This is an audio test", null);
        assertTrue(MediaUtils.isAudioMimeType(audioMedia.getMimeType()));

        // insert media of different types
        mRemoteMediaCache.addOrUpdate(testSiteId, videoMedia);
        mRemoteMediaCache.addOrUpdate(testSiteId, imageMedia);
        mRemoteMediaCache.addOrUpdate(testSiteId, audioMedia);

        // verify the correct media is returned
        final List<MediaModel> storeImages = mMediaStore
                .searchSiteImages(getTestSiteWithLocalId(testSiteId), "test");

        assertNotNull(storeImages);
        assertEquals(1, storeImages.size());
        assertEquals(testImageId, storeImages.get(0).getMediaId());
        assertTrue(MediaUtils.isImageMimeType(storeImages.get(0).getMimeType()));
        assertEquals(testSiteId, storeImages.get(0).getLocalSiteId());
    }

    @Test
    public void testSearchSiteVideos() {
        final String testVideoPath1 = "/test/video_1.mp4";
        final String testVideoPath2 = "/test/video_2.mp4";
        final String testDocumentPath = "/test/test_document.pdf";

        final int testSiteId = 423;
        final long testVideoId1 = 675;
        final long testVideoId2 = 1432;
        final long testDocumentId = 125;

        // generate media of different types
        MediaModel videoMedia1 = generateMediaFromPath(testSiteId, testVideoId1, testVideoPath1,
                "My trip title", null, null);
        assertTrue(MediaUtils.isVideoMimeType(videoMedia1.getMimeType()));

        MediaModel videoMedia2 = generateMediaFromPath(testSiteId, testVideoId2, testVideoPath2,
                "Test video title", null, null);
        assertTrue(MediaUtils.isVideoMimeType(videoMedia2.getMimeType()));

        MediaModel documentMedia = generateMediaFromPath(testSiteId, testDocumentId, testDocumentPath,
                "My first test", null, null);
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia.getMimeType()));

        // insert media of different types
        mRemoteMediaCache.addOrUpdate(testSiteId, videoMedia1);
        mRemoteMediaCache.addOrUpdate(testSiteId, videoMedia2);
        mRemoteMediaCache.addOrUpdate(testSiteId, documentMedia);

        // verify the correct media is returned
        final List<MediaModel> storeVideos = mMediaStore
                .searchSiteVideos(getTestSiteWithLocalId(testSiteId), "test");
        assertNotNull(storeVideos);
        assertEquals(1, storeVideos.size());
        assertEquals(testVideoId2, storeVideos.get(0).getMediaId());
        assertTrue(MediaUtils.isVideoMimeType(storeVideos.get(0).getMimeType()));
        assertEquals(testSiteId, storeVideos.get(0).getLocalSiteId());
    }

    @Test
    public void testSearchSiteAudio() {
        final String testImagePath = "/test/test_image.jpg";
        final String testAudioPath1 = "/test/my_audio.mp3";
        final String testAudioPath2 = "/test/awesome_2018.mp3";
        final String testDocumentPath = "/test/test_document.pdf";

        final int testSiteId = 8765;
        final long testImageId = 34;
        final long testAudioId1 = 100;
        final long testAudioId2 = 99;
        final long testDocumentId = 43;

        // generate media of different types
        MediaModel imageMedia = generateMediaFromPath(testSiteId, testImageId, testImagePath,
                "Title test", null, null);
        assertTrue(MediaUtils.isImageMimeType(imageMedia.getMimeType()));

        MediaModel audioMedia1 = generateMediaFromPath(testSiteId, testAudioId1, testAudioPath1,
                "The big one", "Test for the World", null);
        assertTrue(MediaUtils.isAudioMimeType(audioMedia1.getMimeType()));

        MediaModel audioMedia2 = generateMediaFromPath(testSiteId, testAudioId2, testAudioPath2,
                "The test!", "Without description", null);
        assertTrue(MediaUtils.isAudioMimeType(audioMedia2.getMimeType()));

        MediaModel documentMedia = generateMediaFromPath(testSiteId, testDocumentId, testDocumentPath,
                "Document with every test of the app", null, null);
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia.getMimeType()));

        // insert media of different types
        mRemoteMediaCache.addOrUpdate(testSiteId, imageMedia);
        mRemoteMediaCache.addOrUpdate(testSiteId, audioMedia1);
        mRemoteMediaCache.addOrUpdate(testSiteId, audioMedia2);
        mRemoteMediaCache.addOrUpdate(testSiteId, documentMedia);

        // verify the correct media is returned (just audio)
        final List<MediaModel> storeAudio = mMediaStore
                .searchSiteAudio(getTestSiteWithLocalId(testSiteId), "test");
        assertNotNull(storeAudio);
        assertEquals(2, storeAudio.size());
        assertEquals(testAudioId1, storeAudio.get(0).getMediaId());
        assertEquals(testAudioId2, storeAudio.get(1).getMediaId());

        assertTrue(MediaUtils.isAudioMimeType(storeAudio.get(0).getMimeType()));
        assertTrue(MediaUtils.isAudioMimeType(storeAudio.get(1).getMimeType()));

        assertEquals(testSiteId, storeAudio.get(0).getLocalSiteId());
        assertEquals(testSiteId, storeAudio.get(1).getLocalSiteId());
    }

    @Test
    public void testSearchSiteDocuments() {
        final String testAudioPath = "/test/test_audio.mp3";
        final String testDocumentPath1 = "/test/document.pdf";
        final String testDocumentPath2 = "/test/document.doc";
        final String testDocumentPath3 = "/test/document.xls";
        final String testDocumentPath4 = "/test/document.pps";

        final int testSiteId = 865234;
        final long testAudioId = 78;
        final long testDocumentId1 = 234;
        final long testDocumentId2 = 657;
        final long testDocumentId3 = 98;
        final long testDocumentId4 = 543;

        // generate media of different types
        MediaModel audioMedia = generateMediaFromPath(testSiteId, testAudioId, testAudioPath,
                "My first test", "This is a description test", "Caption test");
        assertTrue(MediaUtils.isAudioMimeType(audioMedia.getMimeType()));

        MediaModel documentMedia1 = generateMediaFromPath(testSiteId, testDocumentId1, testDocumentPath1,
                "The Document", "short description", null);
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia1.getMimeType()));

        MediaModel documentMedia2 = generateMediaFromPath(testSiteId, testDocumentId2, testDocumentPath2,
                "Document to Test", "medium description", null);
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia2.getMimeType()));

        MediaModel documentMedia3 = generateMediaFromPath(testSiteId, testDocumentId3, testDocumentPath3,
                "Document", "Large description with a test", null);
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia3.getMimeType()));

        MediaModel documentMedia4 = generateMediaFromPath(testSiteId, testDocumentId4, testDocumentPath4,
                "Document Title", "description", null);
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia4.getMimeType()));

        // insert media of different types
        mRemoteMediaCache.addOrUpdate(testSiteId, audioMedia);
        mRemoteMediaCache.addOrUpdate(testSiteId, documentMedia1);
        mRemoteMediaCache.addOrUpdate(testSiteId, documentMedia2);
        mRemoteMediaCache.addOrUpdate(testSiteId, documentMedia3);
        mRemoteMediaCache.addOrUpdate(testSiteId, documentMedia4);

        // verify the correct media is returned (just documents)
        final List<MediaModel> storeDocuments = mMediaStore
                .searchSiteDocuments(getTestSiteWithLocalId(testSiteId), "test");
        assertNotNull(storeDocuments);
        assertEquals(2, storeDocuments.size());
        assertEquals(testDocumentId2, storeDocuments.get(0).getMediaId());
        assertEquals(testDocumentId3, storeDocuments.get(1).getMediaId());

        assertTrue(MediaUtils.isApplicationMimeType(storeDocuments.get(0).getMimeType()));
        assertTrue(MediaUtils.isApplicationMimeType(storeDocuments.get(1).getMimeType()));

        assertEquals(testSiteId, storeDocuments.get(0).getLocalSiteId());
        assertEquals(testSiteId, storeDocuments.get(1).getLocalSiteId());
    }

    @Test
    public void givenUploadAction_whenHandled_thenDelegateToWooMediaNetwork() throws Exception {
        SiteModel site = new SiteModel();
        site.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        File file = temporaryFolder.newFile("test-image.jpg");
        MediaModel media = generateMediaFromPath(1, 10L, file.getPath());

        mMediaStore.onAction(new Action<>(
                MediaAction.UPLOAD_MEDIA,
                new MediaStore.UploadMediaPayload(site, media, false)
        ));

        Mockito.verify(mWooMediaNetwork).uploadMedia(site, media);
    }

    @Test
    public void givenFetchMediaListAction_whenHandled_thenDelegateToWooMediaNetwork() {
        SiteModel site = getTestSiteWithLocalId(5);
        site.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        mMediaStore.onAction(new Action<>(
                MediaAction.FETCH_MEDIA_LIST,
                new MediaStore.FetchMediaListPayload(site, 10, false, MimeType.Type.IMAGE)
        ));

        Mockito.verify(mWooMediaNetwork).fetchMediaList(site, 10, 0, MimeType.Type.IMAGE);
    }

    @Test
    public void givenCancelUploadAction_whenHandled_thenDelegateToWooMediaNetwork() {
        SiteModel site = getTestSiteWithLocalId(5);
        site.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        MediaModel media = generateMediaFromPath(5, 11L, "/test/test_image.jpg");
        mMediaStore.onAction(new Action<>(
                MediaAction.CANCEL_MEDIA_UPLOAD,
                new MediaStore.CancelMediaPayload(site, media, false)
        ));

        Mockito.verify(mWooMediaNetwork).cancelUpload(site, media);
    }

    private SiteModel getTestSiteWithLocalId(int localSiteId) {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(localSiteId);
        return siteModel;
    }
}
