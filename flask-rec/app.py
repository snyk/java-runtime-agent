import collections
from datetime import datetime
from typing import Iterable, List, Tuple, Dict

from flask import Flask, request, Response, render_template, abort, jsonify

app = Flask(__name__)

MethodEntryLine = str
LoadClassLine = List[str]


class VmInfo:
    def __init__(self):
        self.messages = []  # type: List[Tuple[datetime, Messages]]


class Messages:
    def __init__(self, method_entries: List[MethodEntryLine], load_classes: List[LoadClassLine]):
        self.method_entries = method_entries
        self.load_classes = load_classes


VmName = str

# type: Dict[VmName, VmInfo]
database = collections.defaultdict(VmInfo)


@app.route('/')
def index():
    return render_template('index.html', vms=sorted(database.keys()))


@app.route('/dump', methods=['PUT'])
def dump():
    save(request.data.decode('utf-8').strip().split('\n'))
    return 'cheers'


@app.route('/view/<vm>/')
def view_vm(vm: str):
    if vm not in database:
        abort(404)

    messages = database[vm].messages

    first_seen = messages[-1][0]

    return render_template('view.html', vm=vm, first_seen=first_seen)


@app.route('/view/<vm>/data')
def view_vm_data(vm: str):
    if vm not in database:
        abort(404)

    messages = database[vm].messages
    when, _ = messages[0]

    return jsonify({
        'last_update': when,
        'total_events': len(messages),
    })


@app.route('/export-latest')
def export_latest():
    def dump():
        for whom, info in database.items():
            yield '\n\n##### vm: {}\n'.format(whom)

            for when, messages in info.messages[:min(10, len(info.messages))]:
                yield '\n\n  ### {}: called methods\n'.format(when)

                for line in sorted(messages.method_entries):
                    yield '    * {}\n'.format(line)

                yield '\n\n  ### {}: dynamic loading\n'.format(when)

                for parts in messages.load_classes:
                    yield '    * {}\n'.format(parts[0])
                    for part in sorted(parts[1:]):
                        yield '      - {}\n'.format(part)

    return Response(dump(), mimetype='text/plain')


def save(lines: Iterable[str]):
    method_entries = []
    load_classes = []
    vm_info = None
    hostname = None
    for line in lines:
        spaced = line.split(' ')
        if 0 == len(spaced):
            continue
        first = spaced[0]
        if first.startswith('v:'):
            vm_info = first[2:]
        elif first.startswith('h:'):
            hostname = first[2:]
        elif first.startswith('e:'):
            method_entries.append(first)
        elif first.startswith('c:'):
            load_classes.append(spaced)
        else:
            raise Exception('invalid prefix: ' + first)

    if vm_info:
        if hostname and not vm_info.endswith(hostname):
            vm_info += '@' + hostname
    elif hostname:
        vm_info = 'unknown@' + hostname
    else:
        raise Exception('no source information')

    when = datetime.utcnow()
    database[vm_info].messages.insert(0, (when, Messages(method_entries, load_classes)))
