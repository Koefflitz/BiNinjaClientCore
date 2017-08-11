package de.dk.bininja.client.model;

import java.io.File;
import java.net.URL;

import de.dk.util.FileUtils;
import de.dk.util.StringUtils;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class DownloadMetadata {
   private static int idCounter = 0;

   private final int id;

   private File targetDirectory;
   private String fileName;
   private URL url;
   private long length = -1;

   public DownloadMetadata(URL url) {
      this.id = idCounter++;
      this.url = url;
   }

   private static Object objectOrNullString(Object o) {
      return o == null ? "not set" : o;
   }

   public int getId() {
      return id;
   }

   public File getTargetDirectory() {
      return targetDirectory;
   }

   public void setTargetDirectory(File targetDirectory) {
      this.targetDirectory = targetDirectory;
   }

   public String getFileName() {
      return fileName;
   }

   public void setFileName(String fileName) {
      this.fileName = fileName;
   }

   public boolean isTargetSpecified() {
      return !FileUtils.isBlank(targetDirectory) && !StringUtils.isBlank(fileName);
   }

   public URL getUrl() {
      return url;
   }

   public void setUrl(URL url) {
      this.url = url;
   }

   public long getLength() {
      return length;
   }

   public void setLength(long length) {
      this.length = length;
   }

   @Override
   public String toString() {
      return "Download {\n"
             + "\tid = " + id + "\n"
             + "\turl = " + objectOrNullString(url) + "\n"
             + "\tlength = " + (length == -1 ? "not set" : length) + "\n"
             + "\ttargetDirectory = " + objectOrNullString(targetDirectory) + "\n"
             + "\tfileName = " + objectOrNullString(fileName) + "\n"
             + "}";
   }

}
