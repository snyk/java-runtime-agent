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

    h.write_update(
        'filter.pre-1.paths=demo/CalledFromExecutor#run\n'
        'filter.pre-1.artifact = maven:ignored:ignored\n'
        'filter.pre-1.version = [3,5)\n'
    )

    victim = subprocess.Popen([
        'java',
        '-javaagent:{}=file://{}'.format(
            path.join(os.getcwd(), 'build/libs/snyk-java-runtime-agent.jar'),
            h.config_path
        ),
        '-jar',
        'e2e/repeat-action/build/libs/repeat-action.jar'
    ])

    # wait for natural shutdown
    victim.wait(5)

    actual = set(e['methodEntry']['methodName'] for e in h.all_seen_events())
    expected = {'demo/CalledFromExecutor#run()V'}
    print('  actual: {}'.format(actual))
    print('expected: {}'.format(expected))
    assert expected == actual

    print('Success!')


if '__main__' == __name__:
    main()
