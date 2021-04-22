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

import com.jayway.jsonpath.DocumentContext;

import java.util.List;

public class DeleteCustomScriptPluginConfigAdjuster extends ConfigAdjuster {

  private String pluginId;
  private List<String> ngDeps;

  private String startDelimiterForAdditionalConfigEntries;
  private String endDelimiterForAdditionalConfigEntries;

  public void setPluginId(String pluginId) {
    this.pluginId = pluginId;
    this.startDelimiterForAdditionalConfigEntries = "// -- " + pluginId + " START";
    this.endDelimiterForAdditionalConfigEntries = "// -- " + pluginId + " END";
  }

  public void setNgDeps(List<String> ngDeps) {
    this.ngDeps = ngDeps;
  }

  protected DocumentContext adjustCustomScripts(DocumentContext customScriptsContext) {
    customScriptsContext = deleteEntryFromArray(customScriptsContext, "$", "customScripts", "scripts/" + pluginId +"/index");
    return customScriptsContext;
  }

  @Override
  protected StringBuilder adjustFinalConfig(StringBuilder newConfigFile) {
    String str = newConfigFile.toString();
    int start = str.indexOf(startDelimiterForAdditionalConfigEntries);
    if (start > 0) {
      str = str.substring(0, start) +
        str.substring(str.indexOf(endDelimiterForAdditionalConfigEntries) + endDelimiterForAdditionalConfigEntries.length());
    }
    newConfigFile = new StringBuilder(str);
    return super.adjustFinalConfig(newConfigFile);
  }
}
