var allRows = $("table.main tbody tr td table[class='test responsivetable'] tbody tr")

var i = 1;
while (i < allRows.length) {


    var oneRow = allRows[i];

    // get text
    oneRow.outerHTML = oneRow.outerHTML.replace("<tr", "<tr id='rowGetRaiting%i%' ".replace("%i%", i));

    var rowNode = document.getElementById("rowGetRaiting%i%".replace("%i%", i));
    // get link with xpath
    var link = rowNode.children[1].children[0].href;
    var detailVal = {'detail': {
        'link': link, 
        'i': i
    }};

    // generate td > button
    var getRaiting = "<td align='center' id='tdGetRaiting%i%'><button betterMovie='getRaiting' id='btnGetRaiting%i%' onclick='var e = new CustomEvent(\"getRaiting\" ,  %detailVal%); document.dispatchEvent(e);'>Get Raiting</button></td>";
    getRaiting = getRaiting.replace("%i%", i).replace("%i%", i);
    getRaiting = getRaiting.replace("%detailVal%", JSON.stringify(detailVal));
    console.log("getRaiting " + getRaiting);
    // add request button
    rowNode.children[3].outerHTML = getRaiting;    

    // Next
    ++i;
}

document.addEventListener("getRaiting", function (e) {
    var obj = e.detail;
    sendRequestRaiting(obj);
});

function getRaitingButton(i) {
    return $("button#btnGetRaiting%i%".replace("%i%", i));
}

function getRaitingTd(i) {
    return $("td#tdGetRaiting%i%".replace("%i%", i));
}

function clearStyle(i) {
    $(getRaitingButton(i)).show();
    $(getRaitingTd(i)).removeAttr('style');
}

function onSuccessReplaceButtonsWithRaiting(responseData, textStatus) {
    clearStyle(responseData.i);
    var clickedButton = getRaitingButton(responseData.i);
    clickedButton.remove();

    var clickedTd = getRaitingTd(responseData.i);
    clickedTd.html(responseData.rate + " %");
    
    $(clickedTd).css({ "font-size": '200%'});
    $(clickedTd).css({"color": '#999966'});
    if (0 + responseData.rate > 0) {
        $(clickedTd).css({"color": '#CC0000'});
    }
    if (0 + responseData.rate > 33) {
        $(clickedTd).css({"color": '#FF3300'});
    }
    if (0 + responseData.rate > 55) {
        $(clickedTd).css({"color": '#00CC00'});
    }
}

function onCompletedHideButtonShowSpining(data, textStatus){
    clearStyle(data.i);
    var clickedButton = getRaitingButton(data.i);
    $(clickedButton).hide();
    
    var clickedTd = getRaitingTd(data.i);
    $(clickedTd).html("Wait...");
}

function onFail(data, textStatus, errorThrown) {
    clearStyle(data.i);
    var clickedTd = getRaitingTd(data.i);
    $(clickedTd).html("<div alt='%status%'>Error!</div>".replace("%status%", textStatus));
}

function sendRequestRaiting(obj) {
    console.log("sending getRaiting request");
    console.log(obj);
    var url = createURL('doCrawAndGetRating');
    makeCorsRequest(
        url, 
        JSON.stringify(obj), 
        onCompletedHideButtonShowSpining, 
        onSuccessReplaceButtonsWithRaiting, 
        onFail);
    var loaderUrl = chrome.extension.getURL('loader.gif');
    $(getRaitingButton(obj.i)).hide();
    $(getRaitingTd(obj.i)).css('background-image', 'url(' + loaderUrl + ')');
    $(getRaitingTd(obj.i)).css('background-repeat', 'no-repeat');
    $(getRaitingTd(obj.i)).css('display', 'block');
    $(getRaitingTd(obj.i)).css('margin-left', 'auto');
    $(getRaitingTd(obj.i)).css('margin-right', 'auto');
}

function getElementByXpathDoc(path, doc) {
    return document.evaluate(path, doc, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
}

function getElementByXpath(path, doc) {
    if (doc === undefined ) {
        return getElementByXpathDoc(path, document);
    } else {
        return getElementByXpathDoc(path, doc);
    }
        
}


