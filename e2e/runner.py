import json
import os
import tempfile
from os import path
from typing import Iterable


def config(project_id: str, report_interval_ms=1000):
    d = tempfile.TemporaryDirectory()
    CONFIG = """
projectId={}
homeBaseUrl=file://{}/
logTo=stderr

debugLoggingEnabled=true

startupDelayMs=10
filterUpdateIntervalMs=800
heartBeatIntervalMs=500
reportIntervalMs={}
    """.format(project_id, d.name, report_interval_ms)
    config_path = path.join(d.name, 'snyk.properties')

    with open(config_path, 'w') as f:
        f.write(CONFIG)

    agent_arg = '-javaagent:{}=file://{}'.format(
        path.join(os.getcwd(), 'build/libs/snyk-java-runtime-agent.jar'),
        config_path
    )

    return d, agent_arg


def all_seen_events(out_dir: str) -> Iterable:
    for name in os.listdir(out_dir):
        if not name.startswith('post-'):
            continue
        name = path.join(out_dir, name)
        with open(name) as f:
            doc = json.load(f)
        if 'eventsToSend' in doc:
            yield from doc['eventsToSend']
