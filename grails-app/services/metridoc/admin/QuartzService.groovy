package metridoc.admin

import grails.plugin.quartz2.InvokeMethodJob
import org.apache.commons.lang.StringUtils
import org.quartz.JobDataMap

import static org.quartz.JobBuilder.newJob
import static org.quartz.TriggerBuilder.newTrigger
import org.quartz.Trigger
import static org.quartz.TriggerKey.*

class QuartzService {

    static final MAX_LIMIT = 100
    static final MAX_MINIMUM = 10
    def grailsApplication
    def triggersByName = [:]
    def quartzScheduler
    def jobDataByName = [:]
    def jobsByName = [:]

    def scheduleJobs() {
        loadAllJobInfo()
        triggersByName.each {name, trigger ->
            quartzScheduler.scheduleJob(jobsByName[name], trigger)
        }
    }

    private loadAllJobInfo() {
        loadJobData()
        loadJobs()
        loadTriggers()
    }

    private loadTriggers() {
        doWorkflowClassesIteration {name ->
            def schedule = grailsApplication.config.metridoc.scheduling.workflows."$name".schedule
            def startNow = grailsApplication.config.metridoc.scheduling.workflows."$name".startNow

            if(schedule) {
                def triggerBuilder = newTrigger()
                        .withIdentity("${name}Trigger", "Workflow").withSchedule(schedule)
                if(startNow) {
                    triggerBuilder = triggerBuilder.startNow()
                }
                triggersByName[name] = triggerBuilder.build()
            }
        }
    }

    private doWorkflowClassesIteration(Closure closure) {
        workflowClasses.each {
            def paramsCount = closure.maximumNumberOfParameters
            def unCapName = StringUtils.uncapitalise(it.name)
            if (paramsCount == 2) {
                closure.call(unCapName, it)
            } else {
                closure.call(unCapName)
            }
        }
    }

    private loadJobs() {
        doWorkflowClassesIteration {unCapName ->
            jobsByName[unCapName] = newJob(InvokeMethodJob.class)
                    .withIdentity("${unCapName}Job", "Worflow")
                    .usingJobData(jobDataByName[unCapName])
                    .build()
        }
    }

    private loadJobData() {
        doWorkflowClassesIteration {unCapName, grailsClass ->
            jobDataByName[unCapName] = new JobDataMap(
                    [targetObject: grailsClass, targetMethod: "run"]
            )
        }
    }

    private getWorkflowClasses() {
        grailsApplication.workflowClasses
    }

    def listWorkflows(params) {
        def workflows = []

        doWorkflowClassesIteration {unCapName, grailsClass ->
            workflows << [name: "$grailsClass.name"]
        }

        loadJobDetails(workflows)
        return listWorkflowsWithOffsetAndMax(params, workflows)
    }

    def totalWorkflowCount() {
        workflowClasses.size()
    }

    static getMax(params) {
        def max = params.max

        max = max ? max : MAX_MINIMUM
        max < MAX_LIMIT ? max : MAX_LIMIT
    }

    private loadJobDetails(workflows) {
        workflows.each {
            def name = StringUtils.uncapitalise(it.name)
            def trigger = quartzScheduler.getTrigger(triggerKey("${name}Trigger", "Workflow"))
            it.previousFireTime = "NA"
            it.nextFireTime = "NA"
            if (trigger) {
                it.nextFireTime = trigger.nextFireTime.format("yyyy/MM/dd hh:mm:ss")
                def previousFireTime = trigger.previousFireTime
                if(previousFireTime) {
                    it.previousFireTime = previousFireTime.format("yyyy/MM/dd hh:mm:ss")
                }
            }
        }
    }

    private static listWorkflowsWithOffsetAndMax(params, workflows) {
        def ordered = listOrderedWorkflows(params, workflows)
        def offset = params.offset ? params.offset : 0
        def to = Math.min(getMax(params) + offset, workflows.size())

        ordered.subList(offset, to)
    }

    private static listOrderedWorkflows(params, workflows) {
        def result = []
        def order = params.order

        if (order) {
            def map = new TreeMap()
            workflows.each {
                map.put(it.name, it)
            }

            result.addAll map.values()

            if (order == "desc") {
                result = result.reverse()
            }
        } else {
            result = workflows
        }

        return result
    }


}
