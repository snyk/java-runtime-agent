## SECURITY-914 / CVE-2018-1999002

Python script: exploit

https://snyk.io/vuln/SNYK-JAVA-ORGJENKINSCIMAIN-32434

Proposed filter:

```
filter.stapler.artifact = maven:org.kohsuke.stapler:stapler
filter.stapler.version = << 1.250 || << 1.254
filter.stapler.paths = org/kohsuke/stapler/Stapler$LocaleDrivenResourceSelector#open
```

..how would that version expression really look?

Fix version: 1.250.1
Vulnerable: 1.250

Fix version: 1.254.1
Vulnerable: 1.254


https://github.com/jenkinsci/jenkins/commit/29ca81dd59c255ad633f1bd86cf1be40a5f02c64

Fix: https://github.com/stapler/stapler/commit/8e9679b08c36a2f0cf2a81855d5e04e2ed2ac2b3
Code in: https://github.com/stapler/stapler/blob/8e9679b08c36a2f0cf2a81855d5e04e2ed2ac2b3/core/src/main/java/org/kohsuke/stapler/Stapler.java#L343
Called by (eventually): https://github.com/stapler/stapler/blob/8e9679b08c36a2f0cf2a81855d5e04e2ed2ac2b3/core/src/main/java/org/kohsuke/stapler/Stapler.java#L214 as "openResourcePathByLocale".

Will be hit any time jenkins serves a static file?

Vulnerable if Stapler is being used at all?



## SECURITY-897 / CVE-2018-1999001

Exploitable if this line is reached, but unfortunately it's not a method
https://github.com/jenkinsci/jenkins/blame/7d29d4df37ae5602fed41430e8a67eedbe76889f/core/src/main/java/hudson/model/User.java#L478

