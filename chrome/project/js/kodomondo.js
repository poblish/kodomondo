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
		this.terms = inAttrs.terms;
		this.className = inAttrs.className;
		this.title = inAttrs.title;
		this.jarUrl = 'http://localhost:2000/launch/' + inAttrs.foundClass + '?artifact=' + inAttrs.artifact + '&jar=1';
		this.infoUrl = 'http://localhost:2000/launch/' + inAttrs.foundClass + '?artifact=' + inAttrs.artifact + '&jar=1';
		this.sourceUrl = 'http://localhost:2000/launch/' + inAttrs.foundClass + '?artifact=' + inAttrs.artifact + '&source=1';
	};

	ctor.prototype = {
		getDisplayName: function() { return this.terms; },
		getRegex: function() { return this.regex; },
		getSpanTitle: function() { return this.title; },
		getHighlightClass: function() { return this.className; },
		getJarUrl: function() { return this.jarUrl; },
		getSourceUrl: function() { return this.sourceUrl; },
		getInfoUrl: function() { return this.infoUrl; }
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

function getArtifactInfo(inItem) {

	var theHeaderElem = new FacebookStylePopupHeader('Kodomondo' + '&nbsp;&raquo;&nbsp;' + 'Java Class Info','myId');
	var thePopup = new FacebookStylePopup( document.body, theHeaderElem, 800);

	var overlay = document.createElement('div');
	overlay.setAttribute('class', 'overlay');
	document.body.appendChild(overlay);

	$('body').bind('keyup', function(e) { if ( e.keyCode == 27) closeDialog() });

	var introNode = document.createElement('p');
	introNode.setAttribute('style', 'font-weight: bold; padding: 6px 0 0 7px; margin: 0');
	introNode.innerHTML =  inItem.getDisplayName();
	thePopup.appendChild(introNode);

	var srcButton = document.createElement('a');
	srcButton.setAttribute('href', '');
	srcButton.innerHTML = 'View Source';
	$(srcButton).click(function(e) { e.preventDefault(); asyncRequestUrl( inItem.getSourceUrl() ); });

	var jarButton = document.createElement('a');
	jarButton.setAttribute('href', '');
	jarButton.innerHTML = 'Open JAR';
	$(jarButton).click(function(e) { e.preventDefault(); asyncRequestUrl( inItem.getJarUrl() ); });

	var buttonsDiv = document.createElement('div');
	buttonsDiv.setAttribute('style', 'padding: 9px 0 9px 7px');
	buttonsDiv.appendChild(srcButton);
	buttonsDiv.appendChild(document.createTextNode('  |  ') );
	buttonsDiv.appendChild(jarButton);
	thePopup.appendChild(buttonsDiv);

	var closeButton = document.createElement('button');
	closeButton.setAttribute('style', "margin-right: 7px");
	closeButton.innerHTML = 'Close';
	$(closeButton).click(function(e) { e.preventDefault(); closeDialog(); });

	var theCloseFormElem = document.createElement('form');
	theCloseFormElem.setAttribute('style', "padding: 0 0 7px 10px; text-align: right");
	theCloseFormElem.appendChild(closeButton);
	thePopup.appendChild(theCloseFormElem);

	thePopup.show();
}

function asyncRequestUrl(url) {
	$.get(url)
		.error( function(xhr) { alert('Sorry, a JAR/source download error occurred. Is the server running?') } )
		.success( function(obj) {
			// console.log('Info requested OK');
		});
}

////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////

function closeDialog() {
	$('div.fbOutermost').each( function(i) { $(this).remove(); });
	$('div.overlay').remove();
}


var FacebookStylePopup = (function() {
	var ctor = function( inParentElem, inHeader, inWidth, inTopOffsetOverride) {
		this.top = $(window).scrollTop();
		this.topOffset = ( inTopOffsetOverride != null) ? parseInt(inTopOffsetOverride) : 125;

		this.outerElem = document.createElement('div');
		this.outerElem.setAttribute('class', 'userInfoPopup');
		this.outerElem.setAttribute('style', "position: relative; margin: auto; height: 0; width: " + inWidth + "px; top: " + ( this.top + this.topOffset) + "px");

		this.contentElem = document.createElement('div');
		this.contentElem.setAttribute('style', "background-color: white; padding-bottom: 1px");
		this.contentElem.appendChild( inHeader.getElem() );

		var wrapper = document.createElement('div');
		wrapper.setAttribute('class', 'fbContentWrapper');
		wrapper.setAttribute('style', '-webkit-border-bottom-left-radius: 8px 8px; -webkit-border-bottom-right-radius: 8px 8px; -webkit-border-top-left-radius: 8px 8px; -webkit-border-top-right-radius: 8px  8px;');
		wrapper.appendChild(this.contentElem);
		this.outerElem.appendChild(wrapper);

		this.outermostElem = document.createElement('div');
		this.outermostElem.setAttribute('class', 'fbOutermost');
		this.outermostElem.setAttribute('style', 'display: none');
		this.outermostElem.appendChild(this.outerElem);

		inParentElem.parentNode.insertBefore( this.outermostElem, inParentElem.nextSibling);
	};

	ctor.prototype = {
		setTop: function( inTop ) {
			this.top = inTop;
			return this;
		},

		setTopOffset: function( inTopOffset ) {
			this.topOffset = inTopOffset;
			return this;
		},

		appendChild: function( inChildElem ) {
			this.contentElem.appendChild(inChildElem);
		},

		show: function() {
			this.outerElem.style.top = ( this.top + this.topOffset) + 'px';
			$(this.outermostElem).fadeIn();
		},

		selectChildren: function( inSelector ) {
			return this.contentElem.select(inSelector);	// Prototype
		}
	};

	return ctor;
})();

var FacebookStylePopupHeader = (function() {
	var ctor = function( inContent, inId ) {
		this.elem = document.createElement('div');
		this.elem.id = inId;
		this.elem.setAttribute('class', 'fbPopupHeader');
		this.elem.innerHTML = inContent;
	};

	ctor.prototype = {
		getElem: function() {
			return this.elem;
		},

		setContent: function( inContent ) {
			this.elem.innerHTML = inContent;
		}
	};

	return ctor;
})();