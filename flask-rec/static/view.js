// oh if only this was streaming

$(function() {
    update();
    setInterval(update, 1500);
});

function update() {
    let vm = $('#vm').val();
    $.get('/view/' + vm + '/data', function(data) { apply_data(vm, data); });
}

function process_name(part) {
    return part
        .replace(/&/g, '&amp;')
        .replace(/[.$A-Z]/g, '&#8203;$&')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

function apply_data(vm, data) {
    $('#last-update').text(data.last_update);
    $('#total-events').text(data.total_events);
    var recent_called = $('#recent-called');
    recent_called.empty();
    for (var line of data.newest_method_entries) {
        var parts = line.split(':', 2);

        var tr = $('<tr>');

        for (var part of parts) {
            tr.append($('<td>').html(process_name(part)));
        }

        recent_called.append(tr);
    }

    var recent_called = $('#recent-loaded');
    recent_called.empty();
    for (var line of data.newest_dynamic_loads) {
        var parts = line.split(':', 6);

        var tr = $('<tr>');

        tr.append($('<td>').html(process_name(parts[5])));
        tr.append($('<td>').html(process_name(parts[0])));
        tr.append($('<td>').html(process_name(parts[1])));

        recent_called.append(tr);
    }
}
