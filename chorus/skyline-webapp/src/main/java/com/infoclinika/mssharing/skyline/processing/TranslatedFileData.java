package com.infoclinika.mssharing.skyline.processing;

import java.util.Set;

/**
 * @author Oleksii Tymchenko
 */
public class TranslatedFileData {
    public final Set<String> ms1Refs;
    public final Set<String> ms2Refs;
    public final Set<String> simRefs;

    public TranslatedFileData(Set<String> ms1Refs, Set<String> ms2Refs, Set<String> simRefs) {
        this.ms1Refs = ms1Refs;
        this.ms2Refs = ms2Refs;
        this.simRefs = simRefs;
    }
}
