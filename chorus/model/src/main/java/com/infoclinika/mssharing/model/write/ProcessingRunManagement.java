package com.infoclinika.mssharing.model.write;

import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Transactional
public interface ProcessingRunManagement {


    long create(long experiment, String name);
}
