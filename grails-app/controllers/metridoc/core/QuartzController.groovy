package metridoc.core

import grails.converters.JSON
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.text.StrBuilder
import org.quartz.*
import org.quartz.impl.matchers.GroupMatcher

class QuartzController {

    def commonService
    static final String JOB_FAILURE_SCOPE = "jobFailure"
    long NEXT_FIRE_TIME_WHERE_JOB_CONSIDERED_MANUAL = 1000 * 60 * 60 * 24 * 356 * 2 //TWO_YEARS

    static accessControl = {
        role(name: "ROLE_ADMIN")
    }

    static homePage = [
            title: "Job List",
            adminOnly: true,
            description: """
                List of all available jobs and their next scheduled run.  All jobs can be manually ran on this
                page as well.
            """
    ]

    static final Map triggers = [:]

    def quartzScheduler

    /**
     * saves all settings.  Synchronized to avoid any weird behavior from multiple editors submitting at the same time
     */
    synchronized saveSettings() {
        String emails = params.emails
        def badEmails = []
        if (emails) {
            badEmails = NotificationEmails.storeEmails(JOB_FAILURE_SCOPE, emails)
        }

        redirect(action: "index", params: [badEmails: badEmails])
    }

    def index() {
        redirect(action: "list", params: params)
    }

    //TODO: reduce the size of this method, this is getting out of hand
    def list() {
        def jobsList = []
        def listJobGroups = quartzScheduler.getJobGroupNames()
        def manualJob = false
        listJobGroups?.each { jobGroup ->
            quartzScheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup))?.each { jobKey ->
                def triggers = quartzScheduler.getTriggersOfJob(jobKey)
                if (triggers) {
                    triggers.each { Trigger trigger ->
                        def name = trigger.key.name
                        def currentJob = createJob(jobGroup, jobKey.name, jobsList, trigger.key.name)
                        currentJob.trigger = trigger
                        currentJob.triggerStatus = quartzScheduler.getTriggerState(trigger.key)
                        long timeToNextFireTime = trigger.getNextFireTime().time - new Date().time
                        if(timeToNextFireTime > NEXT_FIRE_TIME_WHERE_JOB_CONSIDERED_MANUAL) {
                            manualJob = true
                        }
                        currentJob.manualJob = manualJob
                    }
                } else {
                    createJob(jobGroup, jobKey.name, jobsList)
                }
            }
        }

        def notificationEmails = new StrBuilder()
        NotificationEmails.list().collect { it.email }.each {
            notificationEmails.appendln(it)
        }

        def badEmailMessage = null
        if (params.badEmails) {
            def badEmailMessageBuilder = new StrBuilder("The following emails could not be stored: ")
            if (params.badEmails instanceof String) {
                badEmailMessageBuilder.append(params.badEmails)
                badEmailMessage = badEmailMessageBuilder.toString()
            } else {
                params.badEmails.each {
                    badEmailMessageBuilder.append(it)
                    badEmailMessageBuilder.append(", ")
                }
                badEmailMessage = badEmailMessageBuilder.toString()
                badEmailMessage = StringUtils.chop(badEmailMessage)
                badEmailMessage = StringUtils.chop(badEmailMessage)
            }
        }

        if (NotificationEmails.count() == 0) {
            badEmailMessage = "No emails have been set, no one will be notified of job failures"
        }




        return [
                jobs: jobsList,
                now: new Date(),
                scheduler: quartzScheduler,
                emailIsConfigured: commonService.emailIsConfigured(),
                notificationEmails: notificationEmails,
                badEmailMessage: badEmailMessage,

        ]
    }

    def start() {
        def trigger = triggers.get(params.jobName)
        quartzScheduler.scheduleJob(trigger)
        redirect(action: "list")
    }

    def pause() {
        quartzScheduler.pauseJob(jobKey(params.jobName, params.jobGroup))
        redirect(action: "list")
    }

    def resume() {
        quartzScheduler.resumeJob(jobKey(params.jobName, params.jobGroup))
        redirect(action: "list")
    }

    def runNow() {
        def dataMap = new JobDataMap(params)
        def triggerKey


        if (params.triggerName) {
            triggerKey = new TriggerKey(params.triggerName)

        } else {
            def jobKey = new JobKey(params.jobName, params.jobGroup)
            def triggers = quartzScheduler.getTriggersOfJob(jobKey)
            triggerKey = triggers[0].key
        }

        dataMap.oldTrigger = quartzScheduler.getTrigger(triggerKey)
        def now = new Date()

        def end = now + 365 * 5 //5 years in the future

        //TODO: can we do something else here?  This is a horrendous hack
        //next run will be 4 years in the future, clearly this won't run by then and the job will be unscheduled
        //the 5 year and 4 year number are arbitrary, basically we are tricking quartz to keep the job around so we can
        //continue to retrieve statistics from it.
        def schedule = SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1 * 24 * 56 * 4).repeatForever()
        def trigger = TriggerBuilder.newTrigger().forJob(params.jobName, params.jobGroup).startAt(new Date())
                .endAt(end).withIdentity(triggerKey).withSchedule(schedule).usingJobData(dataMap).build()

        quartzScheduler.rescheduleJob(triggerKey, trigger)
        Thread.sleep(1000) //sleep for 2 seconds to allow for the job to actually start


        if (params.containsKey("returnTriggerId")) {
            render trigger.key.name
        } else {
            redirect(action: "list")
        }
    }

    def status() {
        String triggerId = params.id
        if (triggerId) {
            def jobInfo = QuartzMonitorJobFactory.jobRuns[triggerId]
            if (jobInfo) {
                render jobInfo as JSON
            } else {
                render(status: 404, text: "job with trigger $triggerId does not exist")
            }
        } else {
            render QuartzMonitorJobFactory.jobRuns as JSON
        }
    }

    def startScheduler() {
        quartzScheduler.start()
        redirect(action: "list")
    }

    def stopScheduler() {
        quartzScheduler.standby()
        redirect(action: "list")
    }

    private def createJob(String jobGroup, String jobName, ArrayList jobsList, triggerName = "") {
        def currentJob = [:]
        currentJob.group = jobGroup
        currentJob.name = jobName
        def map = QuartzMonitorJobFactory.jobRuns.get(triggerName)
        if (map) currentJob << map
        jobsList.add(currentJob)
        return currentJob
    }

    private jobKey(name, group) {
        return JobKey.jobKey(name, group)
    }
}
