package de.dk.bininja.client.ui;

import de.dk.bininja.client.model.DownloadMetadata;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface UI {
   public void start();
   public void show(String format, Object... args);
   public void showError(String errorMsg, Object... args);
   public void alert(String format, Object... args);
   public void alertError(String errorMsg, Object... args);
   public void setConnected(boolean connected);
   public void prepareDownload(DownloadMetadata metadata) throws IllegalStateException;
   public void setDownloadTargetTo(DownloadMetadata metadata);
   public void close();
}
