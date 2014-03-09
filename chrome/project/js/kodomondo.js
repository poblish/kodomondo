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
	var totalNumTermsInDoc = 0, keyTermsMatched = 0;

	var keyTermsRegex = g_KeyTermRegexes.length > 0 ? new RegExp( '\\b' + '(' + g_KeyTermRegexes.join('|') + ')' + '\\b', "i") : null;  // FIXME Why do we have to keep regenerating in content script?
	// console.log('keyTermsRegex', keyTermsRegex);

	for (i = 0, textlen = text.length; i < textlen; i++) {

		if (text[i].endsWith('.')) {  // Yuk
			text[i] = text[i].substring( 0, text[i].length - 1);
		}

		if (text[i].length < minIndividualWordLength) {
			continue;
		}

		totalNumTermsInDoc++;

		if (g_StopwordsRegex.test(text[i])) {
			continue;
		}

		if (keyTermsRegex && keyTermsRegex.test(text[i])) {
			keyTermsMatched++;
		}
		else {
			visitTerm( text[i], inDocUrl, ioStats, ioHistory, inOptions);
		}
	}

	window.setTimeout( function(e) {
		var normalTermsFound = termsToHighlight.length;
		var score = Math.sqrt(( 5 * keyTermsMatched + normalTermsFound) / totalNumTermsInDoc);
		console.log('SCORE', score, ' > ', keyTermsMatched, normalTermsFound, '/', totalNumTermsInDoc);

		if ( score > 0.2) {
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
				// console.log(resp.origTerm + ' -> ' + resp.classDetails.name);
			}
		});

		visitedTerms[term] = 1;
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
