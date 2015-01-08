/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.server.logic.data;

import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.report.AbstractReportletConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.persistence.api.entity.Report;
import org.apache.syncope.persistence.api.entity.ReportExec;
import org.apache.syncope.server.logic.init.ImplementationClassNamesLoader;
import org.apache.syncope.server.logic.report.Reportlet;
import org.apache.syncope.server.logic.init.JobInstanceLoader;
import org.apache.syncope.server.logic.report.ReportletConfClass;
import org.apache.syncope.server.spring.BeanUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class ReportDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReportDataBinder.class);

    private static final String[] IGNORE_REPORT_PROPERTIES = { "key", "reportlets", "executions" };

    private static final String[] IGNORE_REPORT_EXECUTION_PROPERTIES = { "key", "report", "execResult" };

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Set<Class<Reportlet>> getAllReportletClasses() {
        Set<Class<Reportlet>> reportletClasses = new HashSet<Class<Reportlet>>();

        for (String className : classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.REPORTLET)) {
            try {
                Class reportletClass = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
                reportletClasses.add(reportletClass);
            } catch (ClassNotFoundException e) {
                LOG.warn("Could not load class {}", className);
            } catch (LinkageError e) {
                LOG.warn("Could not link class {}", className);
            }
        }
        return reportletClasses;
    }

    public Class<? extends ReportletConf> getReportletConfClass(final Class<Reportlet> reportletClass) {
        Class<? extends ReportletConf> result = null;

        ReportletConfClass annotation = reportletClass.getAnnotation(ReportletConfClass.class);
        if (annotation != null) {
            result = annotation.value();
        }

        return result;
    }

    public Class<Reportlet> findReportletClassHavingConfClass(final Class<? extends ReportletConf> reportletConfClass) {
        Class<Reportlet> result = null;
        for (Class<Reportlet> reportletClass : getAllReportletClasses()) {
            Class<? extends ReportletConf> found = getReportletConfClass(reportletClass);
            if (found != null && found.equals(reportletConfClass)) {
                result = reportletClass;
            }
        }

        return result;
    }

    public void getReport(final Report report, final ReportTO reportTO) {
        BeanUtils.copyProperties(reportTO, report, IGNORE_REPORT_PROPERTIES);
        report.getReportletConfs().clear();
        for (ReportletConf conf : reportTO.getReportletConfs()) {
            report.addReportletConf(conf);
        }
    }

    public ReportTO getReportTO(final Report report) {
        ReportTO reportTO = new ReportTO();
        reportTO.setId(report.getKey());
        BeanUtils.copyProperties(report, reportTO, IGNORE_REPORT_PROPERTIES);

        copyReportletConfs(report, reportTO);

        ReportExec latestExec = reportExecDAO.findLatestStarted(report);
        reportTO.setLatestExecStatus(latestExec == null
                ? ""
                : latestExec.getStatus());

        reportTO.setStartDate(latestExec == null
                ? null
                : latestExec.getStartDate());

        reportTO.setEndDate(latestExec == null
                ? null
                : latestExec.getEndDate());

        for (ReportExec reportExec : report.getExecs()) {
            reportTO.getExecutions().add(getReportExecTO(reportExec));
        }

        String triggerName = JobInstanceLoader.getTriggerName(JobInstanceLoader.getJobName(report));

        Trigger trigger;
        try {
            trigger = scheduler.getScheduler().getTrigger(new TriggerKey(triggerName, Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + triggerName, e);
            trigger = null;
        }

        if (trigger != null) {
            reportTO.setLastExec(trigger.getPreviousFireTime());
            reportTO.setNextExec(trigger.getNextFireTime());
        }

        return reportTO;
    }

    private void copyReportletConfs(final Report report, final ReportTO reportTO) {
        reportTO.getReportletConfs().clear();
        for (ReportletConf reportletConf : report.getReportletConfs()) {
            reportTO.getReportletConfs().add((AbstractReportletConf) reportletConf);
        }
    }

    public ReportExecTO getReportExecTO(final ReportExec execution) {
        ReportExecTO executionTO = new ReportExecTO();
        executionTO.setKey(execution.getKey());
        BeanUtils.copyProperties(execution, executionTO, IGNORE_REPORT_EXECUTION_PROPERTIES);
        if (execution.getKey() != null) {
            executionTO.setKey(execution.getKey());
        }
        executionTO.setReport(execution.getReport().getKey());

        return executionTO;
    }
}