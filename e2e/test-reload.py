#!/usr/bin/env python3
import json
import os
import re
import subprocess
import tempfile
from os import path
from time import sleep
from typing import Iterable


def main():
    d = tempfile.TemporaryDirectory()
    project_id = '0153525f-5a99-4efe-a84f-454f12494033'
    CONFIG = """
projectId={}
homeBaseUrl=file://{}/
logTo=stderr

debugLoggingEnabled=true

startupDelayMs=10
filterUpdateIntervalMs=800
heartBeatIntervalMs=500
reportIntervalMs=1000
""".format(project_id, d.name)
    config_path = path.join(d.name, 'snyk.properties')
    with open(config_path, 'w') as f:
        f.write(CONFIG)

    victim = subprocess.Popen([
        'java',
        '-javaagent:{}=file://{}'.format(
            path.join(os.getcwd(), 'build/libs/snyk-java-runtime-agent.jar'),
            config_path
        ),
        '-jar',
        'e2e/repeat-action/build/libs/repeat-action.jar'
    ])

    # assuming here it completes startup and the first 500ms in this time
    # should be pretty safe, there's no real reason startup should take over 40ms
    sleep(1.5)

    # shouldn't've seen any events yet
    assert [] == list(all_seen_events(d.name))

    update_dir = path.join(d.name, 'snapshot/{}'.format(project_id))
    os.makedirs(update_dir)
    update_file = path.join(update_dir, 'java')

    expected_paths = [
        'demo/CalledFromExecutor#run',
        'demo/ConstructedFromExecutor#foo',
        'demo/LoopWithSleep#foo',
    ]

    also_try = [
        'demo/LoopWithSleep#run',
    ]

    all_paths = expected_paths + also_try
    new_filters = ''
    for i, wanted in enumerate(all_paths):
        new_filters += 'filter.up-{}.paths = {}\n'.format(i, wanted)

    with open(update_file, 'w') as f:
        f.write(new_filters)
    print('>>> update written to {}'.format(update_file))

    # test app should run for >2 more seconds, so >4 more reloads and reports
    victim.wait(10)

    seen_methods = set()
    for entry in all_seen_events(d.name):
        seen_methods.add(re.sub(r'\(.*', '', entry['methodEntry']['methodName']))

    print('    seen: {}'.format(sorted(seen_methods)))
    print('expected: {}'.format(sorted(all_paths)))
    assert set(expected_paths).issubset(seen_methods)

    print('Success!')


def all_seen_events(out_dir: str) -> Iterable:
    for name in os.listdir(out_dir):
        if not name.startswith('post-'):
            continue
        name = path.join(out_dir, name)
        with open(name) as f:
            doc = json.load(f)
        if 'eventsToSend' in doc:
            yield from doc['eventsToSend']


if '__main__' == __name__:
    main()
