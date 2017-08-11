package de.dk.bininja.client.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.client.controller.ProcessorController;
import de.dk.bininja.client.model.DownloadMetadata;
import de.dk.bininja.client.net.ClientDownload;
import de.dk.bininja.client.net.DownloadRequest;
import de.dk.bininja.net.DownloadManager;
import de.dk.bininja.net.packet.download.DownloadCancelPacket;
import de.dk.bininja.net.packet.download.DownloadHeaderPacket;
import de.dk.bininja.net.packet.download.DownloadPacket;
import de.dk.bininja.net.packet.download.DownloadReadyPacket;
import de.dk.util.StringUtils;
import de.dk.util.channel.Channel;
import de.dk.util.channel.ChannelClosedException;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class Logic {
   private static final Logger LOGGER = LoggerFactory.getLogger(Logic.class);

   private final ProcessorController controller;
   private DownloadManager<ClientDownload> downloads = new DownloadManager<>();

   public Logic(ProcessorController controller) {
      this.controller = controller;
   }

   public synchronized ClientDownload requestDownloadFrom(DownloadMetadata metadata,
                                                          Channel<DownloadPacket> downloadChannel) throws IOException {
      DownloadHeaderPacket header;
      try {
         header = requestDownloadHeader(metadata.getUrl(), downloadChannel);
      } catch (IOException e) {
         cancelDownload(downloadChannel, e.getMessage());
         throw new IOException("Error requesting the download meta data", e);
      }

      LOGGER.info("Download metadata received: " + header);
      metadata.setLength(header.getLength());
      if (StringUtils.isBlank(metadata.getFileName()))
         metadata.setFileName(header.getFilename());

      LOGGER.info("Preparing the Download " + metadata);
      if (!metadata.isTargetSpecified()) {
         controller.setDownloadTargetTo(metadata);
         if (!metadata.isTargetSpecified())
            return null;
      }

      File target = new File(metadata.getTargetDirectory(), metadata.getFileName());
      LOGGER.debug("Opening OutputStream to downloadtarget: " + target.getAbsolutePath());
      FileOutputStream output;
      try {
         output = new FileOutputStream(target);
      } catch (SecurityException | FileNotFoundException e) {
         String msg = String.format("Konnte auf Downloaddatei \"%s\" nicht zugreifen.\n%s",
                                    target.getAbsolutePath(),
                                    e.getMessage());

         throw new IOException(msg, e);
      }

      LOGGER.debug("Creating a Download object for " + metadata);
      ClientDownload download;
      download = new ClientDownload(downloadChannel, output, metadata.getLength());
      downloads.add(download);
      return download;
   }

   public void waitForDownloads() throws InterruptedException {
      downloads.waitFor();
   }

   private void cancelDownload(Channel<DownloadPacket> channel, String msg) {
      try {
         channel.send(new DownloadCancelPacket(msg));
      } catch (IOException | ChannelClosedException e) {
         LOGGER.warn("Error canceling the download.", e);
      }
   }

   public DownloadHeaderPacket requestDownloadHeader(URL url,
                                                     Channel<DownloadPacket> downloadChannel) throws IOException {

      DownloadRequest request = new DownloadRequest(downloadChannel, url);
      LOGGER.info(String.format("Sending download request from \"%s\" to the server", url.toString()));

      try {
         return request.request();
      } catch (IOException e) {
         String errorMsg = String.format("Could not send download request from \"%s\".",
                                         url.toString());

         throw new IOException(errorMsg, e);
      } catch (InterruptedException e) {
         String errorMsg = "Thread was interrupted while waiting for response on download request from " + url;
         throw new IOException(errorMsg, e);
      } catch (TimeoutException e) {
         throw new IOException("Dowload request timed out.", e);
      }
   }

   public void startDownload(Channel<DownloadPacket> downloadChannel, ClientDownload download) throws IOException {
      LOGGER.info("Starting download");
      download.start();
      LOGGER.debug("Download prepared. Telling the server, that everything is ready for the download "
                   + "by sending a DownloadReadyPacket.");
      try {
         downloadChannel.send(new DownloadReadyPacket());
      } catch (IOException e) {
         download.cancel();
         throw new IOException("Could not send DownloadReadyPacket to server");
      }
      LOGGER.info("Download initiated");
   }

   public void cancelDownloads() {
      LOGGER.debug("Canceling downloads");
      for (ClientDownload download : downloads)
         download.cancel();
   }

   public int activeDownloadCount() {
      return downloads.size();
   }

   public void close() {
      cancelDownloads();
   }
}
