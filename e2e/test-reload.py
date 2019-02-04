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

    # We're expecting the startup to take between 40ms and 6s. We've seen
    # 6s in the wild, e.g. on OSX resolving host-names. The app will produce
    # events for 10s after startup, so we'd expect between 2.5 and 8.5s of
    # events from the app. We only need 0.5s, so this is a pretty good safety
    # margin.
    sleep(7.5)

    # shouldn't've seen any events yet
    assert [] == list(all_seen_events(d.name))

    update_dir = path.join(d.name, 'v2', 'snapshot/{}'.format(project_id))
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

    new_filters = ''
    for i, wanted in enumerate(expected_paths + also_try):
        new_filters += 'filter.up-{}.paths = {}\n'.format(i, wanted)

    with open(update_file, 'w') as f:
        f.write(new_filters)
    print('>>> update written to {}'.format(update_file))

    # test app should run for >2 more seconds, so >4 more reloads and reports
    victim.wait(4)

    seen_methods = set()
    for entry in all_seen_events(d.name):
        assert entry['methodEntry']['filterName'].startswith('up-')
        seen_methods.add(re.sub(r'\(.*', '', entry['methodEntry']['methodName']))

    print('    seen: {}'.format(sorted(seen_methods)))
    print('expected: {}'.format(sorted(expected_paths)))
    assert seen_methods == set(expected_paths)

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
