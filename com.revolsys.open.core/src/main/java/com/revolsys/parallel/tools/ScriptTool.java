package com.revolsys.parallel.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.context.HashMapContext;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.MethodInvocationException;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.PropertyBatchUpdateException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

import com.revolsys.beans.ResourceEditorRegistrar;
import com.revolsys.collection.map.ThreadSharedAttributes;
import com.revolsys.logging.log4j.ThreadLocalFileAppender;
import com.revolsys.parallel.process.ProcessNetwork;
import com.revolsys.util.JexlUtil;
import com.revolsys.util.ManifestUtil;

public class ScriptTool {
  private static final Logger LOG = LoggerFactory.getLogger(ScriptTool.class);

  private static final String LOG_FILE = "logFile";

  private static final String LOG_FILE_OPTION = "l";

  private static final String PROPERTIES = "properties";

  private static final String PROPERTIES_OPTION = "p";

  private static final String SCRIPT = "script";

  private static final String SCRIPT_OPTION = "s";

  private static final String VERSION = "version";

  private static final String VERSION_OPTION = "v";

  private static Throwable getBeanExceptionCause(final BeanCreationException e) {
    Throwable cause = e.getCause();
    if (cause == null) {
      return e;
    }
    while (cause instanceof BeanCreationException
      || cause instanceof MethodInvocationException
      || cause instanceof PropertyAccessException
      || cause instanceof PropertyBatchUpdateException
      || cause instanceof InvalidPropertyException) {
      Throwable newCause;
      if (cause instanceof PropertyBatchUpdateException) {
        final PropertyBatchUpdateException batchEx = (PropertyBatchUpdateException)cause;
        newCause = batchEx.getPropertyAccessExceptions()[0];
      } else {
        newCause = cause.getCause();
      }
      if (newCause != null) {
        cause = newCause;
      } else {
        return cause;
      }
    }
    return cause;
  }

  /**
   * @param args
   */
  public static void main(final String[] args) {
    final ScriptTool app = new ScriptTool();
    app.start(args);
  }

  private CommandLine commandLine;

  private boolean displayVersion;

  private File logFile;

  private final Options options = new Options();

  private final Map<String, String> parameters = new LinkedHashMap<String, String>();

  private String propertiesName;

  private String scriptFileName;

  private File scriptFile;

  public ScriptTool() {
    createOptions();
  }

  private void createOptions() {
    final Option script = new Option(SCRIPT_OPTION, SCRIPT, true,
      "the script file that defines the processor pipeline");
    script.setRequired(false);
    options.addOption(script);

    final Option logFile = new Option(LOG_FILE_OPTION, LOG_FILE, true,
      "The file to write log messages to");
    logFile.setRequired(false);
    options.addOption(logFile);

    final Option properties = new Option(PROPERTIES_OPTION, PROPERTIES, true,
      "The file to load properties from");
    properties.setRequired(false);
    options.addOption(properties);

    final Option version = new Option(VERSION_OPTION, VERSION, false,
      "Display the version number");
    properties.setRequired(false);
    options.addOption(version);

    OptionBuilder.withDescription("use value for given property");
    OptionBuilder.withArgName("property=value");
    OptionBuilder.withValueSeparator();
    OptionBuilder.hasArgs(2);
    final Option property = OptionBuilder.create("D");

    options.addOption(property);
  }

  private void displayVersion() {
    displayVersion = true;
    final String implementationTitle = System.getProperty("script.implementationTitle");
    if (implementationTitle != null) {
      final String build = ManifestUtil.getMainAttributeByImplementationTitle(
        implementationTitle, "SCM-Revision");
      if (build != null) {
        System.out.println(implementationTitle + " (build " + build + ")");
      } else {
        System.out.println(implementationTitle);
      }
      System.out.println();
    }
  }

