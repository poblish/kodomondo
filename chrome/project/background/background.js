var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-28859929-1']);
_gaq.push(['_trackPageview']);

(function() { // See: http://code.google.com/chrome/extensions/tut_analytics.html
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = 'https://ssl.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();

////////////////////////////////

pullDataSources();

chrome.alarms.create('Pull DataSources', {periodInMinutes: 5}); // every n mins
chrome.alarms.onAlarm.addListener( function(alarm) {
  pullDataSources();
})

function pullDataSources() {
	console.log('pullDataSources:', new Date());
	fetchDir('http://localhost:2000');
}

function fetchDir(inDir) {
	$.get(inDir)
			.error( function(xhr) { /* Anything? */ } )
			.success( function(obj) {
					try {
							if ( obj.dirs != null) {
									for ( i = 0; i < obj.dirs.length; i++) {
										fetchDir( inDir + obj.dirs[i].dir );
									}
							}
							else if ( obj.version != null) {
									fetchJar( inDir + '/' + obj.version);
							}
					} catch (e) { /* Just ignore */ }
			});
}

function fetchJar(inUrl) {
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

function recordClass(inClazz) {
}

////////////////////////////////

// See: http://www.w3.org/TR/IndexedDB/
// Pull from http://localhost:2000/org/apache/lucene/lucene-core/4.3.1/lucene-core-4.3.1.jar

var openReq = indexedDB.open("library");
// console.log('openReq', openReq);

openReq.onupgradeneeded = function(evt) {
  // The database did not previously exist, so create object stores and indexes.
  var db = evt.target.result;
  var store = db.createObjectStore("books", {keyPath: "isbn"});
  var titleIndex = store.createIndex("by_title", "title", {unique: true});
  var authorIndex = store.createIndex("by_author", "author");

  // Populate with initial data.
  store.put({title: "Quarry Memories", author: "Fred", isbn: 123456});
  store.put({title: "Water Buffaloes", author: "Fred", isbn: 234567});
  store.put({title: "Bedrock Nights", author: "Barney", isbn: 345678});
  console.log('Stored OK');
};

openReq.onsuccess = function(evt) {
  var db = evt.target.result;
  var tx = db.transaction("books", "readonly");
  var store = tx.objectStore("books");
  var index = store.index("by_title");

  var request = index.get("Bedrock Nights");
  request.onsuccess = function() {
    var matching = request.result;
    if (matching !== undefined) {
      // A match was found.
      console.log('REPORT', matching.isbn, matching.title, matching.author);
    } else {
      // No match was found.
      console.log('REPORT', null);
    }
  };
};

openReq.onerror = function() {
  report(openReq.error);
}