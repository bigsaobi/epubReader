package epub.entity;


import android.text.Html;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;

public class Content implements Serializable {


    public void initObj(String jsonString) {
        JSONObject obj = JSON.parseObject(jsonString);
        x = obj.getFloat("x");
        y = obj.getFloat("y");
        content = obj.getString("content");
        tagName = obj.getString("tagName");
        tagClass = obj.getString("tagClass");
    }

    public String toJsonString() {
//        String jsonString = "";
//
//        if (content.equals("\"")) {
//            jsonString = "{\"tagClass\":\""+tagClass+"\",\"x\":" + x + ",\"y\":" + y + ",\"content\":\"\\\"\",\"tagName\":\"" + tagName + "\"}";
//        } else {
//            if (content.contains("\"")){
//                content.replace("\"","");
//            }
//            jsonString = "{\"tagClass\":\""+tagClass+"\",\"x\":" + x + ",\"y\":" + y + ",\"content\":\"" + content + "\",\"tagName\":\"" + tagName + "\"}";
//        }
        if(content.contains("&")){
            content = Html.fromHtml(content).toString();
        }
        JSONObject obj = new JSONObject();
        obj.put("x",x);
        obj.put("y",y);
        obj.put("content",content);
        obj.put("tagName",tagName);
        obj.put("tagClass",tagClass);
        return obj.toJSONString();
    }


    private String content;
    private String tagName;
    private float x, y;
    private String tagClass;

    public String getTagClass() {
        return tagClass;
    }

    public void setTagClass(String tagClass) {
        this.tagClass = tagClass;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void appendText(String appendText) {
        content = content + appendText;
//        content.replace("\"","'");
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Content{" +
                "content='" + content + '\'' +
                ", tagName='" + tagName + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", tagClass='" + tagClass + '\'' +
                '}';
    }
}
