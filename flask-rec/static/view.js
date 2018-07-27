$(function() {
    setInterval(update, 4000);
});

function update() {
    let vm = $('#vm').val();
    $.get('/view/' + vm + '/data', function(data) { apply_data(vm, data); });
}

function apply_data(vm, data) {
    $('#last-update').text(data.last_update);
    $('#total-events').text(data.total_events);
}
