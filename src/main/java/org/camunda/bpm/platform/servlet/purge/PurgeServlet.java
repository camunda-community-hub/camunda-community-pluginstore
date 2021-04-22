package org.camunda.bpm.platform.servlet.purge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.impl.ManagementServiceImpl;
import org.eclipse.jgit.api.Git;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

//import org.eclipse.jgit.util.FileUtils;

/**
 * Servlet that deployes
 */
@WebServlet(name="DeployBasicProcessServlet", urlPatterns={"/*"})
public class PurgeServlet extends HttpServlet {
  private List<ManagementServiceImpl> managementServices;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    managementServices = (List<ManagementServiceImpl>) config.getServletContext().getAttribute("managementServices");
    this.objectMapper = new ObjectMapper();
  }

  private static String OS = System.getProperty("os.name").toLowerCase();

  public static boolean isWindows() {

		return (OS.indexOf("win") >= 0);

	}

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      if (Objects.equals("/plugins", req.getPathInfo())) {
        retrieveAllAvailablePlugins(resp);
      } else if (Objects.equals("/installed", req.getPathInfo())) {
        retrieveInstalledPlugins(resp);
      } else if (req.getPathInfo() != null  && req.getPathInfo().startsWith("/install")) {
        installPlugin(req.getParameter("id"), resp);
      } else if (req.getPathInfo() != null  && req.getPathInfo().startsWith("/uninstall")) {
        uninstallPlugin(req.getParameter("id"), resp);
      } else if (req.getPathInfo() != null  && req.getPathInfo().startsWith("/image")) {
        getImage(req.getPathInfo(), resp);
      }
    } catch (Exception e) {
      e.printStackTrace();
      resp.sendError(500);
    }

    System.out.println("Got request from: " + req.getServletPath());
    System.out.println("Context path: " + req.getContextPath());
    System.out.println("Path info: " + req.getPathInfo());
  }

  private void getImage(String pathInfo, HttpServletResponse response) throws IOException {


    String pluginId = pathInfo.substring(pathInfo.lastIndexOf("/") + 1, pathInfo.lastIndexOf("."));

    System.out.println("Returning image for plugin " + pluginId);


    String classPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
    if (isWindows()) {
      classPath = classPath.replaceFirst("/", "");
    }
    Path classesFolderPath = Paths.get(classPath);

    Path webInfPath = classesFolderPath.getParent();
    Path camundaPluginStore = webInfPath.resolve("community-summit-plugins");
    File pluginFolder = camundaPluginStore.resolve(pluginId).toFile();
    if (pluginFolder.exists()) {

      File screenshot = camundaPluginStore.resolve(pluginId).resolve("screenshot.png").toFile();

      if (screenshot.exists()) {

        response.setHeader("Content-Type", "image/png");
        response.setHeader("success", "yes");
        FileInputStream fileInputStreamReader = new FileInputStream(screenshot);
        byte[] bytes = new byte[(int) screenshot.length()];
        fileInputStreamReader.read(bytes);
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
      } else {
        throw new RuntimeException("Could not find screen shot for plugin!");
      }

    } else {
      throw new RuntimeException("Could not find plugin folder!");
    }
  }

  private void uninstallPlugin(String pluginId, HttpServletResponse resp) throws IOException {

    System.out.println("Uninstalling plugin with id: " + pluginId);

    Path scripts = getCockpitScriptsPath();

    CamundaPluginSetup camundaPluginSetup = readPluginSetup(pluginId);
    if (Objects.equals(camundaPluginSetup.getType().toLowerCase().trim(), "custom script")) {
      uninstallCustomScriptsPlugin(pluginId, scripts, camundaPluginSetup);
    } else if (Objects.equals(camundaPluginSetup.getType().toLowerCase().trim(), "translation")) {
      uninstallTranslationPlugin(pluginId);
    }
  }

  private void uninstallTranslationPlugin(String pluginId) throws IOException {
    Path srcDirPath = getCamundaPluginRepoPathForPlugin(pluginId).resolve("src");
    List<String> locals = Arrays.stream(srcDirPath.toFile()
                                           .listFiles(f -> f.isFile() && f.getName().endsWith(".json")))
      .map(File::getName)
      .collect(Collectors.toList());

    File configFile = getCockpitScriptsPath().resolve("config.js").toFile();
    if (configFile.exists()) {
      System.out.println("Found config file!");

      DeleteTranslationPluginConfigAdjuster adjuster = new DeleteTranslationPluginConfigAdjuster();
      adjuster.setPathToConfigFile(configFile.getPath());
      adjuster.setLocales(locals);
      adjuster.adjustConfig();
    } else {
      throw new RuntimeException("Could not find config file!");
    }

    // delete locales files
    if (srcDirPath.toFile().exists()) {

      Path newPluginFolder = getCockpitScriptsPath().resolve(pluginId);
      newPluginFolder.toFile().delete();

      Arrays.stream(getCockpitScriptsPath().toFile().listFiles(f -> locals.contains(f.getName())))
        .forEach(
          file -> file.delete()
        );

    } else {
      throw new RuntimeException("Could not find source folder of plugin!");
    }
  }

  private void uninstallCustomScriptsPlugin(String pluginId, Path scripts, CamundaPluginSetup camundaPluginSetup) throws
                                                                                                                  IOException {
    Path camundaPluginStore = getCamundaPluginStoreRepoPath();
    Map<String, Object> config = camundaPluginSetup.getConfig();
    // List<String> ngDeps = (List<String>) config.get("ngDeps");

    File configFile = scripts.resolve("config.js").toFile();
    if (configFile.exists()) {
      System.out.println("Found config file!");

      DeleteCustomScriptPluginConfigAdjuster adjuster = new DeleteCustomScriptPluginConfigAdjuster();
      adjuster.setPathToConfigFile(configFile.getPath());
      // adjuster.setNgDeps(ngDeps);
      adjuster.setPluginId(pluginId);
      adjuster.adjustConfig();
    } else {
      throw new RuntimeException("Could not find config file!");
    }

    // delete plugin folder
    Path srcDirPath = camundaPluginStore.resolve(pluginId).resolve("src");
    if (srcDirPath.toFile().exists()) {

      Path pluginFolderInScriptDir = scripts.resolve(pluginId);
      FileUtils.deleteDirectory(pluginFolderInScriptDir.toFile());
    } else {
      throw new RuntimeException("Could not find source folder of plugin!");
    }
  }

  private CamundaPluginSetup readPluginSetup(String pluginId) throws IOException {

    System.out.println("Reading plugin setup for id: " + pluginId);
    Path camundaPluginStore = getCamundaPluginStoreRepoPath();
    File pluginFolder = camundaPluginStore.resolve(pluginId).toFile();
    if (pluginFolder.exists()) {
      File file = Arrays.stream(pluginFolder.listFiles(f -> f.getName().equals("setup.json"))).findFirst().get();
      List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
      String setupJsonAsString = String.join("", lines);
      return objectMapper.readValue(setupJsonAsString, CamundaPluginSetup.class);
    } else {
      throw new RuntimeException("Could not find plugin folder!");
    }
  }

  private Path getCamundaPluginStoreRepoPath() {
    String classPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
    if (isWindows()) {
      classPath = classPath.replaceFirst("/", "");
    }
    Path classesFolderPath = Paths.get(classPath);
    Path webapps = classesFolderPath.getParent().getParent().getParent();

    Path webInfPath = classesFolderPath.getParent();
    return webInfPath.resolve("community-summit-plugins");
  }

  private Path getCamundaPluginRepoPathForPlugin(String pluginId) {
    return getCamundaPluginStoreRepoPath().resolve(pluginId);
  }

  private void installPlugin(String pluginId, HttpServletResponse resp) throws IOException {

    System.out.println("Installing plugin with id: " + pluginId);

    Path scripts = getCockpitScriptsPath();


    CamundaPluginSetup camundaPluginSetup = readPluginSetup(pluginId);
    System.out.println("Camunda plugin setup: " + camundaPluginSetup);
    if (Objects.equals(camundaPluginSetup.getType().toLowerCase().trim(), "custom script")) {
      installCustomScriptsPlugin(pluginId, scripts, camundaPluginSetup);
    } else if (Objects.equals(camundaPluginSetup.getType().toLowerCase().trim(), "translation")) {
      installTranslationPlugin(pluginId);
    }

  }

  private Path getCockpitScriptsPath() {
    String classPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
    if (isWindows()) {
      classPath = classPath.replaceFirst("/", "");
    }
    Path classesFolderPath = Paths.get(classPath);
    Path webapps = classesFolderPath.getParent().getParent().getParent();
    return webapps.resolve(
      "camunda" + File.separator + "app" + File.separator + "cockpit" +
        File.separator + "scripts");
  }

  private Path getCockpitLocalsPath() {
    String classPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
    if (isWindows()) {
      classPath = classPath.replaceFirst("/", "");
    }
    Path classesFolderPath = Paths.get(classPath);
    Path webapps = classesFolderPath.getParent().getParent().getParent();
    return webapps.resolve(
      "camunda" + File.separator + "app" + File.separator + "cockpit" +
        File.separator + "locales");
  }

  private void installTranslationPlugin(String pluginId) throws IOException {

    Path srcDirPath = getCamundaPluginRepoPathForPlugin(pluginId).resolve("src");
    List<String> locals = Arrays.stream(srcDirPath.toFile()
                                           .listFiles(f -> f.isFile() && f.getName().endsWith(".json")))
      .map(File::getName)
      .collect(Collectors.toList());

    File configFile = getCockpitScriptsPath().resolve("config.js").toFile();
    if (configFile.exists()) {
      System.out.println("Found config file!");

      AddTranslationPluginConfigAdjuster adjuster = new AddTranslationPluginConfigAdjuster();
      adjuster.setPathToConfigFile(configFile.getPath());
      adjuster.setLocales(locals);
      adjuster.adjustConfig();
    } else {
      throw new RuntimeException("Could not find config file!");
    }

    // copy files
    if (srcDirPath.toFile().exists()) {

      // create an empty folder so we can later find out that this
      // plugin has been installed
      Path newPluginFolder = getCockpitScriptsPath().resolve(pluginId);
      newPluginFolder.toFile().mkdir();

      Arrays.stream(srcDirPath.toFile().listFiles(f -> f.isFile() && f.getName().endsWith(".json")))
        .forEach(
          file -> {
            System.out.println("Found locales file to copy: " + file.getName());
            try {
              Files.copy(file.toPath(), getCockpitLocalsPath().resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              e.printStackTrace();
              throw new RuntimeException("Could not copy locales file", e);
            }
          }
        );
    } else {
      throw new RuntimeException("Could not find source folder of plugin!");
    }

  }

  private void installCustomScriptsPlugin(String pluginId, Path scripts, CamundaPluginSetup camundaPluginSetup) throws
                                                                                                                IOException {

    System.out.println("Installing custom scripts plugin");

    Map<String, Object> config = camundaPluginSetup.getConfig();
    // List<String> ngDeps = (List<String>) config.get("ngDeps");
    File configFile = scripts.resolve("config.js").toFile();
    if (configFile.exists()) {
      System.out.println("Found config file!");

      AddCustomScriptsPluginConfigAdjuster adjuster = new AddCustomScriptsPluginConfigAdjuster();
      adjuster.setPathToConfigFile(configFile.getPath());
      // adjuster.setNgDeps(ngDeps);
      adjuster.setPluginId(pluginId);
      Path configAdditionsPath = getCamundaPluginRepoPathForPlugin(pluginId).resolve("src").resolve("config.js");
      if (configAdditionsPath.toFile().exists()) {

        System.out.println("Found additional configs to add!");
        List<String> content = Files.readAllLines(
          Paths.get(configAdditionsPath.toFile().getPath()),
          Charset.defaultCharset()
        );
        String configAdditions = String.join("", content);

        System.out.println("Config additions:" + configAdditions);
        adjuster.setAdditionConfigEntries(configAdditions);
      }

      adjuster.adjustConfig();

    } else {
      throw new RuntimeException("Could not find config file!");
    }

    // copy files
    Path srcDirPath = getCamundaPluginRepoPathForPlugin(pluginId).resolve("src");
    if (srcDirPath.toFile().exists()) {

      Path newPluginFolder = scripts.resolve(pluginId);
      newPluginFolder.toFile().mkdir();

      try {
        FileUtils.copyDirectory(srcDirPath.toFile(), newPluginFolder.toFile());
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      throw new RuntimeException("Could not find source folder of plugin!");
    }
  }

  private void retrieveInstalledPlugins(HttpServletResponse response) throws IOException {
    Path scripts = getCockpitScriptsPath();
    File scriptFolder = scripts.toFile();
    File[] installedPlugins = scriptFolder.listFiles(f -> f.isDirectory() && !f.getName().equals("pluginStore"));
    List<String> result = Arrays.stream(installedPlugins).map(File::getName).collect(Collectors.toList());

    response.setHeader("Content-Type", "application/json");
    response.setHeader("success", "yes");
    PrintWriter writer = response.getWriter();
    writer.write(objectMapper.writeValueAsString(result));
    writer.close();
  }

  private void retrieveAllAvailablePlugins(HttpServletResponse response) throws Exception {

    String classPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
    if (isWindows()) {
      classPath = classPath.replaceFirst("/", "");
    }

    Path classesFolderPath = Paths.get(classPath);
    Path webInfPath = classesFolderPath.getParent();
    Path camundaPluginStore = webInfPath.resolve("community-summit-plugins");
    File camundaPluginStoreFolder = new File(camundaPluginStore.toString());
    Git git;
    if (!camundaPluginStoreFolder.exists()) {
      git = Git.cloneRepository()
      .setURI("https://github.com/camunda-community-hub/community-summit-plugins.git")
      .setDirectory(camundaPluginStoreFolder)
      .call();
    } else {
      git = Git.open(camundaPluginStoreFolder);
      git.pull().call();
    }

    List<CamundaPluginDto> plugins = new ArrayList<>();
//    System.out.println("Pluginstorefolder: " + camundaPluginStore.toString());
    File[] directories = camundaPluginStoreFolder.listFiles(f -> f.isDirectory() && !f.getName().equals(".git"));
//    Arrays.stream(directories).forEach(System.out::println);
    for (File directory : Objects.requireNonNull(directories)) {
      CamundaPluginDto plugin = new CamundaPluginDto();
      plugin.setId(directory.getName());

//      Arrays.stream(directory.listFiles()).forEach(System.out::println);
      Optional<File> setupJson = Arrays.stream(Objects.requireNonNull(directory.listFiles()))
        .filter(l -> l.getName().equals("setup.json"))
        .findFirst();
      if (setupJson.isPresent()) {
        List<String> lines = Files.readAllLines(Paths.get(setupJson.get().getPath()), Charset.defaultCharset());
        String setupJsonAsString = String.join("", lines);
        DocumentContext context = JsonPath.parse(setupJsonAsString);
        String title = context.read("$.title");
        plugin.setTitle(title);
        String description = context.read("$.description");
        plugin.setDescription(description);
      }
      plugins.add(plugin);
    }

    response.setHeader("Content-Type", "application/json");
    response.setHeader("success", "yes");
    PrintWriter writer = response.getWriter();
    writer.write(objectMapper.writeValueAsString(plugins));
    writer.close();
  }


}
