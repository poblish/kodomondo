/*

highlight v3 - Modified by Marshal (beatgates@gmail.com) to add regexp highlight, 2011-6-24

Highlights arbitrary terms.

<http://johannburkard.de/blog/programming/javascript/highlight-javascript-text-higlighting-jquery-plugin.html>

MIT license.

Johann Burkard
<http://johannburkard.de>
<mailto:jb@eaio.com>

*/

var theIgnoreClassesArray = ['highlightCore','highlightExtra','highlightReplaced','highlightIgnore','highlightMgmt','highlightShutUp','highlightHooray'];

var idCount = 0;

jQuery.fn.highlight = function( ioStats, ioHistory, inDocUrl, inTermsGroup, inOptions) {

    var theWhiteList = getContentStatsWhiteListFor(inDocUrl);
    var theBlackList = getContentStatsBlackListFor(inDocUrl);
    var theHighlightOption = inOptions['highlightOptions'];
    var isHighlightFirstMode = theHighlightOption === 'h_first';
    var wantTermTracking = ( ioHistory != null && ( inOptions['displayTermCount'] === 'true' || isHighlightFirstMode));

    function innerHighlight( node, ioStats, ioHistory, inHighlightOption) {
        if ( node.parentNode != null && node.parentNode.nodeName == "A") {
            return 0;
        }

        var skip = 0;
        if (node.nodeType === 3) { // 3 - Text node
            var pos = node.data.search( inTermsGroup.getRegex() );
            if (pos >= 0 && node.data.length > 0) { // .* matching "" causes infinite loop
                var match = node.data.match( inTermsGroup.getRegex() ); // get the match(es), but we would only handle the 1st one, hence /g is not recommended
                var middleBit = node.splitText(pos); // split to 2 nodes, node contains the pre-pos text, middleBit has the post-pos

		if ( jQuery.inArray( middleBit.parentNode.className, theIgnoreClassesArray) >= 0) {  // (AGR) Check not already done!
			// Skip
		}
		else {
                        var spanNode = document.createElement('span');

                        if (wantTermTracking) {
                                var histKeyToUse = adjustAssocArrayKeyCase( ioHistory, match[0]);
                                var histObj = ioHistory[ histKeyToUse ];

                                if ( histObj == null) {
                                    if (inTermsGroup.getHighlightClass() === 'highlightIgnore') {
                                        ioHistory[ histKeyToUse ] = /* Poison: */ {node: null, c: -999};
                                    } else {
                                        ioHistory[ histKeyToUse ] = {node: spanNode, c: 1};
                                    }

                                    if (isHighlightFirstMode) {
                                        spanNode.className = inTermsGroup.getHighlightClass();
                                    }
                                } else if ( histObj.c > 0) {
                                    ioHistory[ histKeyToUse ].c = histObj.c + 1;

                                    if (isHighlightFirstMode) {
                                        spanNode.className = inTermsGroup.getHighlightClass() + '_ul';
                                    }
                                } else {
                                    // Ignore poison - don't deal with ignored terms!
                                }

                                // console.log("adding...", histKeyToUse);
                        }

                        if ( inHighlightOption !== 'disable') {
                            if ( inTermsGroup.getHighlightClass() === 'highlightIgnore') {
                                spanNode.className = inTermsGroup.getHighlightClass();
                            }
                            else if (!isHighlightFirstMode) {
                                if (inHighlightOption === 'u_all') {
                                    spanNode.className = inTermsGroup.getHighlightClass() + '_ul';
                                }
                                else /* if (inHighlightOption === 'h_all') */ {
                                    spanNode.className = inTermsGroup.getHighlightClass();
                                }
                            }
                        }

                        spanNode.title = inTermsGroup.getSpanTitle();

		        if ( ioStats != null && inTermsGroup.getHighlightClass() != 'highlightIgnore') {
				var keyToUse = adjustAssocArrayKeyCase( ioStats, match[0]);
				var obj = ioStats[keyToUse];

				if ( obj == null) {
				    ioStats[keyToUse] = {t: ( inTermsGroup.getHighlightClass() == 'highlightCore' ? 'C': 'E'),c:1};
				    ioStats['$meta'].uniqueTerms++;
				} else {
				    ioStats[keyToUse].c = obj.c + 1;
				}

				ioStats['$meta'].totalMatches++;
			}

                        if (inHighlightOption !== 'disable') {
                            /* var endBit = */ middleBit.splitText(match[0].length); // similarly split middleBit in two @ http://mzl.la/S7KA7V
                            var middleClone = middleBit.cloneNode(true);
                            spanNode.appendChild(middleClone);

                            /* if ( spanNode.className == null) {
                                alert('Error');
                            } */

                            // parentNode ie. node, now has 3 nodes by 2 splitText()s, replace the middle with the highlighted spanNode:
                            middleBit.parentNode.replaceChild(spanNode, middleBit);
                        }

			if (!spanNode.className.endsWith('_ul')) {
				var jarInfoNode = document.createElement('a');
				jarInfoNode.id = 'kodomondo_info_' + idCount;
				jarInfoNode.href = '';

				var infoImgNode = document.createElement('img');
				infoImgNode.setAttribute('class', 'info');
				infoImgNode.src = chrome.runtime.getURL('img/info.png');
				infoImgNode.title = 'Kodomondo Info';
				infoImgNode.alt = '';

				jarInfoNode.appendChild(infoImgNode);
				spanNode.appendChild(jarInfoNode);

				$("#kodomondo_info_" + idCount).click( function(e){ e.preventDefault(); getArtifactInfo(inTermsGroup); });
				idCount++;
			}
                }
		skip = 1; // skip this middleBit, but still need to check endBit
            }
        } else if (node.nodeType === 1 && node.childNodes && !/(script|style|textarea)/i.test(node.tagName)) { // 1 - Element node
            for (var i = 0; i < node.childNodes.length; i++) { // highlight all children

                var theJQNode = $(node.childNodes[i]);
                // FIXME Removed 'display===none' test for now

                //////////////////////////////////////////////////////////////////  Handle stats-submission black/white-listing

                var theStatsObjToUse = ioStats;

                if ( theWhiteList != null && theJQNode.is(theWhiteList)) {
                    // OK
                }
                else if ( theBlackList != null && theJQNode.is(theBlackList)) {
                    theStatsObjToUse = null;
                }

                i += innerHighlight( node.childNodes[i], theStatsObjToUse, ioHistory, theHighlightOption); // skip highlighted ones
            }
        }
        return skip;
    }

    return this.each(function() {
        innerHighlight( this, ioStats, ioHistory, theHighlightOption);
    });
};

