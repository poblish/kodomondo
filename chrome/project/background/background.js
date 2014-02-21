var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-28859929-1']);
_gaq.push(['_trackPageview']);

(function() { // See: http://code.google.com/chrome/extensions/tut_analytics.html
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = 'https://ssl.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();

////////////////////////////////

// See: http://www.w3.org/TR/IndexedDB/
// Pull from http://localhost:2000/org/apache/lucene/lucene-core/4.3.1/lucene-core-4.3.1.jar

// indexedDB.deleteDatabase("visitedLinks");
var visitedLinksReq = indexedDB.open("visitedLinks");

visitedLinksReq.onupgradeneeded = function(evt) {
	var db = evt.target.result;
	db.createObjectStore("urls", {keyPath: "url"});

	var dataStore = db.createObjectStore("data", {keyPath: "id"});
	dataStore.createIndex( "simpleNameIndex", "simpleName", {unique: false});
}

var globalDb;

visitedLinksReq.onsuccess = function(evt) {
	var db = evt.target.result;
	globalDb = db;
	pullDataSources(db);  // Now we have IndexedDB, pull DataSources

	chrome.alarms.create('Pull DataSources', {periodInMinutes: 5}); // every n mins
	chrome.alarms.onAlarm.addListener( function(alarm) {
		pullDataSources(db);
	})

	function pullDataSources(inDB) {
		console.log('pullDataSources:', new Date());
		fetchDir('http://localhost:2000', inDB, function(finishedDir) {
			// Now we can say we've visited that URL
			// *FIXME* This does not quite work - some parent dirs are still left behind (FWIW...)
			inDB.transaction("urls", "readwrite").objectStore("urls").put({url: finishedDir});
			// console.log('STORE ' + finishedDir);
		} );
	}
}

function fetchDir(inDir, inDB, dirDoneHandler) {
	var lookupReq = inDB.transaction("urls", "readonly").objectStore("urls").get(inDir);
	lookupReq.onsuccess = function() {
		if (lookupReq.result !== undefined) {
			// console.log('Skip visited ' + inDir);
			return;
		}

		$.get(inDir)
				.error( function(xhr) { /* Anything? */ } )
				.success( function(obj) {
						try {
								if ( obj.dirs != null && obj.dirs.length > 0) {
									for ( i = 0; i < obj.dirs.length; i++) {
										fetchDir( inDir + obj.dirs[i].dir, inDB, dirDoneHandler);
									}
								}
								else if ( obj.version != null) {
									fetchJar( inDir + '/' + obj.version, inDB);
									dirDoneHandler(inDir);
								}
								else /* Is this possible? */ {
									dirDoneHandler(inDir);
								}
						} catch (e) { alert(e) /* Just ignore */ }
				});
	};
}

function fetchJar(inUrl, inDB) {
	$.get(inUrl)
			.error( function(xhr) { /* Anything? */ } )
			.success( function(obj) {
					try {
							if ( obj.jar != null) {
									recordClasses( obj.classes, obj.jar, inDB, 0);
							}
					} catch (e) { /* Just ignore */ }
			});
}

function recordClasses(inClasses, inParent, inDB, idx) {
	// console.log( inClasses.length + ' classes for: ' + inParent);

	var store = inDB.transaction("data", "readwrite").objectStore("data");
	idx = 0;

	putNext();

	function putNext() {
			// console.log('>>> ', inParent,idx,inClasses.length);
			if ( idx < inClasses.length) {
				var clazzName = inClasses[idx].class;
				var simpleName = clazzName.substring( clazzName.lastIndexOf('.') + 1);
				store.put({ id: clazzName, className: clazzName, simpleName: simpleName, parent: inParent}).onsuccess = putNext;
				idx++;
			}
			else {
				console.log('recordClasses() DONE for ' + inParent);
			}
	}
}

////////////////////////////////

$(document).ready(function () {
		chrome.runtime.onMessage.addListener( function( request, sender, sendResponse) {
				if (request.method == "getOptions") {
						if ( localStorage.length == 0) {		// Set defaults!
								localStorage["foo"] = true;
						}

						sendResponse({method: "getOptions", /* url: sender.tab.url, */ options: localStorage});
				}
				else if (request.method == "setBadge") {
				console.log('here');
					if ( request.score <= 0) {
						resetBadge(sender);
					}
					else {
						if ( request.score <= 3) {
							chrome.browserAction.setBadgeBackgroundColor({ color: [255,127,0, 255], tabId: sender.tab.id});
						}
						else if ( request.score <= 8) {
							chrome.browserAction.setBadgeBackgroundColor({ color: [255,70,70, 255], tabId: sender.tab.id});
						}
						else {
							chrome.browserAction.setBadgeBackgroundColor({ color: [220,0,0, 255], tabId: sender.tab.id});
						}

						localStorage['$stats.' + sender.tab.id + '.score'] = request.score;
						localStorage['$stats.' + sender.tab.id + '.url'] = request.url;

						// chrome.browserAction.setBadgeText({ text: '' + request.score, tabId: sender.tab.id});
						// chrome.browserAction.setTitle({ title: 'BannedList Score for this page: ' + request.score, tabId: sender.tab.id});
					}
				}
				else if (request.method == "resetBadge") {
					resetBadge(sender);
				}

				return true;  // See: http://stackoverflow.com/a/18484709/954442
			});

		chrome.runtime.onConnect.addListener( function(port) {
				console.assert(port.name == "termLookupPort");
				port.onMessage.addListener( function(msg) {
					if ( msg.term != null) {
						var lookupReq = globalDb.transaction("data", "readonly").objectStore("data").get(msg.term);
						lookupReq.onsuccess = function(e) {
							if ( lookupReq.result != null) {
								port.postMessage({ /* So sender can match response to request */ origTerm: msg.term, classDetails: {name: lookupReq.result.className}});
							}
						}
					}
				});
		});
});

function resetBadge(inSender) {
	chrome.browserAction.setBadgeText({ text: '', tabId: inSender.tab.id});
	// chrome.browserAction.setTitle({ title: 'BannedList Score for this page: 0', tabId: inSender.tab.id});
	localStorage.removeItem('$stats.' + inSender.tab.id + '.score');
	localStorage.removeItem('$stats.' + inSender.tab.id + '.url');
}
