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

## snyk test

 *✗ High severity vulnerability found in org.springframework:spring-webmvc
 *  Description: Directory Traversal
 *  Info: https://snyk.io/vuln/SNYK-JAVA-ORGSPRINGFRAMEWORK-32202
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112 > org.springframework:spring-webmvc@2.5.6.SEC03
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112 > org.springframework:spring-webmvc@2.5.6.SEC03
 *
 *✗ High severity vulnerability found in org.kohsuke:libpam4j
 *  Description: Access Restriction Bypass
 *  Info: https://snyk.io/vuln/SNYK-JAVA-ORGKOHSUKE-31583
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112 > org.kohsuke:libpam4j@1.8
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112 > org.kohsuke:libpam4j@1.8
 *
 *✗ High severity vulnerability found in org.jenkins-ci.plugins:script-security
 *  Description: Arbitrary Code Execution
 *  Info: https://snyk.io/vuln/SNYK-JAVA-ORGJENKINSCIPLUGINS-32254
 *  Introduced through: org.jenkins-ci.main:jenkins-test@2.112
 *  From: org.jenkins-ci.main:jenkins-test@2.112 > org.jenkins-ci.plugins:matrix-project@1.4.1 > org.jenkins-ci.plugins:script-security@1.13
 *
 *✗ High severity vulnerability found in org.jenkins-ci.main:jenkins-core
 *  Description: Arbitrary File Write via Archive Extraction (Zip Slip)
 *  Info: https://snyk.io/vuln/SNYK-JAVA-ORGJENKINSCIMAIN-31683
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112
 *
 *✗ High severity vulnerability found in org.jenkins-ci.main:jenkins-core
 *  Description: Authentication Bypass
 *  Info: https://snyk.io/vuln/SNYK-JAVA-ORGJENKINSCIMAIN-32433
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112
 *
 *✗ High severity vulnerability found in org.jenkins-ci.main:jenkins-core
 *  Description: Arbitrary File Read
 *  Info: https://snyk.io/vuln/SNYK-JAVA-ORGJENKINSCIMAIN-32434
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112
 *
 *✗ High severity vulnerability found in org.codehaus.plexus:plexus-utils
 *  Description: Shell Command Injection
 *  Info: https://snyk.io/vuln/SNYK-JAVA-ORGCODEHAUSPLEXUS-31522
 *  Introduced through: org.jenkins-ci.main:jenkins-test@2.112
 *  From: org.jenkins-ci.main:jenkins-test@2.112 > org.jenkins-ci.main:maven-plugin@2.14 > org.apache.maven:maven-core@3.1.0 > org.codehaus.plexus:plexus-utils@3.0.10
 *
 *✗ High severity vulnerability found in commons-fileupload:commons-fileupload
 *  Description: Arbitrary File Write
 *  Info: https://snyk.io/vuln/SNYK-JAVA-COMMONSFILEUPLOAD-30080
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112 > commons-fileupload:commons-fileupload@1.3.1-jenkins-2
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112 > commons-fileupload:commons-fileupload@1.3.1-jenkins-2
 *
 *✗ High severity vulnerability found in commons-fileupload:commons-fileupload
 *  Description: Denial of Service (DoS)
 *  Info: https://snyk.io/vuln/SNYK-JAVA-COMMONSFILEUPLOAD-30081
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112 > commons-fileupload:commons-fileupload@1.3.1-jenkins-2
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112 > commons-fileupload:commons-fileupload@1.3.1-jenkins-2
 *
 *✗ High severity vulnerability found in commons-fileupload:commons-fileupload
 *  Description: Denial of Service (DoS)
 *  Info: https://snyk.io/vuln/SNYK-JAVA-COMMONSFILEUPLOAD-30082
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112 > commons-fileupload:commons-fileupload@1.3.1-jenkins-2
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112 > commons-fileupload:commons-fileupload@1.3.1-jenkins-2
 *
 *✗ High severity vulnerability found in commons-fileupload:commons-fileupload
 *  Description: Arbitrary Code Execution
 *  Info: https://snyk.io/vuln/SNYK-JAVA-COMMONSFILEUPLOAD-30401
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112 > commons-fileupload:commons-fileupload@1.3.1-jenkins-2
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112 > commons-fileupload:commons-fileupload@1.3.1-jenkins-2
 *
 *✗ High severity vulnerability found in commons-beanutils:commons-beanutils
 *  Description: Arbitrary Code Execution
 *  Info: https://snyk.io/vuln/SNYK-JAVA-COMMONSBEANUTILS-30077
 *  Introduced through: org.jenkins-ci.main:jenkins-core@2.112, org.jenkins-ci.main:jenkins-war@2.112
 *  From: org.jenkins-ci.main:jenkins-core@2.112 > commons-beanutils:commons-beanutils@1.8.3
 *  From: org.jenkins-ci.main:jenkins-war@2.112 > org.jenkins-ci.main:jenkins-core@2.112 > commons-beanutils:commons-beanutils@1.8.3
 *
