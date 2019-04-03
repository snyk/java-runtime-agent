#!/usr/bin/env python3
import os
import re
import subprocess
from os import path
from time import sleep

import homebase_dir


def main():
    h = homebase_dir.HomebaseMock("""
projectId=PROJECT_ID
homeBaseUrl=file://OUTPUT_PATH/
logTo=stderr

debugLoggingEnabled=true

startupDelayMs=10
filterUpdateIntervalMs=800
heartBeatIntervalMs=500
reportIntervalMs=1000
""")

    victim = subprocess.Popen([
        'java',
        '-javaagent:{}=file://{}'.format(
            path.join(os.getcwd(), 'build/libs/snyk-java-runtime-agent.jar'),
            h.config_path
        ),
        '-jar',
        'e2e/repeat-action/build/libs/repeat-action.jar'
    ])

    # app outputs events for 4 seconds, and takes under a second to start
    sleep(1)

    # shouldn't've seen any events yet
    assert [] == list(h.all_seen_events())

    expected_paths = [
        'demo/CalledFromExecutor#run',
        'demo/ConstructedFromExecutor#foo',
        'demo/LoopWithSleep#foo',
    ]

    also_try = [
        'demo/LoopWithSleep#run',
    ]

    new_filters = ''
    for i, wanted in enumerate(expected_paths + also_try):
        new_filters += 'filter.up-{}.paths = {}\n'.format(i, wanted)
        new_filters += 'filter.up-{}.artifact = maven:ignored:ignored\n'.format(i)
        new_filters += 'filter.up-{}.version = [1,3)\n'.format(i)

    h.write_update(new_filters)

    # test app should run for >2 more seconds, so >4 more reloads and reports
    victim.wait(4)

    seen_methods = set()
    for entry in h.all_seen_events():
        seen_methods.add(re.sub(r'\(.*', '', entry['methodEntry']['methodName']))

    print('    seen: {}'.format(sorted(seen_methods)))
    print('expected: {}'.format(sorted(expected_paths)))
    assert seen_methods == set(expected_paths)

    print('Success!')


if '__main__' == __name__:
    main()
