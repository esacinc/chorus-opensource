package com.infoclinika.mssharing.web.controller.v2.dto;

import com.infoclinika.mssharing.model.read.dto.details.ProcessingRunItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class ProcessingRunDetails implements Serializable {

   private long id;
   private String name;
   private String processedDate;
   private List<ProcessingFileDTO> processedFiles = newArrayList();

   public ProcessingRunDetails(long id, String name, String processedDate, List<ProcessingFileDTO> processedFiles) {
      this.id = id;
      this.name = name;
      this.processedDate = processedDate;
      this.processedFiles.addAll(processedFiles);
   }

   /**
    * Getter for property 'id'.
    *
    * @return Value for property 'id'.
    */
   public long getId() {
      return id;
   }

   /**
    * Setter for property 'id'.
    *
    * @param id Value to set for property 'id'.
    */
   public void setId(long id) {
      this.id = id;
   }

   /**
    * Getter for property 'name'.
    *
    * @return Value for property 'name'.
    */
   public String getName() {
      return name;
   }

   /**
    * Setter for property 'name'.
    *
    * @param name Value to set for property 'name'.
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * Getter for property 'processedDate'.
    *
    * @return Value for property 'processedDate'.
    */
   public String getProcessedDate() {
      return processedDate;
   }

   /**
    * Setter for property 'processedDate'.
    *
    * @param processedDate Value to set for property 'processedDate'.
    */
   public void setProcessedDate(String processedDate) {
      this.processedDate = processedDate;
   }

   /**
    * Getter for property 'processedFiles'.
    *
    * @return Value for property 'processedFiles'.
    */
   public List<ProcessingFileDTO> getProcessedFiles() {
      return processedFiles;
   }

   /**
    * Setter for property 'processedFiles'.
    *
    * @param processedFiles Value to set for property 'processedFiles'.
    */
   public void setProcessedFiles(List<ProcessingFileDTO> processedFiles) {
      this.processedFiles = processedFiles;
   }

   @Override
   public String toString() {
      return "ProcessingRunDetails{" +
              "id=" + id +
              ", name='" + name + '\'' +
              ", processedDate='" + processedDate + '\'' +
              ", processedFiles=" + processedFiles +
              '}';
   }
}
