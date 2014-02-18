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
  console.log('Create store');
	store = db.createObjectStore("urls", {keyPath: "url"});
}

visitedLinksReq.onsuccess = function(evt) {
	var db = evt.target.result;
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
									console.log( obj.classes.length + ' classes for: ' + obj.jar);
									for ( i = 0; i < obj.classes.length; i++) {
										recordClass( obj.classes[i].class );
									}
							}
					} catch (e) { /* Just ignore */ }
			});
}

function recordClass(inClazz, inDB) {
}
