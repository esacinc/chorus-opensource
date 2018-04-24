package com.infoclinika.mssharing.model.write;


import java.util.Collection;
import java.util.Map;

public interface ProcessingFileManagement {

    void createProcessingFile(long experimentId, ProcessingFileInfo processingFileInfo);

    boolean isProcessingFileAlreadyUploadedToExperiment(long experiment, String fileName);

    void associateProcessingFileWithRawFile(Map<String, Collection<String>> map, long experimentId, long userId, String processingRunName);

    boolean isUserLabMembership(long user, long lab);





    class ProcessingFileInfo{

        public final String name;
        public final String content;

        public ProcessingFileInfo(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }
}
