package de.dk.bininja.client.entrypoint;

import java.util.Objects;

import de.dk.bininja.net.Base64Connection;
import de.dk.util.opt.ArgumentParserBuilder;
import de.dk.util.opt.ExpectedOption;
import de.dk.util.opt.OptionBuilder;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public enum Option {
   HEADLESS('c', "headless", "Start the application in headless mode as a command line tool without gui."),
   HOST('H',
        "host",
        "The host to connect to. If this option is specified, the application will connect to the host at startup.",
        true),
   PORT('p',
        "port",
        "The port to connect to. If this option is not given the default port of "
        + Base64Connection.PORT + " will be used.",
        true),
   COMMAND("command",
           "command",
           "Specifiy a command to be executed. If this option is given, the application will start in headless mode.",
           true),
   SCRIPT('f',
          "script",
          "Specify a script to be executed. If this option is given, the application will start in headless mode.",
          true);

   private final char key;
   private final String longKey;
   private final String name;
   private final String description;

   private final boolean expectsValue;
   private final boolean mandatory;

   private Option(char key, String longKey, String name, String description, boolean expectsValue, boolean mandatory) {
      this.key = key;
      this.longKey = longKey;
      this.name = Objects.requireNonNull(name);
      this.description = Objects.requireNonNull(description);
      this.expectsValue = expectsValue;
      this.mandatory = mandatory;
   }

   private Option(char key, String name, String description) {
      this(key, null, name, description, false, false);
   }

   private Option(char key, String name, String description, boolean expectsValue) {
      this(key, null, name, description, expectsValue, false);
   }

   private Option(char key, String name, String description, boolean expectsValue, boolean mandatory) {
      this(key, null, name, description, expectsValue, mandatory);
   }

   private Option(String longKey, String name, String description) {
      this(ExpectedOption.NO_KEY, longKey, name, description, false, false);
   }

   private Option(String longKey, String name, String description, boolean expectsValue) {
      this(ExpectedOption.NO_KEY, longKey, name, description, expectsValue, false);
   }

   private Option(String longKey, String name, String description, boolean expectsValue, boolean mandatory) {
      this(ExpectedOption.NO_KEY, longKey, name, description, expectsValue, mandatory);
   }

   public ArgumentParserBuilder build(ArgumentParserBuilder builder) {
      OptionBuilder oBuilder;
      if (key != ExpectedOption.NO_KEY) {
         oBuilder = builder.buildOption(key, name)
                           .setLongKey(longKey);
      } else {
         oBuilder = builder.buildOption(longKey, name);
      }

      return oBuilder.setDescription(description)
                     .setMandatory(mandatory)
                     .setExpectsValue(expectsValue)
                     .build();
   }

   public char getKey() {
      return key;
   }

   public String getLongKey() {
      return longKey;
   }

}
