package epub.entity;

import java.io.Serializable;

/**
 * Created by liuqing on 15/1/9.
 */
public class EpubBookBaseInfo implements Serializable {
    /**
     *
     */

    private String epubName; // 图书名称
    private String creator; // 作者
    private String ISBN; // ISBN
    private String language; // 语言
    private String date; // 发布时间
    private String publisher; // 出版社

    public String getEpubName() {
        return epubName;
    }

    public void setEpubName(String epubName) {
        this.epubName = epubName;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String iSBN) {
        ISBN = iSBN;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @Override
    public String toString() {
        return "EpubBookBaseInfo{" +
                "epubName='" + epubName + '\'' +
                ", creator='" + creator + '\'' +
                ", ISBN='" + ISBN + '\'' +
                ", language='" + language + '\'' +
                ", date='" + date + '\'' +
                ", publisher='" + publisher + '\'' +
                '}';
    }
}
