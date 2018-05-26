package me.ialistannen.jvmagentutils.instrumentation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

class AgentRunner {

  private static final Logger LOGGER = Logger.getLogger("AgentRunner");

  /**
   * Runs an agent and tried to also work around the Java 9 "allowAttachSelf" limitation.
   *
   * @param agentJar the path to the agent jar
   * @param pid the pid of the JVM to attach to
   * @param arguments the arguments for the agent
   */
  static void run(Path agentJar, int pid, String arguments)
      throws IOException {

    LOGGER.info(attachPrefix("Attaching via an external runner..."));
    startNewJVMAndAttach(agentJar, pid, arguments);
  }

  private static void startNewJVMAndAttach(Path agentJar, int pid, String arguments)
      throws IOException {
    Path tempFile = Files.createTempFile("TNT-Spawn_external_attacher", ".jar");
    tempFile.toFile().deleteOnExit();

    JvmUtils.writeClassesToJar(
        tempFile,
        generateExternalAttacherManifest()
    );

    ProcessBuilder processBuilder = new ProcessBuilder(
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
        "-jar",
        tempFile.toAbsolutePath().toString(),
        Integer.toString(pid),
        agentJar.toAbsolutePath().toString(),
        arguments
    );

    LOGGER.fine(attachPrefix("Running command '%s'", processBuilder.command()));

    Process process = processBuilder.start();

    new Thread(() -> {
      try {
        String error = new String(
            IOUtils.getAllBytes(process.getErrorStream()), StandardCharsets.UTF_8
        );
        String output = new String(
            IOUtils.getAllBytes(process.getInputStream()), StandardCharsets.UTF_8
        );

        process.waitFor();

        if (process.exitValue() != 0) {
          LOGGER.warning(attachPrefix("Attach process ended with a non-zero exit code."));
          LOGGER.warning(attachPrefix("The output stream was '%s'", output));
          LOGGER.warning(attachPrefix("The error stream was '%s'", error));
        }

      } catch (RuntimeException | InterruptedException e) {
        LOGGER.log(Level.WARNING, attachPrefix("Could not start the external agent"), e);
      }
    }).start();
  }

  private static Manifest generateExternalAttacherManifest() {
    Manifest manifest = new Manifest();
    Attributes mainAttributes = manifest.getMainAttributes();
    mainAttributes.put(Name.MANIFEST_VERSION, "1.0");
    mainAttributes.put(
        "Premain-Class",
        "me.ialistannen.me.ialistannen.jvmagentutils.instrumentation.ExternalAgentAttacher"
    );

    return manifest;
  }

  private static String attachPrefix(String message, Object... formatArgs) {
    return "[AgentRunner] " + String.format(message, formatArgs);
  }
}
