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
package org.everit.jira.core.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.everit.jira.core.EVWorklogManager;
import org.everit.jira.core.util.WorklogUtil;
import org.everit.jira.timetracker.plugin.dto.EveritWorklog;
import org.everit.jira.timetracker.plugin.dto.EveritWorklogComparator;
import org.everit.jira.timetracker.plugin.exception.WorklogException;
import org.everit.jira.timetracker.plugin.util.DateTimeConverterUtil;
import org.ofbiz.core.entity.EntityCondition;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParameters;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogNewEstimateInputParameters;
import com.atlassian.jira.bc.issue.worklog.WorklogResult;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;

/**
 * Implementation of {@link EVWorklogManager}.
 */
public class WorklogComponent implements EVWorklogManager {

  /**
   * Keys for properties.
   */
  public static final class PropertiesKey {

    public static final String DATE_PARSE = "plugin.date_parse";

    public static final String INVALID_ISSUE = "plugin.invalid_issue";

    public static final String NOPERMISSION_CREATE_WORKLOG = "jttp.nopermission.worklog.create";

    public static final String NOPERMISSION_DELETE_WORKLOG = "jttp.nopermission.worklog.delete";

    public static final String NOPERMISSION_ISSUE = "plugin.nopermission_issue";

    public static final String NOPERMISSION_UPDATE_WORKLOG = "jttp.nopermission.worklog.update";

    public static final String WORKLOG_CREATE_FAIL = "plugin.worklog.create.fail";

    public static final String WORKLOG_DELETE_FAIL = "plugin.worklog.delete.fail";

    public static final String WORKLOG_NOT_EXISTS = "plugin.worklog.not.exists";

    public static final String WORKLOG_UPDATE_FAIL = "plugin.worklog.update.fail";

    private PropertiesKey() {
    }
  }

  @Override
  public long countWorklogsWithoutPermissionChecks(final Date startDate, final Date endDate) {
    Calendar startDateCalendar = DateTimeConverterUtil.setDateToDayStart(startDate);
    Calendar endDateCalendar = (Calendar) startDateCalendar.clone();
    if (endDate == null) {
      endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
    } else {
      endDateCalendar = DateTimeConverterUtil.setDateToDayStart(endDate);
      endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
    }

    JiraAuthenticationContext authenticationContext = ComponentAccessor
        .getJiraAuthenticationContext();
    ApplicationUser loggedInUser = authenticationContext.getLoggedInUser();

    List<EntityCondition> exprList =
        WorklogUtil.createWorklogQueryExprList(startDateCalendar, endDateCalendar,
            loggedInUser.getKey());

    List<GenericValue> worklogGVList = ComponentAccessor.getOfBizDelegator()
        .findByAnd("IssueWorklogView", exprList);
    return worklogGVList.size();
  }

  @Override
  public void createWorklog(final String issueId, final String comment, final Date date,
      final String startTime, final String timeSpent) throws WorklogException {
    JiraAuthenticationContext authenticationContext = ComponentAccessor
        .getJiraAuthenticationContext();
    ApplicationUser user = authenticationContext.getLoggedInUser();
    JiraServiceContext serviceContext = new JiraServiceContextImpl(user);
    IssueManager issueManager = ComponentAccessor.getIssueManager();
    MutableIssue issue = issueManager.getIssueObject(issueId);
    if (issue == null) {
      throw new WorklogException(PropertiesKey.INVALID_ISSUE, issueId);
    }
    PermissionManager permissionManager = ComponentAccessor.getPermissionManager();
    if (!permissionManager.hasPermission(Permissions.WORK_ISSUE, issue,
        user)) {
      throw new WorklogException(PropertiesKey.NOPERMISSION_ISSUE, issueId);
    }
    Date startDate;
    try {
      startDate = DateTimeConverterUtil.stringToDateAndTime(date, startTime);
    } catch (IllegalArgumentException e) {
      throw new WorklogException(PropertiesKey.DATE_PARSE, date + " " + startTime);
    }

    WorklogNewEstimateInputParameters params = WorklogInputParametersImpl
        .issue(issue).startDate(startDate).timeSpent(timeSpent)
        .comment(comment).buildNewEstimate();
    WorklogService worklogService = ComponentAccessor.getComponent(WorklogService.class);
    if (!worklogService.hasPermissionToCreate(serviceContext, issue, true)) {
      throw new WorklogException(PropertiesKey.NOPERMISSION_CREATE_WORKLOG, issueId);
    }
    WorklogResult worklogResult = worklogService.validateCreate(
        serviceContext, params);
    if (worklogResult == null) {
      throw new WorklogException(PropertiesKey.WORKLOG_CREATE_FAIL);
    }
    Worklog createdWorklog = worklogService.createAndAutoAdjustRemainingEstimate(serviceContext,
        worklogResult, true);
    if (createdWorklog == null) {
      throw new WorklogException(PropertiesKey.WORKLOG_CREATE_FAIL);
    }
  }

