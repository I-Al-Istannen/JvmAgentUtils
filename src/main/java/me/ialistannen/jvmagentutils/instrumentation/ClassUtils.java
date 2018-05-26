package me.ialistannen.jvmagentutils.instrumentation;

public class ClassUtils {

  /**
   * Returns the bytes for a given class.
   *
   * @param clazz the class to get the bytes for
   * @return the read bytes
   */
  public static byte[] getClassFileBytes(Class<?> clazz) {
    String resourceName = getResourceName(clazz);
    return IOUtils.getAllBytes(getClassLoader(clazz).getResourceAsStream(resourceName));
  }

  private static ClassLoader getClassLoader(Class<?> clazz) {
    if (clazz.getClassLoader() == null) {
      return ClassLoader.getSystemClassLoader();
    }
    return clazz.getClassLoader();
  }

  /**
   * Returns the resource name for a class (i.e. replace "." with "/" and append ".class").
   *
   * @param clazz the class to get it for
   * @return the resource name
   */
  public static String getResourceName(Class<?> clazz) {
    return clazz.getName().replace('.', '/') + ".class";
  }
}