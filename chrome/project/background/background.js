var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-28859929-1']);
_gaq.push(['_trackPageview']);

(function() { // See: http://code.google.com/chrome/extensions/tut_analytics.html
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = 'https://ssl.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();

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