//TODO: add check for actual changes..... don't make the update button appear unless something actually did happen
function makeUpdateButtonAppear() {
    $('#updateScheduleBtn').removeAttr("disabled")
}
$('#availableSchedules').change(makeUpdateButtonAppear);
$('#arguments').keypress(makeUpdateButtonAppear);
$('#description').change(makeUpdateButtonAppear);
$('#customCron').keypress(makeUpdateButtonAppear);

function editorChange() {
    makeUpdateButtonAppear()
}

//override the groovy based one so we can hook into change events
editor = CodeMirror.fromTextArea(document.getElementById('code'), {
    mode: 'groovy',
    lineNumbers: true,
    matchBrackets: true,
    onKeyEvent: editorChange
});

$('#editDescription').tooltip();

//handle custom cron
$('#availableSchedules').change(doCronToggle);

function doCronToggle() {
    var selectedSchedule = $('#availableSchedules').val()
    if(selectedSchedule === 'CUSTOM_CRON') {
        $('#customCron').toggle(true)
        $('#customCron').attr('required', "true")
    } else {
        $('#customCron').toggle(false)
        $('#customCron').removeAttr('required')
    }
}

$(document).ready(doCronToggle)
