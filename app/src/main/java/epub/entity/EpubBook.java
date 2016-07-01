package epub.entity;

import java.io.Serializable;
import java.util.ArrayList;

public class EpubBook implements Serializable {

	private static final long serialVersionUID = 1L;
	private EpubBookBaseInfo epubInfo;
	private String ncxPath;
    private ArrayList<ChapterEntity> chapterEntities;


	public String getNcxPath() {
		return ncxPath;
	}

	public void setNcxPath(String ncxPath) {
		this.ncxPath = ncxPath;
	}

	public EpubBookBaseInfo getEpubInfo() {
		return epubInfo;
	}

	public void setEpubInfo(EpubBookBaseInfo epubInfo) {
		this.epubInfo = epubInfo;
	}

    public String getPageListKey(){
        return getChapterEntities().toString();
    }

    public String getPageKey(ContentPage page){
        return getEpubInfo().toString()+page.getPageNumber();
    }
    public String getDBName(){
        return  getEpubInfo().toString();
    }

    public String getDirDBName(){
        return getEpubInfo().toString()+"_dirs";
    }


    public ArrayList<ChapterEntity> getChapterEntities() {
        return chapterEntities;
    }

    public void setChapterEntities(ArrayList<ChapterEntity> chapterEntities) {
        this.chapterEntities = chapterEntities;
    }

    public String getBookContentDBName(){
        return getEpubInfo()+"_chapters";
    }

    public String getBookReadInfoDBName(){
        return  getEpubInfo()+"_readinfo";
    }

    @Override
    public String toString() {
        String htmlNamesString = "";
        String htmlPathsString  = "";
        for (ChapterEntity entity:chapterEntities){
            htmlPathsString = htmlPathsString+"-"+entity.chapter_shortPath;
            htmlNamesString = htmlNamesString+"-"+entity.chapter_Title;
        }
        return "EpubBook{" +
                "epubInfo=" + epubInfo +
                ", ncxPath='" + ncxPath + '\'' +
                ", htmlNames=" + htmlNamesString +
                ", htmlPaths=" + htmlPathsString +
                '}';
    }

    public static class ChapterEntity implements Serializable {
        public String chapter_FullPath = "";
        public String chapter_shortPath ="";
        public String chapter_Title = "";


        public String getChapterPageListKey(){
            return  chapter_FullPath+"_pagelistkey";
        }

        public String getChapterDirListKey(){
            return  chapter_FullPath+"_dirlistkey";
        }
        @Override
        public String toString() {
            return "ChapterEntity{" +
                    "chapter_FullPath='" + chapter_FullPath + '\'' +
                    ", chapter_shortPath='" + chapter_shortPath + '\'' +
                    ", chapter_Title='" + chapter_Title + '\'' +
                    '}';
        }
    }
}