package epub.entity;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;

/**
 * Created by liuqing on 15/1/24.
 */
public class PageEntity implements Serializable {
    String chapterDBkey;
    String pageDBkey;

    public void initObj(JSONObject obj){
        if (obj!=null){
            this.chapterDBkey = obj.getString("chapterDBkey");
            this.pageDBkey = obj.getString("pageDBkey");
        }
    }


    public String getChapterDBkey() {
        return chapterDBkey;
    }

    public void setChapterDBkey(String chapterDBkey) {
        this.chapterDBkey = chapterDBkey;
    }

    public String getPageDBkey() {
        return pageDBkey;
    }

    public void setPageDBkey(String pageDBkey) {
        this.pageDBkey = pageDBkey;
    }
}
