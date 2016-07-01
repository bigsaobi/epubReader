package epub.entity;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;

/**
 * Created by liuqing on 15/2/2.
 */
public class BookDirEntity implements Serializable {
    int pageIndex;
    String tagName;
    String dirName;

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public void initObj(JSONObject obj){
        if (obj!=null){
            this.setPageIndex(obj.getInteger("pageIndex"));
            this.setDirName(obj.getString("dirName"));
            this.setTagName(obj.getString("tagName"));
        }
    }

    public String toJsonString() {
        String jsonString = "";
            jsonString = "{\"pageIndex\":"+pageIndex+",\"dirName\":\"" + dirName + "\",\"tagName\":\"" + tagName + "\"}";
        return jsonString;
    }

    @Override
    public String toString() {
        return "BookDirEntity{" +
                "pageIndex=" + pageIndex +
                ", tagName='" + tagName + '\'' +
                ", dirName='" + dirName + '\'' +
                '}';
    }
}
