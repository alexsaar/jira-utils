#!/usr/bin/env groovy

import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON

import groovy.util.CliBuilder
import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7')

def cli = new CliBuilder(usage:'./jslurp [options]', header:'Options:')
cli.with {
	f longOpt: 'file', args: 1, argName: 'file', "file to slurp (defaults to issues.txt)"
	u longOpt: 'usr', args: 1, argName: 'usr', "JIRA user name", required: true
	p longOpt: 'pwd', args: 1, argName: 'pwd', "JIRA password", required: true
	s longOpt: 'svr', args: 1, argName: 'svr', "JIRA server", required: true
}

def options = cli.parse(args)
if (!options) return
else slurp(options)

def slurp(opts) {
	File f = new File(opts.file ? opts.file : "issues.txt")
	if(!f.exists()) { println "${f} does not exist" }
	else {
		println "processing ${f}"

        def issues = []

        def issue = [:]
		f.eachLine { line ->
			line = line.trim()
			if (line && !line.startsWith("#")) {
				def (key, value) = line.split(':').collect { it.trim() }
				issue[key] = value
			} else if (!line) {
				if(issue) {
                    issues << createBody(issue)
					issue = [:]
				}
			}
		}
	    if(issue) {
            issues << createBody(issue)
	    }
        def jira = getClient(opts)
        issues.each { i ->
            jira.request(POST, JSON) { req ->
                uri.path = "issue/"
                body = i
                response.success = { resp, data ->
                    println "Created ${data.key}"
                }
                response.failure = { resp ->
                    println "Failed to create issue (${resp.status})"
                }
            }
        }
	}
}

def GString createBody(issue) {
    def comps = "";

    issue['c'].split(',').collect { it.trim() }.eachWithIndex { item, index ->
        comps += index == 0 ? "" : ",";
        comps += "{ \"name\": \"${item}\" }"
    }
    "{ \"fields\": { \"project\": { \"key\":\"${issue['p']}\" }, \"summary\":\"${issue['s']}\", \"description\":\"${issue['d']}\", \"issuetype\": { \"name\": \"${issue['t']}\" }, \"priority\": { \"name\": \"${issue['prio']}\" }, \"fixVersions\": [ { \"name\": \"${issue['fv']}\" } ], \"components\": [ ${comps} ] } }"
}

def getClient(opts) {
    println "Connecting to ${opts.svr} ..."

    def jira = new HTTPBuilder(opts.svr + "/rest/api/latest/");
    jira.client.addRequestInterceptor(new HttpRequestInterceptor() {
        void process(HttpRequest req, HttpContext ctx) {
            req.addHeader('Authorization', "Basic " + "${opts.usr}:${opts.pwd}".bytes.encodeBase64().toString())
        }
    })

    def serverInfo = jira.get(path: 'serverInfo')
    println "Connected to ${serverInfo.serverTitle} (version ${serverInfo.version})\n"

    return jira
}
