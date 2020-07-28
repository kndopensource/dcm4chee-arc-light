/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *  Java(TM), hosted at https://github.com/dcm4che.
 *
 *  The Initial Developer of the Original Code is
 *  J4Care.
 *  Portions created by the Initial Developer are Copyright (C) 2015-2020
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *  See @authors listed below
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 2 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 *
 */

package org.dcm4chee.arc.retrieve.mgt.impl;

import org.dcm4chee.arc.Scheduler;
import org.dcm4chee.arc.conf.ArchiveDeviceExtension;
import org.dcm4chee.arc.conf.Duration;
import org.dcm4chee.arc.conf.QueueDescriptor;
import org.dcm4chee.arc.conf.ScheduleExpression;
import org.dcm4chee.arc.entity.RetrieveTask;
import org.dcm4chee.arc.retrieve.mgt.RetrieveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Vrinda Nayak <vrinda.nayak@j4care.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since Jan 2020
 */
@ApplicationScoped
public class RetrieveScheduler extends Scheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RetrieveScheduler.class);

    @Inject
    private RetrieveManager mgr;

    protected RetrieveScheduler() {
        super(Mode.scheduleWithFixedDelay);
    }

    @Override
    protected Logger log() {
        return LOG;
    }

    @Override
    protected Duration getPollingInterval() {
        ArchiveDeviceExtension arcDev = device.getDeviceExtension(ArchiveDeviceExtension.class);
        return arcDev.getRetrieveTaskPollingInterval();
    }

    @Override
    protected void execute() {
        ArchiveDeviceExtension arcDev = device.getDeviceExtension(ArchiveDeviceExtension.class);
        int fetchSize = arcDev.getRetrieveTaskFetchSize();
        List<RetrieveTask.PkAndQueueName> retrieveTasksToSchedule;
        Set<String> suspendedQueues = suspendedQueues(arcDev);
        do {
            retrieveTasksToSchedule = mgr.findRetrieveTasksToSchedule(fetchSize, suspendedQueues);
            HashSet<String> queueSizeLimitExceeded = new HashSet<>();
            for (RetrieveTask.PkAndQueueName pkAndQueueName : retrieveTasksToSchedule) {
                if (queueSizeLimitExceeded.contains(pkAndQueueName.queueName)) continue;
                try {
                    if (!mgr.scheduleRetrieveTask(pkAndQueueName.retrieveTaskPk)) {
                        queueSizeLimitExceeded.add(pkAndQueueName.queueName);
                        suspendedQueues.add(pkAndQueueName.queueName);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to schedule RetrieveTask[pk={}}]\n:", pkAndQueueName.retrieveTaskPk, e);
                }
            }
        }
        while (getPollingInterval() != null && retrieveTasksToSchedule.size() == fetchSize);
    }

    private static Set<String> suspendedQueues(ArchiveDeviceExtension arcDev) {
        Calendar now = Calendar.getInstance();
        return Stream.of(
                "Retrieve1",
                "Retrieve2",
                "Retrieve3",
                "Retrieve4",
                "Retrieve5",
                "Retrieve6",
                "Retrieve7",
                "Retrieve8",
                "Retrieve9",
                "Retrieve10",
                "Retrieve11",
                "Retrieve12",
                "Retrieve13")
                .filter(queueName -> !isQueueActive(arcDev.getQueueDescriptor(queueName), now))
                .collect(Collectors.toSet());
    }

    private static boolean isQueueActive(QueueDescriptor queueDescriptor, Calendar now) {
        return queueDescriptor != null && ScheduleExpression.emptyOrAnyContains(now, queueDescriptor.getSchedules());
    }
}
