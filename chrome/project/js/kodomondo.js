var g_Stopwords = new Array();
var g_KeyTermRegexes = new Array();

$(function() {
	chrome.runtime.sendMessage({ method: "getOptions"}, function(resp) {
		if (/^https?:\/\/(localhost:2000|www.google.).*/.test(document.URL) === false) {
			chrome.runtime.sendMessage({ method: "datasourceInfo"}, function(dsResp) {
				var stopwords = dsResp.stopwords.slice();
				g_StopwordsRegex = stopwords.length > 0 ? new RegExp( '\\b' + '(' + stopwords.join('|') + ')' + '\\b') : null;  // FIXME Why do we have to keep regenerating in content script?
				g_KeyTermRegexes = dsResp.keyTermRegexes.slice();
				processPage( resp.options );
			});
		}
	});
});

chrome.runtime.onMessage.addListener(
	function( inReq, inSender, inSendResponse) {
		if ( inReq.method == "getOptions") {
			$('body').removeHighlights();
			var theHistory = {};
			refreshTerms( inReq.options, null, null, theHistory);
			// insertTermCounts( theHistory, inReq.options);
		}
	}
);

function processPage( inOptions ) {
	var theHistory = {};

	var theStats = {};
	theStats['$meta'] = {url: document.URL, title: getPageTitle(), uniqueTerms: 0, totalMatches: 0};
	refreshTerms( inOptions, document.URL, theStats, theHistory);

	// FIXME Should send this when we *know* highlighting has finished, rather than waiting
	window.setTimeout( function(e) {
		var unqs = theStats['$meta'].uniqueTerms;
		// var score = ( unqs == 0) ? 0 : Math.round( Math.pow( unqs, 1.4) * Math.pow( theStats['$meta'].totalMatches / unqs, 0.7) );
		chrome.runtime.sendMessage({ method: "setBadge", score: unqs, url: document.URL, date: new Date().toUTCString()});
	}, 2000);

	// submitAnonymousStats( theStats, score);
	// insertTermCounts( theHistory, inOptions);
}

/////////////////////////////////////////////////////

var visitedTerms = {};
var termsToHighlight = [];

function getDocumentText(ignoreCase) {
	var REallowedChars = /[^a-zA-Z0-9\.\-]+/g;  // RE pattern to select valid characters. Invalid characters are replaced with a whitespace. Allow '.' because we need it for pkg names
	// Remove all irrelevant characters
	var text = $('body').text().replace(REallowedChars, " ").replace(/^\s+/, "").replace(/\s+$/, "");
	return ignoreCase ? text.toLowerCase() : text;
}

function getDocumentWords(ignoreCase) {
	return getDocumentText(false).split(/\s+/);
}

function getDocumentText2(ignoreCase) {
	var REallowedChars = /[^a-zA-Z0-9\.\-]+/g;  // RE pattern to select valid characters. Invalid characters are replaced with a whitespace. Allow '.' because we need it for pkg names
	// Remove all irrelevant characters
	var text = getText2($('body')).replace(REallowedChars, " ").replace(/^\s+/, "").replace(/\s+$/, "");
	return ignoreCase ? text.toLowerCase() : text;
}

function getDocumentWords2(ignoreCase) {
	return getDocumentText2(false).split(/\s+/);
}

function refreshTerms( inOptions, inDocUrl, ioStats, ioHistory) {
	refreshTermsCallback( inOptions, inDocUrl, ioStats, ioHistory, visitTerm)
}

var fqnsMatched;

function refreshTermsCallback( inOptions, inDocUrl, ioStats, ioHistory, visitorCallback) {
	/* Original @author Rob W, created on 16-17 September 2011, on request for Stackoverflow (http://stackoverflow.com/q/7085454/938089) */
	var minIndividualWordLength = 5;

	var text = getDocumentWords2(false);

	var i, textlen;
	var totalNumTermsInDoc = 0, keyTermsMatched = 0;

	fqnsMatched = 0;

	var keyTermsRegex = g_KeyTermRegexes.length > 0 ? new RegExp( '\\b' + '(' + g_KeyTermRegexes.join('|') + ')' + '\\b', "i") : null;  // FIXME Why do we have to keep regenerating in content script?
	// console.log('keyTermsRegex', keyTermsRegex);

	for (i = 0, textlen = text.length; i < textlen; i++) {

		if (text[i].endsWith('.')) {  // Yuk
			text[i] = text[i].substring( 0, text[i].length - 1);
		}

		// Check keywords first, because there can be overlap. We use 'Java' to identify Java code, yet don't want to highlight it!
		if (keyTermsRegex && keyTermsRegex.test(text[i])) {
			keyTermsMatched++;
		}
		else {
			if (text[i].length < minIndividualWordLength) {
				continue;
			}

			totalNumTermsInDoc++;

			if (g_StopwordsRegex && g_StopwordsRegex.test(text[i])) {
				continue;
			}

			visitorCallback( text[i], inDocUrl, ioStats, ioHistory, inOptions);
		}
	}

	window.setTimeout( function(e) {
		var normalTermsFound = termsToHighlight.length - fqnsMatched;
		var score = Math.pow(( 15 * fqnsMatched + ( 5 * keyTermsMatched) + normalTermsFound) / totalNumTermsInDoc, 0.6);
		console.log('SCORE', score, ' > ', fqnsMatched, keyTermsMatched, normalTermsFound, '/', totalNumTermsInDoc);

		if ( score > 0.25) {
			for (i = 0; i < termsToHighlight.length; i++) {
				var match = termsToHighlight[i];
				$('body').highlight( ioStats, ioHistory, inDocUrl, new HighlightClass({terms: [match.term], caseInsensitive: false, foundClass: match.resp.classDetails.name, jarFQN: match.resp.jarFQN, artifact: match.resp.artifact, className:'highlightCore'}), inOptions);
			}
		}
	}, 1000);  // FIXME 1000 is too high for small pages, too small for big ones!
}