  private boolean loadProperties(final String name) {
    final Class<? extends ScriptTool> clazz = getClass();
    final ClassLoader classLoader = clazz.getClassLoader();
    final InputStream scriptIn = classLoader.getResourceAsStream(name);
    return loadProperties(name, scriptIn);
  }

  private boolean loadProperties(final String name, final InputStream in) {
    if (in != null) {
      try {
        final Properties props = new Properties();
        props.load(in);
        for (final Entry<Object, Object> parameter : props.entrySet()) {
          final String key = (String)parameter.getKey();
          final String value = (String)parameter.getValue();
          ThreadSharedAttributes.setAttribute(key, value);
          System.setProperty(key, value);
        }
        return true;
      } catch (final Exception e) {
        System.err.println("Unable to load properties from " + name);
        e.printStackTrace();
        return false;
      }
    } else {
      return false;
    }
  }

  public boolean processArguments(final String[] args) {
    try {
      loadProperties("script.properties");
      final CommandLineParser parser = new PosixParser();
      commandLine = parser.parse(options, args);
      final Option[] options = commandLine.getOptions();
      for (int i = 0; i < options.length; i++) {
        final Option option = options[i];
        final String shortOpt = option.getOpt();
        if (shortOpt != null && shortOpt.equals("D")) {
          final Properties properties = commandLine.getOptionProperties("D");
          for (final Entry<Object, Object> property : properties.entrySet()) {
            final String key = (String)property.getKey();
            final String value = (String)property.getValue();
            parameters.put(key, value);
            System.setProperty(key, value);
            ThreadSharedAttributes.setAttribute(key, value);
          }
        }

      }
      if (commandLine.hasOption(SCRIPT_OPTION)) {
        if (!setScriptFileName(commandLine.getOptionValue(SCRIPT_OPTION))) {
          return false;
        }
      }
      if (commandLine.hasOption(PROPERTIES_OPTION)) {
        propertiesName = commandLine.getOptionValue(PROPERTIES_OPTION);
        try {
          final File propertiesFile = new File(propertiesName);
          if (propertiesFile.exists()) {
            final InputStream in = new FileInputStream(propertiesFile);
            loadProperties(propertiesName, in);
          } else {
            if (!loadProperties(propertiesName)) {
              System.err.println("Properties file '" + propertiesName
                + "' does not exist");
              return false;
            }
          }

        } catch (final IOException e) {
          System.err.println("Properties file '" + propertiesName
            + "' could not be read:" + e.getMessage());
          return false;
        }
      }

      if (commandLine.hasOption(LOG_FILE_OPTION)) {
        logFile = new File(commandLine.getOptionValue(LOG_FILE_OPTION));
        final File logDirectory = logFile.getParentFile();
        if (!logDirectory.exists()) {
          if (!logDirectory.mkdirs()) {
            System.err.println("Unable to create Log directory '"
              + logDirectory.getAbsolutePath() + "'");
            return false;
          }
        }
      } else {
        String logFileName = System.getProperty("logFile");
        if (logFileName != null) {
          try {
            while (logFileName.contains("${")) {
              final Expression expression = JexlUtil.createExpression(logFileName);
              final HashMapContext context = new HashMapContext();
              context.setVars(ThreadSharedAttributes.getAttributes());
              logFileName = (String)JexlUtil.evaluateExpression(context,
                expression);
            }
          } catch (final Exception e) {
            e.printStackTrace();
            logFileName = null;
          }
        }
      }
      if (logFile != null) {
        if (logFile.exists() && !logFile.isFile()) {
          System.err.println("Log file '" + logFile.getAbsolutePath()
            + "' is not a file");
          return false;
        }
        System.setProperty("logFile", logFile.getAbsolutePath());
      }
      if (commandLine.hasOption(VERSION_OPTION)) {
        displayVersion();
        return false;
      }
      if (scriptFileName == null) {
        final String[] extraArgs = commandLine.getArgs();
        if (extraArgs.length > 0) {
          if (!setScriptFileName(extraArgs[0])) {
            return false;
          }
        }
      }
      return true;
    } catch (final MissingOptionException e) {
      if (commandLine.hasOption(VERSION_OPTION)) {
        displayVersion();
      } else {
        System.err.println("Missing " + e.getMessage() + " argument");
      }
      return false;
    } catch (final ParseException e) {
      System.err.println("Unable to process command line arguments: "
        + e.getMessage());
      return false;
    }
  }

