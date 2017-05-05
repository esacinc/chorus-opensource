package com.infoclinika.mssharing.model.internal.entity.workflow;

import com.infoclinika.mssharing.model.internal.entity.AbstractAggregate;

import javax.persistence.*;

/**
 * @author andrii.loboda on 27.08.2014.
 * <p/>
 * Represents persisted type of workflow step. Could be used to define whether sequence of steps valid(is it enough
 * information to call it after specific steps or not)
 */
@Entity
@Table(name = "w_WorkflowStepType")
public class WorkflowStepTypeEntry extends AbstractAggregate {

    @Basic(optional = false)
    private String name;

    @Basic
    @Lob
    private String description;

    @Basic(optional = false)
    private boolean displayable;

    public WorkflowStepTypeEntry() {
    }

    public WorkflowStepTypeEntry(String name, String description, boolean displayable) {
        this.name = name;
        this.description = description;
        this.displayable = displayable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public boolean isDisplayable() {
        return displayable;
    }

    public void setDisplayable(boolean displayable) {
        this.displayable = displayable;
    }
}
