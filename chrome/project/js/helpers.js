var HighlightClass = (function() {
	var ctor = function( inAttrs ) {
		var theJoined = '(' + inAttrs.terms.join('|') + ')';
		this.regex = new RegExp( inAttrs.ignoreWordBoundaries ? theJoined : ('\\b' + theJoined + '\\b'), ( inAttrs.caseInsensitive == null || inAttrs.caseInsensitive) ? "i" : "");
		this.jarFQN = inAttrs.jarFQN;
		this.foundClass = inAttrs.foundClass;
		this.className = inAttrs.className;
		this.title = inAttrs.title;
		this.jarUrl = 'http://localhost:2000/launch/' + inAttrs.foundClass + '?artifact=' + inAttrs.artifact + '&jar=1';
		this.infoUrl = 'http://localhost:2000/launch/' + inAttrs.foundClass + '?artifact=' + inAttrs.artifact + '&jar=1';
		this.sourceUrl = 'http://localhost:2000/launch/' + inAttrs.foundClass + '?artifact=' + inAttrs.artifact + '&source=1';
	};

	ctor.prototype = {
		getDisplayName: function() { return '<strong>' + this.foundClass + '</strong>  <--  ' + this.jarFQN; },
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