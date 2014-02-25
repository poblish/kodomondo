var stopwords = new Array();

$(function() {
	chrome.runtime.sendMessage({ method: "getOptions"}, function(resp) {
		if (/^https?:\/\/(localhost:2000|www.google.).*/.test(document.URL) === false) {
			chrome.runtime.sendMessage({ method: "getStopwords"}, function(stopwordsResp) {
				stopwords = stopwordsResp.stopwords.slice();
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

var set = {};

function refreshTerms( inOptions, inDocUrl, ioStats, ioHistory) {
	/* Original @author Rob W, created on 16-17 September 2011, on request for Stackoverflow (http://stackoverflow.com/q/7085454/938089) */

	var minIndividualWordLength = 5;
	var ignoreCase = false;  // Case-sensitivity
	var REallowedChars = /[^a-zA-Z0-9\.\-]+/g;  // RE pattern to select valid characters. Invalid characters are replaced with a whitespace. Allow '.' because we need it for pkg names

	// Remove all irrelevant characters
	var text = $('body').text().replace(REallowedChars, " ").replace(/^\s+/, "").replace(/\s+$/, "");

	if (ignoreCase) {
		text = text.toLowerCase();
	}

	text = text.split(/\s+/);

	var i, textlen;
	for (i = 0, textlen = text.length; i < textlen; i++) {
		if (text[i].length >= minIndividualWordLength && stopwords.indexOf(text[i]) < 0) {
			visitTerm( text[i], inDocUrl, ioStats, ioHistory, inOptions);
		}
	}
}

var termLookupPort = chrome.runtime.connect({name: "termLookupPort"});

function visitTerm(term, inDocUrl, ioStats, ioHistory, inOptions) {  // FIXME Needs to be async!!!
	if (term.endsWith('.')) {  // Yuk
		term = term.substring( 0, term.length - 1);
	}

	if (!( term in set)) {
		// console.log('SENT', term );
		termLookupPort.postMessage({ method: "lookupTerm", term: term});
		termLookupPort.onMessage.addListener( function(resp) {
			if (/* Match response to request */ resp.origTerm == term) {
				$('body').highlight( ioStats, ioHistory, inDocUrl, new HighlightClass({terms: [ resp.origTerm ], caseInsensitive: false, foundClass: resp.classDetails.name, jarFQN: resp.jarFQN, artifact: resp.artifact, className:'highlightCore'}), inOptions);
			}
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
		this.jarUrl = 'http://localhost:2000/launch/' + inAttrs.foundClass + '?artifact=' + inAttrs.artifact + '&jar=1';
		this.sourceUrl = 'http://localhost:2000/launch/' + inAttrs.foundClass + '?artifact=' + inAttrs.artifact + '&source=1';
	};

	ctor.prototype = {
		getRegex: function() { return this.regex; },
		getSpanTitle: function() { return this.title; },
		getHighlightClass: function() { return this.className; },
		getJarUrl: function() { return this.jarUrl; },
		getSourceUrl: function() { return this.sourceUrl; }
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

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

function getArtifactInfo(url) {
	console.log(url);
	$.get(url)
		.error( function(xhr) { alert('Sorry, a JAR/source download error occurred. Is the server running?') } )
		.success( function(obj) {
			console.log('Info requested OK');
		});
}
