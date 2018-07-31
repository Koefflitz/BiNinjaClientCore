package de.dk.bininja.client.net;

import static de.dk.bininja.net.DownloadState.CANCELLED;
import static de.dk.bininja.net.DownloadState.COMPLETE;
import static de.dk.bininja.net.DownloadState.ERROR;
import static de.dk.bininja.net.DownloadState.LOADING_FINISHED;
import static de.dk.bininja.net.DownloadState.RUNNING;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Download;
import de.dk.bininja.net.packet.download.DownloadCancelPacket;
import de.dk.bininja.net.packet.download.DownloadDataPacket;
import de.dk.bininja.net.packet.download.DownloadHeaderPacket;
import de.dk.bininja.net.packet.download.DownloadPacket;
import de.dk.bininja.net.packet.download.DownloadReadyPacket;
import de.dk.bininja.net.packet.download.DownloadRequestPacket;
import de.dk.ch.Channel;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class ClientDownload extends Download {
   private static final Logger LOGGER = LoggerFactory.getLogger(ClientDownload.class);

   private final Channel<DownloadPacket> channel;
   private final OutputStream out;
   private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

   private IOException exception;
   private String breakeUpMessage;

   public ClientDownload(Channel<DownloadPacket> channel, OutputStream out, long length) {
      this.channel = Objects.requireNonNull(channel);
      this.out = Objects.requireNonNull(out);
      channel.addListener(this);
      this.length = length;
   }

   public ClientDownload(Channel<DownloadPacket> channel, OutputStream out) {
      this(channel, out, -1);
   }

   @Override
   public void run() {
      setState(RUNNING);

      while (getDownloadState() == RUNNING || !queue.isEmpty()) {
         try {
            byte[] bytes = queue.take();
            out.write(bytes);
            written(bytes.length);
         } catch (InterruptedException e) {
            // Nothing to do here
         } catch (IOException e) {
            LOGGER.error("Error while downloading", e);
            this.exception = e;
            setState(ERROR);
            try {
               channel.send(new DownloadCancelPacket());
            } catch (IOException ex) {
               // Cannot do anything here
            }
         }
      }
      if (getDownloadState() != CANCELLED && getDownloadState() != ERROR) {
         setState(COMPLETE);
         LOGGER.info("Download complete");
      }

      try {
         out.close();
      } catch (IOException e) {
         LOGGER.warn("Could not close Outputstream of of download");
      }
   }

   @Override
   protected void request(DownloadRequestPacket packet) {
      LOGGER.warn("Download client received DownloadRequestPacket.");
   }

   @Override
   protected void header(DownloadHeaderPacket packet) {
      this.length = packet.getLength();
   }

   @Override
   protected void ready(DownloadReadyPacket packet) {
      LOGGER.warn("Download client received DownloadReadyPacket.");
   }

   @Override
   protected void data(DownloadDataPacket packet) {
      try {
         received(packet.getPayload().length);
         queue.put(packet.getPayload());
      } catch (InterruptedException e) {
         // Nothing to do here
      }
   }

   @Override
   protected void finish() {
      setState(LOADING_FINISHED);
      interrupt();
   }

   @Override
   protected void cancel(DownloadCancelPacket packet) {
      LOGGER.error("The server canceled the download: " + packet.getMsg());
      setState(ERROR);
      this.breakeUpMessage = packet.getMsg();
      interrupt();
   }

   public void cancel() {
      LOGGER.debug("Canceling the download");
      setState(CANCELLED);
      interrupt();
      try {
         channel.send(new DownloadCancelPacket());
      } catch (IOException e) {
         // Cannot do anything here
      }
   }

   public IOException getException() {
      return exception;
   }

   public String getBreakUpMessage() {
      return breakeUpMessage;
   }
}
