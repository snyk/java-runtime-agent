Python script: exploit

https://github.com/jenkinsci/jenkins/commit/29ca81dd59c255ad633f1bd86cf1be40a5f02c64

Fix version: 1.250.1
Vulnerable: 1.250


Fix: https://github.com/stapler/stapler/commit/8e9679b08c36a2f0cf2a81855d5e04e2ed2ac2b3
Code in: https://github.com/stapler/stapler/blob/8e9679b08c36a2f0cf2a81855d5e04e2ed2ac2b3/core/src/main/java/org/kohsuke/stapler/Stapler.java#L343
Called by (eventually): https://github.com/stapler/stapler/blob/8e9679b08c36a2f0cf2a81855d5e04e2ed2ac2b3/core/src/main/java/org/kohsuke/stapler/Stapler.java#L214 as "openResourcePathByLocale".

Will be hit any time jenkins serves a static file?

Vulnerable if Stapler is being used at all?

