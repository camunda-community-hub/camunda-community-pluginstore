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

public class DeleteTranslationPluginConfigAdjuster extends ConfigAdjuster {

  private List<String> locales;

  public void setLocales(List<String> locales) {
    this.locales = locales;
  }

  protected DocumentContext adjustCustomScripts(DocumentContext customScriptsContext) {
    String arrayPath = "$.locales";
    String arrayField = "availableLocales";
    for (String local : locales) {
      local = local.substring(0, local.lastIndexOf("."));
      customScriptsContext = deleteEntryFromArray(customScriptsContext, arrayPath, arrayField, local);
    }
    return customScriptsContext;
  }
}
