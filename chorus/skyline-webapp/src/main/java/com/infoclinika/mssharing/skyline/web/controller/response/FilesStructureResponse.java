package com.infoclinika.mssharing.skyline.web.controller.response;

import com.infoclinika.mssharing.model.read.DashboardReader;

import java.util.Set;

/**
 * @author Oleksii Tymchenko
 */
public class FilesStructureResponse {
    public final Set<DashboardReader.UploadedFile> files;

    public FilesStructureResponse(Set<DashboardReader.UploadedFile> files) {
        this.files = files;
    }
}
