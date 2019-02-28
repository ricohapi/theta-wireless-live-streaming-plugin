/************************
 * Regularly update state
 ***********************/

function update() {
    $.ajax({
        url: '/update_status',
        type: 'GET',
        dataType: 'text',
        cache: false,
        timeout: 5000
    })
    .done(function(data) {
        if (data == "1") {
            $("#status_label").text(MESSSAGE_RUNNING);
            if (message_code != data) {
                changeStatusReady();
                message_code = data;
            }
        } else if (data == "2") {
            $("#status_label").text(MESSSAGE_LIVE_STREAMING);
            if (message_code != data) {
                changeStatusLive();
                message_code = data;
            }
        } else if (data == "3") {
            $("#status_label").text(MESSSAGE_STOP_STREAMING);
            if (message_code != data) {
                changeStatusReady();
                message_code = data;
            }
        } else if (data == "4") {
            $("#status_label").text(MESSSAGE_ERROR_CONNECT_SERVER);
            if (message_code != data) {
                changeStatusError();
                message_code = data;
            }
        } else if (data == "5") {
            $("#status_label").text(MESSSAGE_ERROR_NOT_USER_SETTING);
            if (message_code != data) {
                changeStatusError();
                message_code = data;
            }
        } else if (data == "6") {
            $("#status_label").text(MESSSAGE_TIMEOUT);
            if (message_code != data) {
                changeStatusError();
                message_code = data;
            }
        } else if (data == "7") {
            $("#status_label").text(MESSSAGE_ERROR_INITIALIZATION);
            if (message_code != data) {
                changeStatusError();
                message_code = data;
            }
        } else {
            $("#status_label").text(MESSSAGE_ERROR_UNEXPECTED + data);
            if (message_code != data) {
                changeStatusError();
                message_code = data;
            }
        }
    });
}