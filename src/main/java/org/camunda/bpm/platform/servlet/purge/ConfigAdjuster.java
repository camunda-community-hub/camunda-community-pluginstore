/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.platform.servlet.purge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public abstract class ConfigAdjuster {

  public final ObjectMapper objectMapper = new ObjectMapper();
  public final String CUSTOM_SCRIPTS = "[\n" +
    "  ]";

  private final String LOCALES =
    "{\n" +
        "\n" +
    "    \"availableLocales\": [\"en\"],\n" +
        "\n" +
    "    \"fallbackLocale\": \"en\"\n" +
        "\n" +
    "}";
  private String pathToConfigFile;

  public void setPathToConfigFile(String pathToConfigFile) {
    this.pathToConfigFile = pathToConfigFile;
  }

  public void adjustConfig() throws IOException {
    String json = "";
    List<String> lines = Files.readAllLines(Paths.get(pathToConfigFile), Charset.defaultCharset());
    int indexOfLineWithConfig = 0;
    String configName = "export default";
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains(configName)) {
        indexOfLineWithConfig = i;
        break;
      }
    }

    json += lines.get(indexOfLineWithConfig)
      .substring(lines.get(indexOfLineWithConfig).lastIndexOf("{")) + "\n";

    int curlyBracesCount = 1;
    int endIndexOfConfig = 0;
    for (int i = indexOfLineWithConfig + 1; i < lines.size() && curlyBracesCount > 0; i++) {
      String line = lines.get(i);
      String trimmedLine = line.trim();
      if (!trimmedLine.startsWith("//")) {
        json += line + "\n";
        if (trimmedLine.contains("{")) {
          curlyBracesCount++;
        } else if (trimmedLine.contains("}")) {
          if (json.lastIndexOf(";") == json.trim().length() - 1) {
            json = json.substring(0, json.lastIndexOf(";"));
          }
          curlyBracesCount--;
        }
      }
      endIndexOfConfig = i;
    }

    Configuration conf = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
    DocumentContext parse = JsonPath.using(conf).parse(json);

    // create custom scripts if does not exists
    Object customSCriptObject = objectMapper.readValue(CUSTOM_SCRIPTS, List.class);
    Object result = parse.read("$.customScripts");
    String jsonString = parse.jsonString();
    if (result == null) {
      jsonString = parse.put("$", "customScripts", customSCriptObject).jsonString();
    }

    // create locales for translations if does not exists
    Object defaultLocalesAsMap = objectMapper.readValue(LOCALES, Map.class);
    Object currentLocales = parse.read("$.locales");
    if (currentLocales == null) {
      jsonString = parse.put("$", "locales", defaultLocalesAsMap).jsonString();
    }

    // add plugin store stuff
    DocumentContext customScriptsContext = JsonPath.using(conf).parse(jsonString);
    customScriptsContext = adjustCustomScripts(customScriptsContext);


    jsonString = customScriptsContext.jsonString();

    // create the new file
    StringBuilder newConfigFile = new StringBuilder();
    for (int i = 0; i < indexOfLineWithConfig; i++) {
      newConfigFile.append(lines.get(i) + "\n");
    }

    newConfigFile.append(configName + " ");
    newConfigFile.append(jsonToPretty(jsonString));
    newConfigFile.append(";\n");

    for (int i = endIndexOfConfig + 1; i < lines.size(); i++) {
      newConfigFile.append(lines.get(i) + "\n");
    }

    newConfigFile = adjustFinalConfig(newConfigFile);
    System.out.println(newConfigFile);

    try (PrintWriter out = new PrintWriter(pathToConfigFile)) {
      out.println(newConfigFile);
    }

//    System.out.println(json);
  }

  protected StringBuilder adjustFinalConfig(StringBuilder newConfigFile) {
    // by default change nothing
    return newConfigFile;
  }

  protected abstract DocumentContext adjustCustomScripts(DocumentContext customScriptsContext);

  protected DocumentContext addEntryToArray(DocumentContext customScriptsContext, String arrayPath,
                                          String arrayField, String value) {
    List nDepsList = customScriptsContext.read(arrayPath + "." + arrayField);
    nDepsList.removeIf(e -> e.equals(value));
    nDepsList.add(value);
    customScriptsContext = customScriptsContext.put(arrayPath, arrayField, nDepsList);
    return customScriptsContext;
  }

  protected DocumentContext deleteEntryFromArray(DocumentContext customScriptsContext, String arrayPath,
                                          String arrayField, String value) {
    List<String> nDepsList = customScriptsContext.read(arrayPath + "." + arrayField);
    nDepsList.removeIf(e -> e.equals(value));
    customScriptsContext = customScriptsContext.put(arrayPath, arrayField, nDepsList);
    return customScriptsContext;
  }

  private String jsonToPretty(String json) throws IOException {
    Object map = objectMapper.readValue(json, Map.class);
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
  }

}
