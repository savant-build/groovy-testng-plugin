/*
 * Copyright (c) 2013, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.testng

import groovy.xml.MarkupBuilder
import org.savantbuild.dep.domain.ArtifactID
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.lang.Classpath
import org.savantbuild.output.Output
import org.savantbuild.plugin.dep.DependencyPlugin
import org.savantbuild.plugin.groovy.BaseGroovyPlugin

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile

/**
 * The Groovy TestNG plugin. The public methods on this class define the features of the plugin.
 */
class GroovyTestNGPlugin extends BaseGroovyPlugin {
  public static
  final String ERROR_MESSAGE = "You must create the file [~/.savant/plugins/org.savantbuild.plugin.groovy.properties] " +
      "that contains the system configuration for the Groovy plugin. This file should include the location of the GDK " +
      "(groovy and groovyc) by version. These properties look like this:\n\n" +
      "  2.1.0=/Library/Groovy/Versions/2.1.0/Home\n" +
      "  2.2.0=/Library/Groovy/Versions/2.2.0/Home\n"
  public static
  final String JAVA_ERROR_MESSAGE = "You must create the file [~/.savant/plugins/org.savantbuild.plugin.java.properties] " +
      "that contains the system configuration for the Java system. This file should include the location of the JDK " +
      "(java and javac) by version. These properties look like this:\n\n" +
      "  1.6=/Library/Java/JavaVirtualMachines/1.6.0_65-b14-462.jdk/Contents/Home\n" +
      "  1.7=/Library/Java/JavaVirtualMachines/jdk1.7.0_10.jdk/Contents/Home\n" +
      "  1.8=/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home\n"
  GroovyTestNGSettings settings = new GroovyTestNGSettings()
  Properties properties
  Properties javaProperties
  Path javaPath
  Path groovyHomePath
  DependencyPlugin dependencyPlugin

  GroovyTestNGPlugin(Project project, Output output) {
    super(project, output)
    properties = loadConfiguration(new ArtifactID("org.savantbuild.plugin", "groovy", "groovy", "jar"), ERROR_MESSAGE)
    javaProperties = loadConfiguration(new ArtifactID("org.savantbuild.plugin", "java", "java", "jar"), JAVA_ERROR_MESSAGE)
    dependencyPlugin = new DependencyPlugin(project, output)
  }

  /**
   * Runs the TestNG tests.
   */
  void test() {
    initialize()

    Classpath classpath = dependencyPlugin.classpath(settings.resolveConfiguration) {
      path groovyHomePath.resolve("embeddable/groovy-all-${settings.groovyVersion}.jar")
      project.publications.group("main").each { publication -> path(publication.file) }
      project.publications.group("test").each { publication -> path(publication.file) }
    }

    Path xmlFile = buildXMLFile()

    String command = "${javaPath} ${settings.jvmArguments} ${classpath.toString("-classpath ")} org.testng.TestNG -d ${settings.reportDirectory} ${xmlFile}"
    println "Executing ${command}"
    Process process = command.execute(null, project.directory.toFile())
    process.consumeProcessOutput(System.out, System.err)

    int result = process.waitFor()
    Files.delete(xmlFile)
    if (result != 0) {
      fail("Build failed.")
    }
  }

  Path buildXMLFile() {
    Set<String> classNames = new TreeSet<>()
    project.publications.group("test").each { publication ->
      JarFile jarFile = new JarFile(project.directory.resolve(publication.file).toFile())
      jarFile.entries().each { entry ->
        if (!entry.directory && entry.name.endsWith("Test.class")) {
          classNames.add(entry.name.replace("/", ".").replace(".class", ""))
        }
      }
    }

    Path xmlFile = FileTools.createTempPath("savant", "testng.xml", true)
    BufferedWriter writer = Files.newBufferedWriter(xmlFile, Charset.forName("UTF-8"))
    MarkupBuilder xml = new MarkupBuilder(writer)
    xml.suite(name: "All Tests", "allow-return-values": "true", verbose: "10000000") {
      test(name: "All Tests") {
        classes {
          classNames.each { className -> "class"(name: className) }
        }
      }
    }

    writer.flush()
    writer.close()
    println "XML is " + new String(Files.readAllBytes(xmlFile))
    return xmlFile
  }

  private void initialize() {
    if (!settings.groovyVersion) {
      fail("You must configure the Groovy version to use with the settings object. It will look something like this:\n\n" +
          "  groovy.settings.groovyVersion=\"2.1.0\"")
    }

    String groovyHome = properties.getProperty(settings.groovyVersion)
    if (!groovyHome) {
      fail("No GDK is configured for version [${settings.groovyVersion}].\n\n${ERROR_MESSAGE}")
    }

    groovyHomePath = Paths.get(groovyHome)
    if (!Files.isDirectory(groovyHomePath)) {
      fail("The GDK directory [${groovyHome}] is invalid because it doesn't exist.")
    }

    Path groovyJar = groovyHomePath.resolve("embeddable/groovy-all-${settings.groovyVersion}.jar");
    if (!Files.isReadable(groovyJar)) {
      fail("The GDK directory [${groovyHome}] is invalid because it is missing the Groovy JAR file. It should be located at [${groovyJar}]")
    }

    if (!settings.javaVersion) {
      fail("You must configure the Java version to use with the settings object. It will look something like this:\n\n" +
          "  groovy.settings.javaVersion=\"1.7\"")
    }

    String javaHome = javaProperties.getProperty(settings.javaVersion)
    if (!javaHome) {
      fail("No JDK is configured for version [${settings.javaVersion}].\n\n${JAVA_ERROR_MESSAGE}")
    }

    javaPath = Paths.get(javaHome, "bin/java")
    if (!Files.isRegularFile(javaPath)) {
      fail("The java executable [${javaPath.toAbsolutePath()}] does not exist.")
    }
    if (!Files.isExecutable(javaPath)) {
      fail("The java executable [${javaPath.toAbsolutePath()}] is not executable.")
    }
  }
}
