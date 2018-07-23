from datetime import datetime
from typing import Dict, Set, Iterable, List, Tuple

from flask import Flask, request, Response

app = Flask(__name__)

# type: List[Tuple[datetime, Set[str]]]
world_state = []


@app.route('/')
def hello_world():
    return 'Hello, World!'


@app.route('/dump', methods=['PUT'])
def dump():
    save(request.data.decode('utf-8').strip().split('\n'))
    return 'cheers'


@app.route('/report')
def report():
    def dump():
        for when, what in world_state[:min(10, len(world_state))]:
            yield '\n\n### {} ###\n'.format(when)
            if 0 == len(what):
                yield ' *** NOTHING ***'
                continue

            for line in sorted(what):
                yield ' * {}\n'.format(line)

    return Response(dump(), mimetype='text/plain')


def save(lines: Iterable[str]):
    world_state.insert(0, (datetime.utcnow(), set(line for line in lines if 0 != len(line))))
