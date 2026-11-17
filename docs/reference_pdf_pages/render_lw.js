ObjC.import('PDFKit');
ObjC.import('AppKit');

function renderPDF(pdfPath, prefix, count) {
    var doc = $.PDFDocument.alloc.initWithURL($.NSURL.fileURLWithPath(pdfPath));
    var total = Math.min(doc.pageCount, count);
    console.log(prefix + " total pages: " + doc.pageCount + ", rendering " + total);
    for (var i = 0; i < total; i++) {
        var page = doc.pageAtIndex(i);
        var img = page.thumbnailOfSizeForBox($.NSMakeSize(1080, 2400), 1);
        var tiff = img.TIFFRepresentation;
        var rep = $.NSBitmapImageRep.imageRepsWithData(tiff).objectAtIndex(0);
        var png = rep.representationUsingTypeProperties($.NSBitmapImageFileTypePNG, $.NSDictionary.dictionary);
        var outPath = '/Users/mahesh/MasterWave/docs/reference_pdf_pages/' + prefix + '_page_' + (i + 1) + '.png';
        png.writeToFileAtomically(outPath, true);
        console.log("Rendered " + outPath);
    }
}

renderPDF('/Users/mahesh/laswave inspiration.pdf', 'lw', 18);
renderPDF('/Users/mahesh/echo music inspiration.pdf', 'echo', 10);
