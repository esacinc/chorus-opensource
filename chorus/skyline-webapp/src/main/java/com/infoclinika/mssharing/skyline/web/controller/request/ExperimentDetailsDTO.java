package com.infoclinika.mssharing.skyline.web.controller.request;

import com.infoclinika.mssharing.model.helper.LockMzItem;
import com.infoclinika.mssharing.model.write.AnalysisBounds;
import com.infoclinika.mssharing.model.write.ExperimentInfo;
import com.infoclinika.mssharing.platform.model.read.AccessLevel;
import com.infoclinika.mssharing.platform.model.write.ExperimentManagementTemplate;
import com.infoclinika.mssharing.skyline.web.controller.response.ExperimentFileItemDTO;
import com.infoclinika.mssharing.skyline.web.controller.response.RestrictionDTO;

import java.util.Collections;
import java.util.List;

/**
 * todo: eliminate duplication with ExperimentDetails in webapp
 *
 * @author Oleksii Tymchenko
 */
public class ExperimentDetailsDTO {
    public Long id;
    public Long lab;
    public Long labHead;
    public Long billLab;
    public ExperimentInfo info;
    public long project;
    public List<ExperimentFileItemDTO> files;
    public List<ExperimentManagementTemplate.MetaFactorTemplate> factors = Collections.emptyList();
    public boolean twoDLCEnabled;
    public boolean labelsEnabled;
    public int mixedSamples;
    public RestrictionDTO restriction;
    public long type;
    public String ownerEmail;
    public AnalysisBounds bounds;
    public List<LockMzItem> lockMasses;
    public AccessLevel accessLevel;
    public int numberOfProteinSearches;
    public String labName;

    @Override
    public String toString() {
        return "ExperimentDetailsDTO{" +
                "id=" + id +
                ", lab=" + lab +
                ", labHead=" + labHead +
                ", billLab=" + billLab +
                ", info=" + info +
                ", project=" + project +
                ", files=" + files +
                ", factors=" + factors +
                ", restriction=" + restriction +
                ", type=" + type +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", bounds=" + bounds +
                ", lockMasses=" + lockMasses +
                ", accessLevel=" + accessLevel +
                ", numberOfProteinSearches=" + numberOfProteinSearches +
                ", labName='" + labName + '\'' +
                '}';
    }
}