  @Override
  public void deleteWorklog(final Long worklogId) throws WorklogException {
    JiraAuthenticationContext authenticationContext = ComponentAccessor
        .getJiraAuthenticationContext();
    ApplicationUser user = authenticationContext.getLoggedInUser();
    JiraServiceContext serviceContext = new JiraServiceContextImpl(user);
    WorklogService worklogService = ComponentAccessor.getComponent(WorklogService.class);
    Worklog worklog = getWorklogById(worklogId);
    if (!worklogService.hasPermissionToDelete(serviceContext, worklog)) {
      throw new WorklogException(PropertiesKey.NOPERMISSION_DELETE_WORKLOG,
          worklog.getIssue().getKey());
    }
    WorklogResult deleteWorklogResult = worklogService.validateDelete(
        serviceContext, worklogId);
    if (deleteWorklogResult == null) {
      throw new WorklogException(PropertiesKey.WORKLOG_DELETE_FAIL, worklogId.toString());
    }
    worklogService.deleteAndAutoAdjustRemainingEstimate(serviceContext,
        deleteWorklogResult, true);
  }

  @Override
  public void editWorklog(final Long worklogId, final String issueId, final String comment,
      final Date date, final String time, final String timeSpent) throws WorklogException {
    JiraAuthenticationContext authenticationContext = ComponentAccessor
        .getJiraAuthenticationContext();
    ApplicationUser user = authenticationContext.getLoggedInUser();
    JiraServiceContext serviceContext = new JiraServiceContextImpl(user);

    Worklog worklog = getWorklogById(worklogId);
    IssueManager issueManager = ComponentAccessor.getIssueManager();
    MutableIssue issue = issueManager.getIssueObject(issueId);
    if (issue == null) {
      throw new WorklogException(PropertiesKey.INVALID_ISSUE, issueId);
    }
    if (!worklog.getIssue().getKey().equals(issueId)) {
      PermissionManager permissionManager = ComponentAccessor.getPermissionManager();
      if (!permissionManager.hasPermission(Permissions.WORK_ISSUE, issue,
          user)) {
        throw new WorklogException(PropertiesKey.NOPERMISSION_ISSUE, issueId);
      }

      createWorklog(issueId, comment, date, time, timeSpent);

      deleteWorklog(worklogId);
    } else {
      Date dateCreate;
      try {
        dateCreate = DateTimeConverterUtil
            .stringToDateAndTime(date, time);
      } catch (IllegalArgumentException e) {
        throw new WorklogException(PropertiesKey.DATE_PARSE, date + " " + time);
      }
      WorklogInputParameters params = WorklogInputParametersImpl
          .issue(issue).startDate(dateCreate).timeSpent(timeSpent)
          .comment(comment).worklogId(worklogId).issue(issue).build();
      WorklogService worklogService = ComponentAccessor.getComponent(WorklogService.class);
      if (!worklogService.hasPermissionToUpdate(serviceContext, worklog)) {
        throw new WorklogException(PropertiesKey.NOPERMISSION_UPDATE_WORKLOG, issueId);
      }
      WorklogResult worklogResult = worklogService.validateUpdate(
          serviceContext, params);
      if (worklogResult == null) {
        throw new WorklogException(PropertiesKey.WORKLOG_UPDATE_FAIL);
      }

      worklogService.updateAndAutoAdjustRemainingEstimate(serviceContext,
          worklogResult, true);

    }
  }

  @Override
  public EveritWorklog getWorklog(final Long worklogId) throws ParseException, WorklogException {
    return new EveritWorklog(getWorklogById(worklogId));
  }

  private Worklog getWorklogById(final Long worklogId) throws WorklogException {
    WorklogManager worklogManager = ComponentAccessor.getWorklogManager();
    Worklog worklog = worklogManager.getById(worklogId);
    if (worklog == null) {
      throw new WorklogException(PropertiesKey.WORKLOG_NOT_EXISTS);
    }
    return worklog;
  }

  @Override
  public List<EveritWorklog> getWorklogs(final String selectedUser, final Date startDate,
      final Date endDate) throws DataAccessException, ParseException {
    Calendar startDateCalendar = DateTimeConverterUtil.setDateToDayStart(startDate);
    Calendar endDateCalendar = (Calendar) startDateCalendar.clone();
    if (endDate == null) {
      endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
    } else {
      endDateCalendar = DateTimeConverterUtil.setDateToDayStart(endDate);
      endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
    }

    List<EveritWorklog> worklogs = new ArrayList<>();

    JiraAuthenticationContext authenticationContext = ComponentAccessor
        .getJiraAuthenticationContext();
    ApplicationUser loggedInUser = authenticationContext.getLoggedInUser();

    String userKey;
    if ((selectedUser == null) || "".equals(selectedUser)) {
      userKey = loggedInUser.getKey();
    } else {
      userKey = ComponentAccessor.getUserUtil().getUserByName(selectedUser).getKey();
    }

    List<EntityCondition> exprList =
        WorklogUtil.createWorklogQueryExprListWithPermissionCheck(userKey,
            loggedInUser, startDateCalendar, endDateCalendar);

    List<GenericValue> worklogGVList = ComponentAccessor.getOfBizDelegator()
        .findByAnd("IssueWorklogView", exprList);

    IssueManager issueManager = ComponentAccessor.getIssueManager();
    GroupManager groupManager = ComponentAccessor.getGroupManager();
    ProjectRoleManager projectRoleManager =
        ComponentAccessor.getComponent(ProjectRoleManager.class);

    for (GenericValue worklogGv : worklogGVList) {
      boolean hasWorklogVisibility = WorklogUtil.hasWorklogVisibility(loggedInUser,
          issueManager,
          groupManager,
          projectRoleManager,
          worklogGv);
      if (hasWorklogVisibility) {
        EveritWorklog worklog = new EveritWorklog(worklogGv);
        worklogs.add(worklog);
      }
    }

    Collections.sort(worklogs, EveritWorklogComparator.INSTANCE);
    return worklogs;
  }

}