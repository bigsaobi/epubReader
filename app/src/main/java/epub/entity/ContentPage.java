package epub.entity;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuqing on 15/1/9.
 * ContentPage 是某一页里面的全部内容
 */
public class ContentPage implements Serializable {

    private String pageTitle;
    private int PageCount;
    private int pageNumber;
    private List<Content> contents;
    private String mapDBKey;

    public void initObj(String jsonString) throws JSONException {
//        Log.d("----", "jsonString:" + jsonString);
        JSONObject obj = JSON.parseObject(jsonString);
        contents = new ArrayList<Content>();
        JSONArray array = obj.getJSONArray("contents");
        for (int i = 0; i < array.size(); i++) {
            Content content = new Content();
            content.initObj(array.get(i).toString());
            contents.add(content);
        }
        pageTitle = obj.getString("pageTitle");
        PageCount = obj.getInteger("PageCount");
        pageNumber = obj.getInteger("pageNumber");

    }

    public String toJsonString() {
        String jsonString = "";
        jsonString = "\"pageTitle\":" + "\"" + pageTitle + "\",\"PageCount\":" + PageCount + ",\"pageNumber\":" + pageNumber + ",";
        String contentsString = "";
        for (int i = 0; i < contents.size(); i++) {
            if (i != contents.size() - 1) {
                contentsString = contentsString + contents.get(i).toJsonString() + ",";
            } else {
                contentsString = contentsString + contents.get(i).toJsonString();
            }
        }
        contentsString = "[" + contentsString + "]";
        jsonString = "{"+ jsonString +"\"contents\":" + contentsString + "}";
        return jsonString;
    }

    public String getDBKey(){
        return pageTitle+"_"+pageNumber;
    }
    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public int getPageCount() {
        return PageCount;
    }

    public void setPageCount(int pageCount) {
        PageCount = pageCount;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }
    public String getMapDBKey() {
        return mapDBKey;
    }

    public void setMapDBKey(String mapDBKey) {
        this.mapDBKey = mapDBKey;
    }
    @Override
    public String toString() {
        return "ContentPage{" +
                "pageTitle='" + pageTitle + '\'' +
                ", PageCount=" + PageCount +
                ", pageNumber=" + pageNumber +
                ", contents=" + contents +
                '}';
    }
}
