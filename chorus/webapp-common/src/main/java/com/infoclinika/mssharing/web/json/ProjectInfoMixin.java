package com.infoclinika.mssharing.web.json;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Pavel Kaplin
 *
 * Motivated by http://wiki.fasterxml.com/JacksonMixInAnnotations
 */
abstract class ProjectInfoMixin {
    @JsonCreator
    public ProjectInfoMixin(@JsonProperty("name") String name,
                            @JsonProperty("areaOfResearch") String areaOfResearch,
                            @JsonProperty("description") String description) {
    }
}