var termLookupPort = chrome.runtime.connect({name: "termLookupPort"});

function visitTerm(term, inDocUrl, ioStats, ioHistory, inOptions) {  // FIXME Needs to be async!!!
	if (!( term in visitedTerms)) {
		termLookupPort.postMessage({ method: "lookupTerm", term: term});
		termLookupPort.onMessage.addListener( function(resp) {
			if (/* Match response to request */ resp.origTerm == term) {
				termsToHighlight.push({ term: resp.origTerm, resp: resp});

				if (term.indexOf('.') > 0) {
					fqnsMatched++;
				}

				// console.log(resp.origTerm + ' -> ' + resp.classDetails.name);
			}
		});

		visitedTerms[term] = 1;
	}
}

function getArtifactInfo(inItem) {

	var theHeaderElem = new FacebookStylePopupHeader('Kodomondo' + '&nbsp;&raquo;&nbsp;' + 'Java Class Info','myId');
	var thePopup = new FacebookStylePopup( document.body, theHeaderElem, 800);

	var overlay = document.createElement('div');
	overlay.setAttribute('class', 'kodoOverlay');
	document.body.appendChild(overlay);

	$('body').bind('keyup', function(e) { if ( e.keyCode == 27) closeDialog() });

	var introNode = document.createElement('p');
	introNode.setAttribute('class', 'kodoInfo');
	introNode.innerHTML =  inItem.getDisplayName();
	thePopup.appendChild(introNode);

	var srcButton = document.createElement('a');
	srcButton.setAttribute('href', '');
	srcButton.setAttribute('class', 'dlgLink');
	srcButton.innerHTML = 'View Source';
	$(srcButton).click(function(e) { e.preventDefault(); asyncRequestUrl( inItem.getSourceUrl() ); });

	var jarButton = document.createElement('a');
	jarButton.setAttribute('href', '');
	jarButton.setAttribute('class', 'dlgLink');
	jarButton.innerHTML = 'Open JAR';
	$(jarButton).click(function(e) { e.preventDefault(); asyncRequestUrl( inItem.getJarUrl() ); });

	var infoButton = document.createElement('a');
	infoButton.setAttribute('href', inItem.getInfoUrl());
	infoButton.setAttribute('target', '_kodomondoInfo');
	infoButton.setAttribute('class', 'dlgLink');
	infoButton.innerHTML = 'More Info';

	var buttonsDiv = document.createElement('div');
	buttonsDiv.setAttribute('style', 'padding: 9px 0 9px 16px');
	buttonsDiv.appendChild(srcButton);
	buttonsDiv.appendChild(document.createTextNode('  |  ') );
	buttonsDiv.appendChild(jarButton);
	buttonsDiv.appendChild(document.createTextNode('  |  ') );
	buttonsDiv.appendChild(infoButton);
	thePopup.appendChild(buttonsDiv);

	var closeBtn = document.createElement('a');
	closeBtn.setAttribute('class', 'dlgButton');
	closeBtn.setAttribute('style', "margin-right: 7px; background-image: url('" + chrome.runtime.getURL('img/shading.png') + "')");

	var closeBtnText = document.createElement('span');
	closeBtnText.setAttribute('class', 'dlgButtonText');
	$(closeBtnText).html('<span style="color:#AAA">&#9099;</span>&nbsp;&nbsp;' + 'Close');
	closeBtn.appendChild(closeBtnText);

	$(closeBtn).click(function(e) { e.preventDefault(); closeDialog(); });

	var closeForm = document.createElement('form');
	closeForm.setAttribute('style', "padding: 0 0 7px 10px; margin: 0; text-align: right");
	closeForm.appendChild(closeBtn);
	thePopup.appendChild(closeForm);

	thePopup.show();
}

function asyncRequestUrl(url) {
	$.get(url)
		.error( function(xhr) { alert('Sorry, a JAR/source download error occurred. Is the server running?') } )
		.success( function(obj) {
			// console.log('Info requested OK');
		});
}

//////////////////////////////////////

function getText2(elem) {
    if (elem.id === 'footer' || $(elem).css('display') === 'none' || (elem.parentNode != null && ( elem.parentNode.nodeName == "OPTION" || elem.parentNode.nodeName == "LABEL" || elem.parentNode.nodeName == "BUTTON"))) {
        // console.log('SKIP', elem);
        return "";
    }

    var node, ret = "", i = 0, nodeType = elem.nodeType;

    if (!nodeType) {  // If no nodeType, this is expected to be an array
        while ((node = elem[i++])) {
            ret += getText2(node);  // Do not traverse comment nodes
        }
    } else if (nodeType === 1 || nodeType === 9 || nodeType === 11) {
        for (elem = elem.firstChild; elem; elem = elem.nextSibling) {  // Traverse its children
            ret += getText2(elem);
        }
    } else if (nodeType === 3 || nodeType === 4) {
        var trimmed = elem.nodeValue.trim();
        if (trimmed === '.') {
          return '';
        }
        return ( trimmed !== '') ? trimmed + ' ' : trimmed;
    }
    // Do not include comment or processing instruction nodes

    return ret;
};