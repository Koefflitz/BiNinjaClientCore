package de.dk.bininja.client.ui;

import de.dk.bininja.client.model.DownloadMetadata;
import de.dk.bininja.net.DownloadListener;
import de.dk.bininja.ui.cli.CliController;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface UIController extends CliController {
   public boolean requestDownloadFrom(DownloadMetadata metadata, DownloadListener listener);
   public int activeDownloadCount();
   public String getConnectionAsString();
   public void waitForDownloads() throws InterruptedException;
}
