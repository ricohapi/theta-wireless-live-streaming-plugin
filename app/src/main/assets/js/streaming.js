/************************
 * Starting and stopping streaming
 ***********************/

function streaming() {
    $.ajax({
        url: '/start_streaming',
        type: 'POST',
        dataType: 'text',
        cache: false,
        timeout: 5000
    })
}