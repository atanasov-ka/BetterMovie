var currentUrl = window.location.href;

var commentsRows = $("body > table.mainouter > tbody > tr:nth-child(2) > td > table > tbody > tr:nth-child(1) > td:nth-child(3) > table:nth-child(15) > tbody > tr > td > table > tbody > tr > td > table.main");

if (commentsRows.length === 0) {
    commentsRows = $("body > table.mainouter > tbody > tr:nth-child(2) > td > table > tbody > tr:nth-child(1) > td:nth-child(3) > table > tbody > tr > td > table > tbody > tr > td > table.main");
}

var i = 0;
var oneRow;
var comment;
var imgTags;
var j = 0;
while (i < commentsRows.length) {
    oneRow = commentsRows[i];

    // get text
    comment = $(oneRow).find("tbody tr td.text");

    // get imgs
    imgTags = $(comment).find("img");

    // clean each img and leave ONLY alt
    for (j = imgTags.length - 1; j >= 0; j--) {
        var oneImg = imgTags[j];
        var emoticon = $(oneImg).attr("alt");
        $(oneImg).replaceWith(emoticon); // self replace
    }

    var tr = $(oneRow).find("tbody tr")[0]; // first tr

    var citirane = $(comment).find('table.main');
    if (citirane !== undefined && citirane != null) {
        $(citirane).remove();        
    }

    var edoKoiSiNapisa = $(comment).find('p.sub');
    if (edoKoiSiNapisa !== undefined && edoKoiSiNapisa != null) {
        $(edoKoiSiNapisa).remove();        
    }

    // add td
    var bundleCommentData  = {"i": i, "data": $(comment).text()};

    var detailVal = {'detail': bundleCommentData};
    var valTxt = JSON.stringify(detailVal);

    var buttonGood = "<div class='pos' id='betterMovieGood%i%' onclick='var e = new CustomEvent(\"betterMovieGood\" , %data% ); document.dispatchEvent(e);'><img alt='Позитивно'/></div>".replace("%i%", i).replace("%data%", valTxt);
    var buttonBad = "<div class='neg' id='betterMovieBad%i%' onclick='var e = new CustomEvent(\"betterMovieBad\" , %data% ); document.dispatchEvent(e);'><img alt='Негативно'/></div>".replace("%i%", i).replace("%data%", valTxt);
    var td = "<td width=50 id='betterMovieRate%i%'>".replace("%i%", i) + buttonGood + buttonBad + "</td>";

    $(td).appendTo(comment.parent());

    // Next
    ++i;
}

var posPicUrl = chrome.extension.getURL('pos.png');
var negPicUrl = chrome.extension.getURL('neg.png');
$(".pos img").css('background-image', 'url(' + posPicUrl + ')');
$(".neg img").css('background-image', 'url(' + negPicUrl + ')');

function getTdRate(i) {
    return $("td#betterMovieRate%i%".replace("%i%", i));
}

function getPosBtn(i) {
    return $("#betterMovieGood%i%".replace("%i%", i));
}

function getNegBtn(i) {
    return $("#betterMovieBad%i%".replace("%i%", i));
}


function onCompleted(data, textStatus) {
    var posBtn = getPosBtn(data.i);
    $(posBtn).hide();

    var negBtn = getNegBtn(data.i);
    $(negBtn).hide();

    var td = getTdRate(data.i);
    $(td).html("Wait...");
}

function onSuccess(responseData, textStatus) {
    var td = getTdRate(responseData.i);
    $(td).html("Voted as " + responseData.type);
}

function onFail(data, textStatus, errorThrown) {
    var td = getTdRate(data.i);
    td.html("<div alt='%status%'>Error!</div>".replace("%status%", textStatus));   
}

function sendCorpus(type, data) {
    console.log("send corpus");
    var corpus = {
        "store": {
            "i":data.i,
            "type": type,
            "data": data.data
        }
    };
    console.log(JSON.stringify(corpus));
    var url = createURL('saveTrainingEntries');
    makeCorsRequest(url, JSON.stringify(corpus), onCompleted, onSuccess, onFail);
}


document.addEventListener("betterMovieBad", function (e) {
    var obj = e.detail;
    sendCorpus("neg", obj);
});


document.addEventListener("betterMovieGood", function (e) {
    var obj = e.detail;
    sendCorpus("pos", obj);
});