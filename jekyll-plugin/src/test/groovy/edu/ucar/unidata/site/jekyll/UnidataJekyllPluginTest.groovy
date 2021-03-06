package edu.ucar.unidata.site.jekyll

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class UnidataJekyllPluginTest extends Specification {

  @Rule
  TemporaryFolder tmpProjectDir = new TemporaryFolder()
  File buildScript

  def cleanFilePathString(String filepath) {
    // fix windows paths
    if (File.separator == '\\') {
      filepath = filepath.replaceAll('\\\\', '/')
    }
    return filepath
  }

  def setup() {
    // needed to tell the plugin to not add the jekyll-gem dependency, as we will load it out of its
    // project directory
    System.setProperty("UnidataJekyllPluginTesting", "true")

    // get a path to the jekyll-gem jar file, as we will have to add this dependency specifically
    String jekyllGemPath = new File('../jekyll-gems/build/libs/').absolutePath
    jekyllGemPath = cleanFilePathString(jekyllGemPath)

    // copy basic jekyll site for testing
    FileUtils.copyDirectory(new File('src/test/testSiteProject/'), tmpProjectDir.root )

    // create a basic gradle build script and gradle settings file for our temp project
    def settingsFile = tmpProjectDir.newFile('settings.gradle')
    buildScript = tmpProjectDir.newFile('build.gradle')

    buildScript.write """
      plugins {
        id 'edu.ucar.unidata.site.jekyll'
      }

      dependencies {
        gemjar fileTree(dir: \"${jekyllGemPath}\", include: 'jekyll-gems-*.jar')
      }

      """
    settingsFile.write "rootProject.name = 'test-jekyll-site'"
  }

  def cleanup() {
    System.setProperty("UnidataJekyllPluginTesting", "false")
  }

  def "can build default jekyll site."() {
    String jekyllSiteIndexFileString = "${tmpProjectDir.root}/build/site/index.html"
    jekyllSiteIndexFileString = cleanFilePathString(jekyllSiteIndexFileString)
    File jekyllSiteIndexFile  = new File(jekyllSiteIndexFileString)
    when: "a basic, zero config jekyll site project is built following the plugin's conventions"
    def result = GradleRunner.create()
        .withProjectDir(tmpProjectDir.root)
        .withArguments('buildJekyllSite')
        .withPluginClasspath()
        .withDebug(true)
        .build()
    then: "make sure the html is generated in the default location"
    jekyllSiteIndexFile.exists()
    and: "that the task is successful"
    result.task(":buildJekyllSite").outcome == SUCCESS
  }

  def "can build default jekyll site override output dir."() {
    given: "a basic jekyll site project uses a non-conventional output directory"
    String testSiteDestinationDir = "${tmpProjectDir.root}/_site"
    testSiteDestinationDir = cleanFilePathString(testSiteDestinationDir)

    buildScript.append """
    unidataJekyll {
      destinationDirectory = file('${testSiteDestinationDir}')
    }
    """
    String jekyllSiteIndexFileString = "${testSiteDestinationDir}/index.html"
    jekyllSiteIndexFileString = cleanFilePathString(jekyllSiteIndexFileString)
    File jekyllSiteIndexFile  = new File(jekyllSiteIndexFileString)
    when: "building that site"
    def result = GradleRunner.create()
        .withProjectDir(tmpProjectDir.root)
        .withArguments('buildJekyllSite')
        .withPluginClasspath()
        .withDebug(true)
        .build()
    then: "make sure the site is built in the non-conventional location"
    jekyllSiteIndexFile.exists()
    and: "that the task is successful"
    result.task(":buildJekyllSite").outcome == SUCCESS
  }

  def "can apply plugin."() {
    when: "the plugin is applied"
    def result = GradleRunner.create()
        .withProjectDir(tmpProjectDir.root)
        .withArguments('tasks')
        .withPluginClasspath()
        .build()
    then: "the build task is available"
    result.output.contains('buildJekyllSite')
    and: "that the serve task is available"
    result.output.contains('serveJekyllSite')
    and: "that the tasks task ran successfully"
    result.task(":tasks").outcome == SUCCESS
  }
}
