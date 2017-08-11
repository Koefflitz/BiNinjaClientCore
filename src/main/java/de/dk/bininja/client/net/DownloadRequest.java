package de.dk.bininja.client.net;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.packet.download.DownloadHeaderPacket;
import de.dk.bininja.net.packet.download.DownloadPacket;
import de.dk.bininja.net.packet.download.DownloadRequestPacket;
import de.dk.util.channel.Channel;
import de.dk.util.channel.ChannelListener;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class DownloadRequest implements ChannelListener<DownloadPacket> {
   private static final Logger LOGGER = LoggerFactory.getLogger(DownloadRequest.class);

   private static final long DEFAULT_TIMEOUT = 12000;

   private Channel<DownloadPacket> channel;
   private URL url;

   private DownloadHeaderPacket header;

   public DownloadRequest(Channel<DownloadPacket> downloadChannel, URL url) {
      this.channel = downloadChannel;
      this.url = url;
      channel.addListener(this);
   }

   public DownloadHeaderPacket request() throws IOException, InterruptedException, TimeoutException {
      return request(DEFAULT_TIMEOUT);
   }

   public synchronized DownloadHeaderPacket request(long timeout) throws IOException, InterruptedException, TimeoutException {
      try {
         channel.send(new DownloadRequestPacket(url));
         wait(timeout);
         if (header == null)
            throw new TimeoutException();

         return header;
      } finally {
         channel.removeListener(this);
      }
   }

   @Override
   public synchronized void received(DownloadPacket packet) {
      if (!(packet instanceof DownloadHeaderPacket)) {
         LOGGER.warn("DownloadRequest received a DownloadPacket, that was not the header: " + packet);
         return;
      }

      this.header = (DownloadHeaderPacket) packet;
      notify();
   }

   protected URL getUrl() {
      return url;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.channel == null) ? 0 : this.channel.hashCode());
      result = prime * result + ((this.url == null) ? 0 : this.url.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      DownloadRequest other = (DownloadRequest) obj;
      if (this.channel == null) {
         if (other.channel != null)
            return false;
      } else if (!this.channel.equals(other.channel))
         return false;
      if (this.url == null) {
         if (other.url != null)
            return false;
      } else if (!this.url.equals(other.url))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "DownloadRequest { url=" + url + ", channel=" + channel + " }";
   }
}
