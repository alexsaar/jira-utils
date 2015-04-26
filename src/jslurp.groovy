#!/usr/bin/env groovy

import groovy.util.CliBuilder

D_FILE = "issues.txt"

enum FieldNames {
	p ("project"), t ("type"), prio ("priority"), e ("epic"), l ("labels"), 
	c ("components"), s ("summary"), d ("description"), fix ("fixVersions");

    private final String name;       
    private FieldNames(String s) { name = s; }
    public String toString() { return name; }
	public static boolean contains(String s) {
		for (FieldNames name:values()) {
			if (name.name().equals(s)) return true;
		}
		return false;
	}
}

cli = new CliBuilder(usage:'./jslurp [options]', header:'Options:')
cli.with {
	h longOpt: 'help', 'Show usage information'
	d longOpt: 'dry', args: 0, "dry run"
	f longOpt: 'file', args: 1, argName: 'file', "file to slurp (defaults to ${D_FILE})"
	u longOpt: 'usr', args: 1, argName: 'usr', "JIRA user name"
	p longOpt: 'pwd', args: 1, argName: 'pwd', "JIRA password"
	s longOpt: 'svr', args: 1, argName: 'svr', "JIRA server"
}

opts = cli.parse(args)

if (!opts || opts.help) println cli.usage()
else slurp(opts)

def slurp(opts) {
	File f = new File(opts.file ? opts.file : D_FILE)
	if(!f.exists()) { println "${f} does not exist" }
	else {
		println "processing ${f}"
		def issues = []
		
		def issue = [:]
		f.eachLine { line ->
			line = line.trim()
			if (line && !line.startsWith("#")) {
				def (key, value) = line.split(':').collect { it.trim() }
				issue[FieldNames.contains("$key") ? FieldNames.valueOf("$key") : "$key"] = value
			} else if (!line) {
				if(issue) {
					issues << issue
					issue = [:]
				}
			}
		}
	    if(issue) {
	      issues << issue
	    }
		
		// build credentials arg
		creds = []
		if (opts.usr) creds << "--user" << opts.u
		if (opts.pwd) creds << "--password" << opts.p
		if (opts.svr) creds << "--server" << opts.s
		
		issues.eachWithIndex { it, index ->
			cmd = []
			it.each { k, v -> cmd << "--$k" << "$v" }
			
			if (opts.dry) println cmd
			else {
				p = (["jira"] + creds + ["--action", "createIssue", "--autoComponent"] + cmd).execute(); p.waitFor()
				if (p.exitValue()) print p.err.text
				else print p.text
			}
		}
	}
}
