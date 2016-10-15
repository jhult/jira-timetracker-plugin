/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.jira.settings.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TimeTrackerGlobalSettings {

  private Map<GlobalsSettingsKey, Object> pluginSettingsKeyValues = new HashMap<>();

  public TimeTrackerGlobalSettings analyticsCheck(final boolean analyticsCheck) {
    pluginSettingsKeyValues.put(GlobalsSettingsKey.ANALYTICS_CHECK_CHANGE,
        Boolean.toString(analyticsCheck));
    return null;
  }

  public TimeTrackerGlobalSettings collectorIssues(final List<Pattern> collectorIssues) {
    // TODO in pattern out string
    pluginSettingsKeyValues.put(GlobalsSettingsKey.NON_ESTIMATED_ISSUES, collectorIssues);
    return this;
  }

  public TimeTrackerGlobalSettings excludeDates(final String excludeDates) {
    pluginSettingsKeyValues.put(GlobalsSettingsKey.EXCLUDE_DATES, excludeDates);
    return this;
  }

  public TimeTrackerGlobalSettings filteredSummaryIssues(
      final List<Pattern> filteredSummaryIssues) {
    pluginSettingsKeyValues.put(GlobalsSettingsKey.SUMMARY_FILTERS, filteredSummaryIssues);
    return this;
  }

  /**
   * Gets analytics check value.
   *
   * @return the analytics check value.
   */
  public boolean getAnalyticsCheck() {
    boolean analyticsCheckValue = true;
    if ("false".equals(pluginSettingsKeyValues.get(GlobalsSettingsKey.ANALYTICS_CHECK_CHANGE))) {
      analyticsCheckValue = false;
    }
    return analyticsCheckValue;
  }

  public String getExcludeDates() {
    return (String) pluginSettingsKeyValues.get(GlobalsSettingsKey.EXCLUDE_DATES);
  }

  public String getIncludeDates() {
    return (String) pluginSettingsKeyValues.get(GlobalsSettingsKey.INCLUDE_DATES);
  }

  public List<String> getIssuePatterns() {
    // TODO in pattern out string
    List<String> tempIssuePatternList = (List<String>) pluginSettingsKeyValues.get(
        GlobalsSettingsKey.NON_ESTIMATED_ISSUES);
    return tempIssuePatternList;
  }

  public long getLastUpdate() {
    String lastUpdateInMilisec = (String) pluginSettingsKeyValues.get(
        GlobalsSettingsKey.UPDATE_NOTIFIER_LAST_UPDATE);
    return lastUpdateInMilisec == null ? null : Long.parseLong(lastUpdateInMilisec);

  }

  public String getLatestVersion() {
    return (String) pluginSettingsKeyValues.get(GlobalsSettingsKey.LATEST_VERSION);
  }

  public List<String> getPluginGroups() {
    List<String> pluginGroupsNames =
        (List<String>) pluginSettingsKeyValues.get(GlobalsSettingsKey.PLUGIN_PERMISSION);
    List<String> pluginGroups = new ArrayList<>();
    if (pluginGroupsNames != null) {
      pluginGroups = pluginGroupsNames;
    }
    return pluginGroups;
  }

  public Map<GlobalsSettingsKey, Object> getPluginSettingsKeyValues() {
    return pluginSettingsKeyValues;
  }

  public String getPluginUUID() {
    return (String) pluginSettingsKeyValues.get(GlobalsSettingsKey.PLUGIN_UUID);
  }

  public List<String> getSummaryFiletrs() {
    List<String> tempSummaryFilter =
        (List<String>) pluginSettingsKeyValues.get(GlobalsSettingsKey.SUMMARY_FILTERS);
    return tempSummaryFilter;
  }

  public List<String> getTimetrackerGroups() {
    List<String> timetrackerGroupsNames =
        (List<String>) pluginSettingsKeyValues.get(GlobalsSettingsKey.TIMETRACKER_PERMISSION);
    List<String> timetrackerGroups = new ArrayList<>();
    if (timetrackerGroupsNames != null) {
      timetrackerGroups = timetrackerGroupsNames;
    }
    return timetrackerGroups;
  }

  public TimeTrackerGlobalSettings includeDates(final String includeDates) {
    pluginSettingsKeyValues.put(GlobalsSettingsKey.INCLUDE_DATES, includeDates);
    return this;
  }

  public TimeTrackerGlobalSettings lastUpdateTime(final long time) {
    pluginSettingsKeyValues.put(
        GlobalsSettingsKey.UPDATE_NOTIFIER_LAST_UPDATE, String.valueOf(time));
    return this;
  }

  public TimeTrackerGlobalSettings latestVersion(final String latestVersion) {
    pluginSettingsKeyValues.put(GlobalsSettingsKey.UPDATE_NOTIFIER_LATEST_VERSION, latestVersion);
    return this;
  }

  public TimeTrackerGlobalSettings pluginGroups(final List<String> pluginGroups) {
    pluginSettingsKeyValues.put(GlobalsSettingsKey.PLUGIN_PERMISSION, pluginGroups);
    return this;
  }

  public TimeTrackerGlobalSettings pluginUUID(final String pluginUUID) {
    pluginSettingsKeyValues.put(GlobalsSettingsKey.PLUGIN_UUID, pluginUUID);
    return this;
  }

  public TimeTrackerGlobalSettings timetrackerGroups(final List<String> timetrackingGroups) {
    pluginSettingsKeyValues.put(GlobalsSettingsKey.TIMETRACKER_PERMISSION, timetrackingGroups);
    return this;
  }
}
