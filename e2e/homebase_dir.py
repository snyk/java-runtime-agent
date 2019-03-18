import json
import os
import tempfile
from os import path
from typing import Iterable, Dict, Any


class HomebaseMock:
    def __init__(self, config_template: str):
        self.project_id = '0153525f-5a99-4efe-a84f-454f12494034'
        self.d = tempfile.TemporaryDirectory()
        self.config_path = path.join(self.d.name, 'snyk.properties')
        update_dir = path.join(self.d.name, 'v2', 'snapshot/' + self.project_id)
        os.makedirs(update_dir)
        self.update_file = path.join(update_dir, 'java')

        with open(self.config_path, 'w') as f:
            f.write(config_template
                    .replace('OUTPUT_PATH', self.d.name)
                    .replace('PROJECT_ID', self.project_id))

    def write_update(self, update_content: str) -> None:
        with open(self.update_file, 'w') as f:
            f.write(update_content)
        print('>>> update written')

    def all_seen_docs(self) -> Iterable[Dict[str, Any]]:
        for name in os.listdir(self.d.name):
            if not name.startswith('post-'):
                continue
            name = path.join(self.d.name, name)
            with open(name) as f:
                doc = json.load(f)
            yield doc

    def all_seen_events(self) -> Iterable[Dict[str, Any]]:
        for doc in self.all_seen_docs():
            if 'eventsToSend' in doc:
                yield from doc['eventsToSend']
