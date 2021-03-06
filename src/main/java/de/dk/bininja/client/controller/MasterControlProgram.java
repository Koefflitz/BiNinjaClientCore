package de.dk.bininja.client.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.util.concurrent.TimeoutException;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.client.core.Logic;
import de.dk.bininja.client.entrypoint.ParsedArguments;
import de.dk.bininja.client.model.DownloadMetadata;
import de.dk.bininja.client.net.ClientDownload;
import de.dk.bininja.client.ui.UI;
import de.dk.bininja.client.ui.UIController;
import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.net.ConnectionRefusedException;
import de.dk.bininja.net.ConnectionRequest;
import de.dk.bininja.net.ConnectionType;
import de.dk.bininja.net.DownloadListener;
import de.dk.bininja.net.SessionKeyBuilder;
import de.dk.bininja.net.packet.download.DownloadCancelPacket;
import de.dk.bininja.net.packet.download.DownloadPacket;
import de.dk.bininja.ui.cli.Cli;
import de.dk.ch.Channel;
import de.dk.ch.ChannelDeclinedException;
import de.dk.ch.Multiplexer;
import de.dk.util.net.Connection;
import de.dk.util.net.ConnectionListener;
import de.dk.util.net.security.SessionKeyArrangement;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class MasterControlProgram implements ProcessorController,
                                             UIController,
                                             ConnectionListener,
                                             SessionKeyBuilder {
   private static final Logger LOGGER = LoggerFactory.getLogger(MasterControlProgram.class);

   private static final long CONNECTION_TIMEOUT = 8000;
   private static final long CONNECTION_CLOSE_TIMEOUT = 8000;

   private Logic processor;
   private UI ui;

   private Base64Connection connection;
   private Multiplexer channelManager;

   private PublicKey publicKey;

   private boolean stopping = false;

   public MasterControlProgram() {

   }

   public void start(Logic processor, UI ui, ParsedArguments args) {
      LOGGER.debug("BiNinjaClient starting up.");

      this.processor = processor;
      this.ui = ui;

      if (args.isSecure())
         this.publicKey = args.getSecArgs().getPublicKey();

      int port = args.isPortSpecified() ? args.getPort() : Base64Connection.PORT;
      if (args.getHost() != null) {
         try {
            connect(args.getHost(), port);
         } catch (IOException | ConnectionRefusedException e) {
            String msg = "Could not connect to " + args.getHost() + ":" + port;
            LOGGER.info(msg, e);
            ui.showError(msg + "\n" + e.getMessage());
            return;
         }
      }

      if (args.getCommand() != null) {
         Cli<?> cli = (Cli<?>) ui;
         cli.enter(args.getCommand());
         exit();
      } else if (args.getScript() != null) {
         try {
            executeScript(args.getScript(), (Cli<?>) ui);
         } catch (IOException e) {
            String msg = "Error accessing script " + args.getScript().getAbsolutePath();
            LOGGER.error(msg, e);
            ui.showError(msg + "\n" + e.getMessage());
         }
         exit();
      }

      if (!args.isHeadless())
         ui.start();

      LOGGER.info("BiNinjaClient out.");
   }

   @Override
   public SecretKey buildSessionKey(SessionKeyArrangement builder) throws IOException {
      if (publicKey == null)
         return null;

      return builder.setGenerateSessionKey(true)
                    .setPublicKey(publicKey)
                    .arrange();
   }

   private void executeScript(File script, Cli<?> cli) throws IOException {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(script)));

      try {
         for (String line = in.readLine(); in != null; line = in.readLine())
            cli.enter(line);
      } finally {
         try {
            in.close();
         } catch (IOException e) {
            LOGGER.warn("Error closing " + script.getAbsolutePath(), e);
         }
      }
   }

   @Override
   public void connect(String host, int port) throws IOException, ConnectionRefusedException {
      if (connection != null && connection.isRunning()) {
         processor.cancelDownloads();
         try {
            connection.close(CONNECTION_CLOSE_TIMEOUT);
         } catch (IOException | InterruptedException e) {
            LOGGER.warn("Error closing the connection to " + connection.getAddress(), e);
         }
      }

      LOGGER.info("Establishing connection to \"" + host + "\".");
      ConnectionRequest request;
      if (publicKey == null)
         request = new ConnectionRequest(host, port);
      else
         request = new ConnectionRequest(host, port, this);

      try {
         this.connection = request.request(ConnectionType.CLIENT, CONNECTION_TIMEOUT);
      } catch (IOException | ConnectionRefusedException e) {
         LOGGER.error("Connecting to \"" + host + "\" failed", e);
         throw e;
      } catch (InterruptedException e) {
         LOGGER.info("Interrupted while establishing connection to " + host, e);
      }

      LOGGER.debug("Sending initial message, to tell the server, that I am a download client.");
      connection.addListener(this);
      this.channelManager = connection.attachMultiplexer();
      connection.start();
      LOGGER.info("Connection with " + host + " established");
      ui.setConnected(true);
      ui.show("Verbindung zu " + host + " hergestellt");
   }

   @Override
   public boolean requestDownloadFrom(DownloadMetadata metadata, DownloadListener listener) {
      Channel<DownloadPacket> downloadChannel = null;
      ClientDownload download;
      LOGGER.debug("Establishing new channel for download: " + metadata);
      try {
         downloadChannel = channelManager.establishNewChannel(DownloadPacket.class, CONNECTION_CLOSE_TIMEOUT);
         LOGGER.debug("Download channel with id " + downloadChannel.getId() + " established.");
         download = processor.requestDownloadFrom(metadata, downloadChannel);
      } catch (IOException | ChannelDeclinedException | InterruptedException | TimeoutException e) {
         String errorMsg = "Error initializing download: " + metadata;
         LOGGER.error(errorMsg, e);
         ui.showError("Downloadanfrage fehlgeschlagen.\n%s", e.getMessage());
         if (downloadChannel != null)
            close(downloadChannel);
         return false;
      }
      if (download == null) {
         try {
            downloadChannel.send(new DownloadCancelPacket());
            close(downloadChannel);
         } catch (IllegalArgumentException | IOException e) {
            LOGGER.warn("Could not cancel download " + metadata, e);
         }
         return false;
      }

//      ui.prepareDownload(metadata);
      download.addListener(listener);
      try {
         processor.startDownload(downloadChannel, download);
         return true;
      } catch (IOException e) {
         String msg = "Error starting the download " + metadata;
         LOGGER.debug(msg, e);
         ui.showError("Fehler beim Starten des Downloads vom Server");
         close(downloadChannel);
         return false;
      }
   }

   @Override
   public void waitForDownloads() throws InterruptedException {
      processor.waitForDownloads();
   }

   @Override
   public int activeDownloadCount() {
      return processor.activeDownloadCount();
   }

   @Override
   public String getConnectionAsString() {
      return connection.getAddress()
                       .toString();
   }

   @Override
   public void setDownloadTargetTo(DownloadMetadata metadata) {
      ui.setDownloadTargetTo(metadata);
   }

   private void close(Channel<?> channel) {
      try {
         channel.close();
      } catch (IOException e) {
         LOGGER.warn("Error closing the channel: " + channel, e);
      }
   }

   @Override
   public void closed(Connection connection) {
      if (stopping)
         return;

      LOGGER.debug("Connection to server " + connection.getAddress() + " closed.");
      ui.alert("Verbindung zum Server verloren.");
      ui.setConnected(false);
   }

   @Override
   public void exit() {
      if (stopping)
         return;

      stopping  = true;
      LOGGER.info("Exiting the application - freeing recources");

      processor.close();
      ui.close();

      boolean closeNecessary = connection != null
                               && connection.getSocket().isConnected()
                               && !connection.isClosed();
      if (closeNecessary) {
         try {
            LOGGER.debug("Closing connection to " + connection.getAddress());
            connection.close(CONNECTION_CLOSE_TIMEOUT);
         } catch (IOException | InterruptedException e) {
            LOGGER.warn("Error closing the connection " + connection.getAddress(), e);
         }
      }

      LOGGER.debug("Resources freed.");
      stopping = false;
   }

}