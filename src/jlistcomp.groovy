#!/usr/bin/env groovy

import groovy.util.CliBuilder
import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7')

cli = new CliBuilder(usage:'./jlistcomp [options]', header:'Options:')
cli.with {
    h longOpt: 'help', 'Show usage information'
    pr longOpt: 'project', args: 1, argName: 'project', "JIRA Project Name"
    o longOpt: 'owner', "Fetch owner name"
    u longOpt: 'usr', args: 1, argName: 'usr', "JIRA user name"
    p longOpt: 'pwd', args: 1, argName: 'pwd', "JIRA password"
    s longOpt: 'svr', args: 1, argName: 'svr', "JIRA server"
}

opts = cli.parse(args)

if (!opts || opts.help) println cli.usage()
else listComp(opts)

def listComp(opts) {
    def jira = getClient(opts)
    jira.get(path: "project/${opts.project}") { resp, project ->
        project.components.each { component ->
            if (opts.o) {
                jira.get(path: "component/${component.id}") { respCompDetails, compDetails ->
                    def lead = compDetails.lead ? compDetails.lead.name : "N/A"
                    println "${component.name},${lead}"
                }
            } else {
                println "${component.name}"
            }
        }
    }
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