jQuery.fn.replaceHighlight = function( pattern, inReplacement, inHiliteClassName, inSpanTitle) {
    var regex = typeof(pattern) === "string" ? new RegExp(pattern, "i") : pattern; // assume very LOOSELY pattern is regexp if not string
    function innerHighlight( node, ioStats, pattern, inHiliteClassName, inSpanTitle) {
        var skip = 0;
        if (node.nodeType === 3) { // 3 - Text node
            var pos = node.data.search(regex);
            if (pos >= 0 && node.data.length > 0) { // .* matching "" causes infinite loop
                var match = node.data.match(regex); // get the match(es), but we would only handle the 1st one, hence /g is not recommended
                var spanNode = document.createElement('span');
                spanNode.className = inHiliteClassName;
                spanNode.title = inSpanTitle;
                var middleBit = node.splitText(pos); // split to 2 nodes, node contains the pre-pos text, middleBit has the post-pos
                /* var endBit = */ middleBit.splitText(match[0].length); // similarly split middleBit in two @ http://mzl.la/S7KA7V
                spanNode.appendChild( document.createTextNode(inReplacement) );
                // parentNode ie. node, now has 3 nodes by 2 splitText()s, replace the middle with the highlighted spanNode:
                middleBit.parentNode.replaceChild(spanNode, middleBit);
                skip = 1; // skip this middleBit, but still need to check endBit
            }
        } else if (node.nodeType === 1 && node.childNodes && !/(script|style|textarea)/i.test(node.tagName)) { // 1 - Element node
            for (var i = 0; i < node.childNodes.length; i++) { // highlight all children
                i += innerHighlight( node.childNodes[i], null, pattern, inHiliteClassName, inSpanTitle); // skip highlighted ones
            }
        }
        return skip;
    }

    return this.each(function() {
        innerHighlight( this, null, pattern, inHiliteClassName, inSpanTitle);
    });
};

jQuery.fn.removeHighlight = function(inStyleRule) {
    var shownWarning = false;

    return this.find(inStyleRule).each(function() {
        this.parentNode.firstChild.nodeName;
        with (this.parentNode)
        {
            if (shownWarning) {
                return;
            }

            if ( inStyleRule == 'span.highlightReplaced') {
    		chrome.runtime.sendRequest({ method: "notify", heading: "Settings changed", msg: "Sorry, we can't restore words that have already been replaced. Please refresh the page to restore them."}, function(inResp) {});
            	shownWarning = true;
            	return;
            }

            replaceChild(this.firstChild, this);
            normalize();
        }
    }).end();
};

jQuery.fn.removeHighlights = function() {
	this.removeHighlight("span.highlightCore, span.highlightExtra, span.highlightMgmt, span.highlightShutUp, span.highlightHooray, span.highlightCore_ul, span.highlightExtra_ul, span.highlightMgmt_ul, span.highlightShutUp_ul, span.highlightHooray_ul, span.highlightReplaced");
}

function getContentStatsBlackListFor( inDocURL ) {
    return $('header, footer, div#comments, div#allcomments, div#disqus_thread, div#most-popular, div#sidebar, div#sidebar-right-1, div#r_sidebar, div#single-rightcolumn, div#sidebar-first, div#all-comments, div#discussion-comments, div#reader-comments, div#beta, div#bottom, div#departments, div#promo, div#secondaryColumn, div#breadcrumb, div#related, ul#menus, ul.sf-menu, .primary-links, .widget-container, .commentContainer, .comments_block_holder, div.comments-container, .comment_item, .rightbox, .top-index-stories, div.sidebar, div.widget-content, div.comment, div.comment-body, div#comments-wrapper, .commentdata, li.cat-item, div.suf-tiles, div.suf-widget, .tagcloud, div.related, div#breadcrumbs, div#twitter-reactions, div.twtr-widget, div#sidebar_secondary, .related-content, ol.commentlist, .dna-comments_module, section#comments-area');
}

function getContentStatsWhiteListFor( inDocURL ) {
    return null;
}