package com.infoclinika.mssharing.model.internal.read;

import com.infoclinika.mssharing.model.internal.entity.ProcessingFile;
import com.infoclinika.mssharing.model.internal.entity.restorable.AbstractExperiment;

import java.util.Date;
import java.util.List;

public interface ProcessingRunReader {

    boolean findProcessingRunByExperiment(String name, long experiment);

    ProcessingRunInfo readProcessingRun(long processingRunId);

    ProcessingRunInfo readProcessingRunByNameAndExperiment(long experiment, String name);




    class ProcessingRunInfo{
        public Long id;
        public String name;
        public Date date;
        public AbstractExperiment abstractExperiment;
        public List<ProcessingFile> processingFiles;

        public ProcessingRunInfo(Long id, String name, Date date, AbstractExperiment abstractExperiment) {
            this.id = id;
            this.name = name;
            this.date = date;
            this.abstractExperiment = abstractExperiment;
        }

        public ProcessingRunInfo(Long id, String name, Date date, AbstractExperiment abstractExperiment, List<ProcessingFile> processingFiles) {
            this.id = id;
            this.name = name;
            this.date = date;
            this.abstractExperiment = abstractExperiment;
            this.processingFiles = processingFiles;
        }
    }

}
