package com.infoclinika.mssharing.model;

import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;

import java.util.List;
import java.util.Set;

/**
 * @author timofei.kasianov 11/4/16
 */
public interface FilesImporter {

    List<CloudStorageItemReference> importFromFtp(long actor, FtpImportRequest request);

    class FtpImportRequest {
        public final String bucketPathPrefix;
        public final String url;
        public final String login;
        public final String password;
        public final Set<String> masks;
        public final boolean recursive;

        public FtpImportRequest(String bucketPathPrefix, String url, String login, String password, Set<String> masks, boolean recursive) {
            this.bucketPathPrefix = bucketPathPrefix;
            this.url = url;
            this.login = login;
            this.password = password;
            this.masks = masks;
            this.recursive = recursive;
        }
    }
}
