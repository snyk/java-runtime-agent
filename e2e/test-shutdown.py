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
reportIntervalMs=100000000
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

    # We're expecting the startup to take between 40ms and 6s. We've seen
    # 6s in the wild, e.g. on OSX resolving host-names. The app will produce
    # events for 10s after startup, so we'd expect between 2.5 and 8.5s of
    # events from the app. We only need 0.5s, so this is a pretty good safety
    # margin.
    sleep(7.5)

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
