package com.infoclinika.mssharing.model.internal.entity.restorable;

import javax.persistence.*;
import java.util.Date;

@Embeddable
public class TranslationStatus {

    @Lob
    @Column(name = "translation_error")
    private String translationError;
    @Basic(optional = false)
    @Column(name = "translation_submitted")
    private boolean translationSubmitted = false;

    @Column(name = "last_translation_attempt")
    private Date lastTranslationAttempt;
    @Column(name = "last_translation_duration")
    private Long lastTranslationDuration;
    @Column(name = "last_translation_end_date")
    private Date lastTranslationEndDate;

    @Column(name = "to_translate")
    private boolean toTranslate;

    @Column(name = "to_translation_queue")
    private boolean toTranslationQueue;

    @Enumerated
    @Column(columnDefinition = "int(1) default 0")
    private Status status =  Status.NOT_STARTED;

    public TranslationStatus() {
    }

    public boolean isTranslationSubmitted() {
        return translationSubmitted;
    }

    public void setTranslationSubmitted(boolean translationSubmitted) {
        this.translationSubmitted = translationSubmitted;
    }

    public String getTranslationError() {
        return translationError;
    }

    public void setTranslationError(String translationError) {
        this.translationError = translationError;
    }

    public void setLastTranslationEndDate(Date lastTranslationEndDate) {
        this.lastTranslationEndDate = lastTranslationEndDate;
    }

    public Date getLastTranslationEndDate() {
        return lastTranslationEndDate;
    }

    public void setLastTranslationDuration(Long lastTranslationDuration) {
        this.lastTranslationDuration = lastTranslationDuration;
    }

    public Long getLastTranslationDuration() {
        return lastTranslationDuration;
    }

    public void setLastTranslationAttempt(Date lastTranslationAttempt) {
        this.lastTranslationAttempt = lastTranslationAttempt;
    }

    public Date getLastTranslationAttempt() {
        return lastTranslationAttempt;
    }

    public void setToTranslate(boolean toTranslate) {
        this.toTranslate = toTranslate;
    }

    public boolean isToTranslate() {
        return toTranslate;
    }


    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isToTranslationQueue() {
        return toTranslationQueue;
    }

    public void setToTranslationQueue(boolean toTranslationQueue) {
        this.toTranslationQueue = toTranslationQueue;
    }

    public enum Status {
        NOT_STARTED, IN_PROGRESS, FAILURE, SUCCESS
    }

}