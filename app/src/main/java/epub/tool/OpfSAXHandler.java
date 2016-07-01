package epub.tool;


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;

import epub.entity.EpubBook;

public class OpfSAXHandler extends DefaultHandler {

    List<EpubBook.ChapterEntity> chapterEntities;
    boolean isNavMap = false;
    boolean isText = false;
//    boolean isNavPoint = false;
//    boolean isNavLabel = false;
    String tagName = "";
    String curChapterPathName ="";
    String curData = "";
    public OpfSAXHandler(List<EpubBook.ChapterEntity> chapterEntities){
        this.chapterEntities = chapterEntities;
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        tagName = qName;
        if (qName.equals("navMap")) {
            isNavMap = true;
        }
        if (qName.equals("text")){
            isText = true;
        }
          if (isNavMap&&qName.equals("content")){
              curChapterPathName = attributes.getValue("src");
              setTocNcxChapterEntityData(curData, curChapterPathName);
          }
        super.startElement(uri, localName, qName, attributes);
    }


    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);

        if (qName.equals("navMap") && isNavMap) {
            isNavMap = false;
        }
        if (qName.equals("text")&& isNavMap){
            isText = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (isNavMap &&isText&& tagName.equals("text")) {
            curData = new String(ch, start, length);
        }
        super.characters(ch, start, length);
    }

    private void setTocNcxChapterEntityData(String data, String keyString) {
        for (EpubBook.ChapterEntity ce : chapterEntities) {
            if (keyString.equals(ce.chapter_shortPath)) {
                ce.chapter_Title = data;
                break;
            }
        }
    }

    public List<EpubBook.ChapterEntity> getTocNcxChapterData() {
        return chapterEntities;
    }

}
