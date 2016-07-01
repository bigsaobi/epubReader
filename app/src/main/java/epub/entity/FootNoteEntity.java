package epub.entity;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by liuqing on 15/3/6.
 */
public class FootNoteEntity {
    private String id;
    private String content;
    public void initObj(JSONObject obj){
        if (obj!=null){
            setId(obj.getString("id"));
            setContent(obj.getString("content"));
        }
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void appentContent(String appentText){
        content = content +appentText;
    }

    @Override
    public String toString() {
        return "FootNoteEntity{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
