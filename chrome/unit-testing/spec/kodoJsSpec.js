// ...

describe("RESULT_CLICK_MetaTagsOnly", function() {
    it("Matches expected text", function() {
    	var text = getDocumentText();
        expect(text).toMatch(/^Hello/);
    });
});