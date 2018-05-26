package me.ialistannen.jvmagentutils.instrumentation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JvmUtils {

  /**
   * Attaches a given agent to the JVM by creating a temp agent jar for it.
   *
   * @param agentMain the main class of the agent
   * @param agentClasses all classes (including the main) that should be written to the agent
   */
  public static Path generateAgentJar(Class<?> agentMain, Class<?>... agentClasses) {
    try {
      Path agentJar = createAgentJar();
      Manifest manifest = generateManifest(agentMain);

      writeClassesToJar(agentJar, manifest, agentClasses);

      return agentJar;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Path createAgentJar() throws IOException {
    Path agentJar = Files.createTempFile("agent", ".jar");

    // Hacky shutdown hook to hopefully clean up
    agentJar.toFile().deleteOnExit();

    return agentJar;
  }

  private static Manifest generateManifest(Class<?> agentClass) {
    Manifest manifest = new Manifest();
    Attributes mainAttributes = manifest.getMainAttributes();
    mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    mainAttributes.put(new Attributes.Name("Agent-Class"), agentClass.getName());
    mainAttributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
    mainAttributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");

    return manifest;
  }

  /**
   * Writes the given classes into a jar file.
   *
   * @param jarFile the file to write to
   * @param jarManifest the manifest
   * @param classes the classes to write
   */
  static void writeClassesToJar(Path jarFile, Manifest jarManifest, Class<?>... classes) {
    try (OutputStream outputStream = Files.newOutputStream(jarFile);
        JarOutputStream jarOutputStream = new JarOutputStream(outputStream, jarManifest)) {

      for (Class<?> aClass : classes) {
        String resourceName = ClassUtils.getResourceName(aClass);
        jarOutputStream.putNextEntry(new JarEntry(resourceName));
        jarOutputStream.write(ClassUtils.getClassFileBytes(aClass));
        jarOutputStream.closeEntry();
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
