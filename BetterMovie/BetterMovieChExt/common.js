function createURL(item) {
    return 'http://localhost:8080/bm/bettermovies/' + item;
}

// Create the XHR object.
function createCORSRequest(method, url) {
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr) {
        // XHR for Chrome/Firefox/Opera/Safari.
        xhr.open(method, url, true);
    } else if (typeof XDomainRequest != "undefined") {
        // XDomainRequest for IE.
        xhr = new XDomainRequest();
        xhr.open(method, url);
    } else {
        // CORS not supported.
        xhr = null;
    }
    return xhr;
}

// Make the actual CORS request.
function makeCorsRequest(url, data, onComplete, onSuccess, onFail) {

jQuery.ajax({
  url: url,
  type: 'POST',
  contentType: 'application/json; charset=utf-8',
  dataType: 'json',
  data: data,
  complete: function(xhr, textStatus) {
    // todo change element spinner or so
    onComplete(data, textStatus);
  },
  success: function(responseData, textStatus, xhr) {
    onSuccess(responseData, textStatus);
  },
  error: function(xhr, textStatus, errorThrown) {
    //called when there is an error
    onFail(data, textStatus, errorThrown);
  }
});

}

function doTraining() {
    var url = createURL('doTrainingNow');
    console.log(url);

    $.ajax({
        url: url,
        type: 'GET'
    })
    .done(function() {
        $('#status').html("success");
        $('#train').hide();
        console.log("success");
    })
    .fail(function() {
        console.log("error");
        $('#train').hide();
        $('#status').html("error");
    })
    .always(function() {
        console.log("complete");
        $('#train').show();
        $('#status').html("complete");
    });
    
}

$("#train").click(doTraining);

var globalServer = "localhost";

function setServerName() {
    globalServer = $("#server").attr("value");
}

$("#setServer").click(setServerName);