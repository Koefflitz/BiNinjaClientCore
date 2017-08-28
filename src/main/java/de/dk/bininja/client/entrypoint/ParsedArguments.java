package de.dk.bininja.client.entrypoint;

import java.io.File;

import de.dk.bininja.opt.ParsedSecurityArguments;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class ParsedArguments {
   private String host;
   private int port = -1;
   private String command;
   private File script;
   private boolean cli;

   private ParsedSecurityArguments secArgs;

   public ParsedArguments() {

   }

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public boolean isPortSpecified() {
      return port != -1;
   }

   public String getCommand() {
      return command;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public File getScript() {
      return script;
   }

   public void setScript(File script) {
      this.script = script;
   }

   public boolean isCli() {
      return cli || isHeadless();
   }

   public boolean isHeadless() {
      return command != null || script != null;
   }

   public void setCli(boolean cli) {
      this.cli = cli;
   }

   public ParsedSecurityArguments getSecArgs() {
      return secArgs;
   }

   public void setSecArgs(ParsedSecurityArguments secArgs) {
      this.secArgs = secArgs;
   }

   public boolean isSecure() {
      return secArgs != null;
   }

}
