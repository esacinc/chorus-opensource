package com.infoclinika.mssharing.model.internal.write;

import com.infoclinika.mssharing.model.internal.entity.ProcessingRun;
import com.infoclinika.mssharing.model.internal.entity.restorable.ActiveExperiment;
import com.infoclinika.mssharing.model.internal.repository.ExperimentRepository;
import com.infoclinika.mssharing.model.internal.repository.ProcessingRunRepository;
import com.infoclinika.mssharing.model.write.ProcessingRunManagement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;


@Service
@Transactional
public class ProcessingRunManagementImpl implements ProcessingRunManagement {


    @Inject
    private ProcessingRunRepository processingRunRepository;
    @Inject
    private ExperimentRepository experimentRepository;


    @Override
    public void create(long experiment, String name) {
        ProcessingRun processingRun = processingRunRepository.findByNameAndExperiment(name, experiment);
        if(processingRun == null){
            final ActiveExperiment activeExperiment = experimentRepository.findOne(experiment);
            processingRun = new ProcessingRun();
            processingRun.setName(name);
            processingRun.setExperimentTemplate(activeExperiment);
            processingRunRepository.save(processingRun);
        }
    }
}
