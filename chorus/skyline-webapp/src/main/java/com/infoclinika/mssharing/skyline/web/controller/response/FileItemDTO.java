package com.infoclinika.mssharing.skyline.web.controller.response;

import com.infoclinika.mssharing.model.write.StudyManagement;

import java.util.List;

/**
 * @author Oleksii Tymchenko
 */
public class FileItemDTO {
    public long id;
    public List<String> factorValues;
    public StudyManagement.Annotations annotations;
    public boolean copy;
    public int fractionNumber;

    public FileItemDTO() {
    }

    public FileItemDTO(long id, List<String> factorValues, StudyManagement.Annotations annotations, boolean copy, int fractionNumber) {
        this.id = id;
        this.factorValues = factorValues;
        this.annotations = annotations;
        this.copy = copy;
        this.fractionNumber = fractionNumber;
    }

    @Override
    public String toString() {
        return "FileItemDTO{" +
                "id=" + id +
                ", factorValues=" + factorValues +
                ", annotations=" + annotations +
                ", fractionNumber=" + fractionNumber +
                ", copy=" + copy +
                '}';
    }
}
