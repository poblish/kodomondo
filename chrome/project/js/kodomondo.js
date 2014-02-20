$(function() {
	chrome.runtime.sendMessage({ method: "getOptions"}, function(inResp) {
		processPage( inResp.options );
	});
});

chrome.runtime.onMessage.addListener(
	function( inReq, inSender, inSendResponse) {
		if ( inReq.method == "getOptions") {
			$('body').removeHighlights();
			var theHistory = {};
			refreshTerms( inReq.options, null, null, theHistory);
			insertTermCounts( theHistory, inReq.options);
		}
	}
);

function processPage( inOptions ) {
	var theHistory = {};

	var theStats = {};
	theStats['$meta'] = {url: document.URL, title: getPageTitle(), uniqueTerms: 0, totalMatches: 0};
	refreshTerms( inOptions, document.URL, theStats, theHistory);

	// var unqs = theStats['$meta'].uniqueTerms;
	// var score = ( unqs == 0) ? 0 : Math.round( Math.pow( unqs, 1.4) * Math.pow( theStats['$meta'].totalMatches / unqs, 0.7) );
	chrome.runtime.sendMessage({ method: "setBadge", score: 3.14, url: document.URL});

	// submitAnonymousStats( theStats, score);

	// insertTermCounts( theHistory, inOptions);
}

/////////////////////////////////////////////////////

var theIdbDb;

var visitedLinksReq = indexedDB.open("visitedLinks");  // FIXME Need to rename this if we use it for everything!
visitedLinksReq.onsuccess = function(evt) {
	theIdbDb = visitedLinksReq.result;
}

var set = {};
var text = $('body').text();

function refreshTerms( inOptions, inDocUrl, ioStats, ioHistory) {
	/* Original @author Rob W, created on 16-17 September 2011, on request for Stackoverflow (http://stackoverflow.com/q/7085454/938089) */

	var minIndividualWordLength = 4;
	var numWords = 5;  // Show statistics for one to .. words
	var ignoreCase = false;  // Case-sensitivity
	var REallowedChars = /[^a-zA-Z0-9\.\-]+/g;  // RE pattern to select valid characters. Invalid characters are replaced with a whitespace. Allow '.' because we need it for pkg names

	var i, j, textlen, s;

	// Remove all irrelevant characters
	text = text.replace(REallowedChars, " ").replace(/^\s+/, "").replace(/\s+$/, "");

	if (ignoreCase) {
		text = text.toLowerCase();
	}

	text = text.split(/\s+/);

	for (i = 0, textlen = text.length; i < textlen; i++) {

		if (text[i].length >= minIndividualWordLength) {
			s = text[i];
			visitTerm(s, inDocUrl, ioStats, ioHistory, inOptions);
		}
		else s = '';

		for (j = 2; j <= numWords; j++) {
			if (i + j <= textlen) {
				if (text[i + j - 1].length >= minIndividualWordLength) {
					if (s.length > 0) {
						s += " " + text[i + j - 1];
					}
					else {
						s += text[i + j - 1];
					}

					visitTerm(s, inDocUrl, ioStats, ioHistory, inOptions);
				}
			}
			else break;
		}
	}
}

function visitTerm(term, inDocUrl, ioStats, ioHistory, inOptions) {  // FIXME Needs to be async!!!
	if (!( term in set)) {
		chrome.runtime.sendMessage({ method: "lookupTerm", term: term}, function(resp) {
			// console.log('RETURNED', resp.classDetails.name );
			$('body').highlight( ioStats, ioHistory, inDocUrl, new HighlightClass({terms: [ resp.classDetails.name ], className:'highlightCore'}), inOptions);
		});

		set[term] = 1;
    }
}

var HighlightClass = (function() {
	var ctor = function( inAttrs ) {
		var theJoined = '(' + inAttrs.terms.join('|') + ')';
		this.regex = new RegExp( inAttrs.ignoreWordBoundaries ? theJoined : ('\\b' + theJoined + '\\b'), ( inAttrs.caseInsensitive == null || inAttrs.caseInsensitive) ? "i" : "");
		this.className = inAttrs.className;
		this.title = inAttrs.title;
	};

	ctor.prototype = {
		getRegex: function() { return this.regex; },
		getSpanTitle: function() { return this.title; },
		getHighlightClass: function() { return this.className; }
	};

	return ctor;
})();

/////////////////////////////////////////////////////

function getPageTitle() {
	var theTitles = $('head title');
	if ( theTitles.length > 0) {
		return $.trim(theTitles[0].innerHTML);
	} else {
		return '???';
	}
}

function adjustAssocArrayKeyCase( inArray, inKeyToFind) {
	var theLCaseMatch = inKeyToFind.toLowerCase();

	for ( eachExistingTerm in inArray) {
		if (eachExistingTerm.toLowerCase() === theLCaseMatch) {
			// console.log("Use '" + theLCaseMatch + "' instead of '" + inKeyToFind + "'");
			return eachExistingTerm;
		}
	}

	return inKeyToFind;
}