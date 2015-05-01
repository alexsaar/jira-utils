#JIRA Tools
Set of tools for making working with JIRA easier

## jslurp
JIRA Sluper imports JIRA issues from text files.

### Dependencies
* Groovy

### Installation
* `brew tap alexsaar/taps`
* `brew install Jslurp`
* `jslurp -h`

### Text format example
See below for a sample of how to define an issue. Multiple issues can be defined in one text file separated via blank lines. 

```
# a comment that will not be processed
p:ACME
t:Improvement
prio:Minor
l:refactor,kown-issue
c:Sample
s:Slurper Test
d:some description
fv:1.0
```

Supported fields are:

* p: project key (required)
* t: issue type
* prio: priority
* s: summary (required)
* d: description
* fv: fix version (multi value)
* l: labels (multi value)
* c: components (multi value)