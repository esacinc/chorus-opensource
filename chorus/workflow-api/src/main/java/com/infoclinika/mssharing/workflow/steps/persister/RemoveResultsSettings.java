package com.infoclinika.mssharing.workflow.steps.persister;

/**
 * Represent settings how processing run workflow step results should be removed(should files on cloud should be removed, etc.)
 *
 * @author andrii.loboda
 */
public class RemoveResultsSettings {
    public final boolean removeFromCloud;

    public RemoveResultsSettings(boolean removeFromCloud) {
        this.removeFromCloud = removeFromCloud;
    }
}