  private void run() {
    final long startTime = System.currentTimeMillis();

    final ThreadLocalFileAppender localAppender = ThreadLocalFileAppender.getAppender();
    if (localAppender != null && logFile != null) {
      final File parentFile = logFile.getParentFile();
      if (parentFile != null) {
        parentFile.mkdirs();
      }
      localAppender.setLocalFile(logFile.getAbsolutePath());
    } else if (logFile != null) {

      final org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
      try {
        final Layout layout = new PatternLayout("%d\t%p\t%m%n");
        final Appender appender = new FileAppender(layout,
          logFile.getAbsolutePath(), false);
        rootLogger.addAppender(appender);
      } catch (final IOException e) {
        final Layout layout = new PatternLayout("%p\t%m%n");
        final Appender appender = new ConsoleAppender(layout);
        rootLogger.addAppender(appender);
        LOG.error("Cannot find log file " + logFile, e);
      }

    }

    final StringBuffer message = new StringBuffer("Processing ");
    message.append(" -s ");
    message.append(scriptFileName);
    if (propertiesName != null) {
      message.append(" -p ");
      message.append(propertiesName);
    }
    for (final Entry<String, String> parameter : parameters.entrySet()) {
      message.append(" ");
      message.append(parameter.getKey());
      message.append("=");
      message.append(parameter.getValue());
    }
    System.out.println(message);

    if (System.getProperty("applicationHome") == null) {
      ThreadSharedAttributes.setAttribute("applicationHome", ".");
      System.setProperty("applicationHome", ".");
    }
    try {
      final GenericApplicationContext beans = new GenericApplicationContext();
      AnnotationConfigUtils.registerAnnotationConfigProcessors(beans, null);
      beans.getBeanFactory().addPropertyEditorRegistrar(
        new ResourceEditorRegistrar());

      if (scriptFile != null) {
        new XmlBeanDefinitionReader(beans).loadBeanDefinitions("file:"
          + scriptFile.getAbsolutePath());
      } else {
        new XmlBeanDefinitionReader(beans).loadBeanDefinitions("classpath:"
          + scriptFileName);
      }
      beans.refresh();
      try {
        LOG.info(message.toString());
        final Object bean = beans.getBean("processNetwork");
        final ProcessNetwork pipeline = (ProcessNetwork)bean;
        pipeline.startAndWait();
      } finally {
        beans.close();
      }
    } catch (final BeanCreationException e) {
      final Throwable cause = getBeanExceptionCause(e);
      LOG.error(cause.getMessage(), cause);
      cause.printStackTrace();
      System.err.flush();
    }
    final long endTime = System.currentTimeMillis();
    final long time = endTime - startTime;
    long seconds = time / 1000;
    final long minutes = seconds / 60;
    seconds = seconds % 60;
    LOG.info(minutes + " minutes " + seconds + " seconds");
    System.out.println(minutes + " minutes " + seconds + " seconds");

  }

  private boolean setScriptFileName(final String scriptFileName) {
    if (new File(scriptFileName).exists()) {
      this.scriptFileName = scriptFileName;
      scriptFile = new File(scriptFileName);
      return true;
    } else if (getClass().getClassLoader().getResource(scriptFileName) == null) {
      System.err.println("The script '" + scriptFileName + "' does not exist");
      return false;
    } else {
      this.scriptFileName = scriptFileName;
      return true;
    }
  }

  public void start(final String[] args) {
    if (processArguments(args)) {
      run();
    } else {
      if (!displayVersion) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("scriptTool", options);
      }
    }

  }
}
