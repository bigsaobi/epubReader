package epub.tool;

import android.text.TextUtils;


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

import epub.entity.EpubBook;
import epub.entity.EpubBookBaseInfo;


public class ContentOpfSAXHandler extends DefaultHandler {

	private String tagName;
	private EpubBook opfContent;
	private EpubBookBaseInfo epubInfo;
	private ArrayList<EpubBook.ChapterEntity> htmlNames;

	@Override
	public void startDocument() throws SAXException {
		opfContent = new EpubBook();
		epubInfo = new EpubBookBaseInfo();
		htmlNames = new ArrayList<EpubBook.ChapterEntity>();
		super.startDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		tagName = qName;
		if ("item".equalsIgnoreCase(tagName)) {
			if ("ncx".equalsIgnoreCase(attributes.getValue("id"))) {
				getOpfContent().setNcxPath(attributes.getValue("href"));
			}
		} else if ("itemref".equalsIgnoreCase(tagName)) {
            EpubBook.ChapterEntity entity = new EpubBook.ChapterEntity();
            entity.chapter_shortPath=attributes.getValue("idref") + ".html";
			htmlNames.add(entity);
		}

		super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String data = new String(ch, start, length);
		if (!TextUtils.isEmpty(data.trim())) {
			if ("dc:title".equalsIgnoreCase(tagName)) {
				epubInfo.setEpubName(data);
			} else if ("dc:creator".equalsIgnoreCase(tagName)) {
				epubInfo.setCreator(data);
			} else if ("dc:identifier".equalsIgnoreCase(tagName)) {
				epubInfo.setISBN(data);
			} else if ("dc:language".equalsIgnoreCase(tagName)) {
				epubInfo.setLanguage(data);
			} else if ("dc:date".equalsIgnoreCase(tagName)) {
				epubInfo.setDate(data);
			} else if ("dc:publisher".equalsIgnoreCase(tagName)) {
				epubInfo.setPublisher(data);
			}
		}
		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
	};

	@Override
	public void endDocument() throws SAXException {
		getOpfContent().setEpubInfo(epubInfo);
		getOpfContent().setChapterEntities(htmlNames);
        super.endDocument();

	}

	public EpubBook getOpfContent() {
		return opfContent;
	}

}
