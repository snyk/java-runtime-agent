// oh if only this was streaming

$(function() {
    update();
    setInterval(update, 1500);
});

function update() {
    let vm = $('#vm').val();
    $.get('/view/' + vm + '/data', function(data) { apply_data(vm, data); });
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
            tr.append($('<td>').html(part.replace(/[.$A-Z]/g, '&#8203;$&').replace(/</g, '&lt;')));
        }

        recent_called.append(tr);
    }
}
