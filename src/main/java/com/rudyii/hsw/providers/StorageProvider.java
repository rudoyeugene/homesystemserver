package com.rudyii.hsw.providers;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import com.rudyii.hsw.services.UuidService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StorageProvider {
    private final Bucket bucket;

    @Autowired
    public StorageProvider(UuidService uuidService) throws IOException {
        Storage defaultStorage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(this.getClass().getResourceAsStream("/server-global.json")))
                .setProjectId("complete-home-system")
                .build().getService();
        if (defaultStorage.get(uuidService.getServerKey(), Storage.BucketGetOption.fields()) == null) {
            this.bucket = defaultStorage.create(
                    BucketInfo.newBuilder(uuidService.getServerKey())
                            .setStorageClass(StorageClass.STANDARD)
                            .setLocation("EUROPE-WEST3")
                            .setLifecycleRules(ImmutableList.of(
                                    new BucketInfo.LifecycleRule(
                                            BucketInfo.LifecycleRule.LifecycleAction.newDeleteAction(),
                                            BucketInfo.LifecycleRule.LifecycleCondition.newBuilder().setAge(30).build())))
                            .build());

        } else {
            this.bucket = defaultStorage.get(uuidService.getServerKey());
        }
    }

    public URL putData(String objectName, MediaType mediaType, byte[] data) {
        Blob blob = bucket.create(objectName, data, mediaType.type());
        log.info("Uploaded {}", blob.getBlobId());
        return blob.signUrl(30, TimeUnit.DAYS);
    }

    public URL putData(String objectName, MediaType mediaType, InputStream data) {
        Blob blob = bucket.create(objectName, data, mediaType.type());
        log.info("Uploaded {}", blob.getBlobId());
        return blob.signUrl(30, TimeUnit.DAYS);
    }

    public void deleteData(String objectName) {
        Blob blob = bucket.get(objectName + ".mp4");
        deleteBlob(blob);
        blob = bucket.get(objectName + ".jpg");
        deleteBlob(blob);
    }

    private void deleteBlob(Blob blob) {
        if (blob != null) {
            blob.delete();
            log.info("Deleted {}", blob.getBlobId());
        }
    }
}
