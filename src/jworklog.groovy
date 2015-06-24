#!/usr/bin/env groovy

/**
 * Uses JIRA REST API: https://docs.atlassian.com/software/jira/docs/api/REST/latest/
 */
import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.protocol.HttpContext
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.format.DateTimeFormat

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7')
@Grab(group='joda-time', module='joda-time', version='2.3')

class Constants {
    static DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	static SIMPLE_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd")
}

def cli = new CliBuilder(usage:'./IWMetrics.groovy [options]', header:'Options:')
cli.h('print this message')
cli.j(args:1, argName:'jira', 'JIRA Server', required:true)
cli.u(args:1, argName:'usr', 'JIRA User', required:true)
cli.p(args:1, argName:'pwd', 'JIRA Password', required:true)
cli.s(args:1, argName:'start', 'Start Date (YYYY-MM-DD)', required:true)
cli.e(args:1, argName:'end', 'End Date (YYYY-MM-DD)', required:true)
cli.o(args:1, argName:'org', 'Organisation (e.g. ORG-ASAAR-ALL)', required:true)

def options = cli.parse(args)
if (options == null) {
    return
}

def jira = getClient(options)


def jql = "worklogDate > ${options.s} and worklogDate < ${options.e} and worklogAuthor in (membersOf(${options.o}))"

def start = Constants.SIMPLE_DATE_FORMAT.parseDateTime(options.s).toLocalDate()
def end = Constants.SIMPLE_DATE_FORMAT.parseDateTime(options.e).toLocalDate()

jira.get(path: 'search', query:[ jql:jql ]) { rsearch, result ->
	def times = [:]
	
    result.issues.each { issue -> 
		jira.get(path: "issue/${issue.key}/worklog") { rlog, worklog ->
		    worklog.worklogs.each { log -> 
				
				def updated = Constants.DATE_FORMAT.parseDateTime(log.updated).toLocalDate()
				if (updated.isBefore(end) && updated.isAfter(start)) {
					println "${issue.key} - ${log.author.name} - ${updated} - ${log.timeSpentSeconds} sec"
					
					if (times[log.author.name]) times[log.author.name] = times[log.author.name] + log.timeSpentSeconds
					else times[log.author.name] = log.timeSpentSeconds
				}
			}
		}
	}
	
	println "ID,Seconds"
	times.each { k, v ->
		println "${k},${v}"
	}
}


/**
 * Create JIRA client.
 */
def getClient(opts) {
    println "Connecting to ${opts.j} ..."

    def jira = new HTTPBuilder(opts.j + "/rest/api/latest/");
    jira.client.addRequestInterceptor(new HttpRequestInterceptor() {
        void process(HttpRequest req, HttpContext ctx) {
            req.addHeader('Authorization', "Basic " + "${opts.u}:${opts.p}".bytes.encodeBase64().toString())
        }
    })

    def serverInfo = jira.get(path: 'serverInfo')
    println "Connected to ${serverInfo.serverTitle} (version ${serverInfo.version})"

    return jira
}
