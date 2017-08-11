package de.dk.bininja.client.entrypoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.InvalidArgumentException;
import de.dk.bininja.client.controller.MasterControlProgram;
import de.dk.bininja.client.core.Logic;
import de.dk.bininja.client.ui.UI;
import de.dk.bininja.client.ui.UIController;
import de.dk.util.opt.ArgumentModel;
import de.dk.util.opt.ArgumentParser;
import de.dk.util.opt.ArgumentParserBuilder;
import de.dk.util.opt.ex.ArgumentParseException;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class Entrypoint {
   private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);

   private static final String GRAPHICAL_UI_CLASSNAME = "de.dk.bininja.client.ui.view.GUI";
   private static final String COMMANDLINE_UI_CLASSNAME = "de.dk.bininja.client.ui.cli.ClientCli";

   public Entrypoint() {

   }

   public static void main(String... args) {
      ParsedArguments parsedArgs;
      try {
         parsedArgs = parseArgs(args);
      } catch (ArgumentParseException e) {
         LOGGER.error("Error parsing args.", e);
         System.exit(1);
         return;
      }
      if (parsedArgs == null)
         return;

      MasterControlProgram mcp = new MasterControlProgram();
      Logic processor = new Logic(mcp);
      UI ui = null;
      String className = parsedArgs.isCli() ? COMMANDLINE_UI_CLASSNAME : GRAPHICAL_UI_CLASSNAME;
      try {
         ui = loadUI(className, mcp);
      } catch (ReflectiveOperationException e) {
         LOGGER.error("Could not load ui " + className, e);
         if (parsedArgs.isCli()) {
            System.err.println("Jar file corrupt. UI class " + className + " not found!");
            System.exit(1);
            return;
         } else {
            System.out.println("GUI class not available.");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            while (ui == null) {
               System.out.print("Continue in command line mode? (y/n) ");
               String input;
               try {
                  input = in.readLine();
               } catch (IOException ex) {
                  LOGGER.error("Could not read from stdin", e);
                  return;
               }
               if (input == null)
                  return;

               if (input.equals("n") || input.equals("no"))
                  return;

               if (input.equals("y") || input.equals("yes")) {
                  try {
                     ui = loadUI(COMMANDLINE_UI_CLASSNAME, mcp);
                  } catch (ReflectiveOperationException exeption) {
                     String msg = "Could not load Commandline UI.";
                     LOGGER.error(msg, e);
                     System.err.println(msg);
                     System.exit(1);
                     return;
                  }
               }
            }
         }
      }

      mcp.start(processor, ui, parsedArgs);
   }

   private static UI loadUI(String uiClassName, MasterControlProgram mcp) throws ReflectiveOperationException {
      LOGGER.debug("Loading ui class " + uiClassName);
      Class<?> uiClass = Thread.currentThread()
                               .getContextClassLoader()
                               .loadClass(uiClassName);

      LOGGER.debug("UI class loaded.");

      if (!UI.class.isAssignableFrom(uiClass))
         throw new IllegalArgumentException("The class " + uiClassName + " does not implement the UI interface.");

      Constructor<?> constructor = uiClass.getDeclaredConstructor(UIController.class);
      LOGGER.debug("Creating new instance of " + uiClassName);
      return (UI) constructor.newInstance(mcp);
   }

   private static ParsedArguments parseArgs(String... args) throws ArgumentParseException {
      LOGGER.debug("Parsing args");
      ArgumentParserBuilder builder = ArgumentParserBuilder.begin();
      for (Option opt : Option.values())
         opt.build(builder);

      ArgumentParser parser = builder.buildAndGet();
      ArgumentModel result;
      if (parser.isHelp(args)) {
         parser.printUsage(System.out);
         return null;
      }
      try {
         result = parser.parseArguments(args);
      } catch (ArgumentParseException e) {
         System.out.println(e.getMessage());
         parser.printUsage(System.out);
         throw e;
      }
      ParsedArguments parsedArgs = new ParsedArguments();
      result.getOptionalValue(Option.HOST.getKey())
            .ifPresent(parsedArgs::setHost);

      String portOption = result.getOptionValue(Option.PORT.getKey());
      if (portOption != null) {
         int port;
         try {
            port = Integer.parseInt(portOption);
         } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Invalid port: " + portOption);
         }
         parsedArgs.setPort(port);
      }

      parsedArgs.setCli(result.isOptionPresent(Option.HEADLESS.getKey()));
      if (result.isOptionPresent(Option.SCRIPT.getLongKey())) {
         File script = new File(result.getOptionValue(Option.SCRIPT.getLongKey()));
         parsedArgs.setScript(script);
      }
      parsedArgs.setCommand(result.getOptionValue(Option.COMMAND.getLongKey()));
      return parsedArgs;
   }
}