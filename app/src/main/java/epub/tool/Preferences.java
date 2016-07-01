package epub.tool;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * 保存常用数据
 * @author liuqing
 */
public class Preferences {
	private static final String CONFIG_NAME = "epub_config";
	private SharedPreferences spf;
	private Editor editor;
	private Context context;

	public void registerChangeListener(OnSharedPreferenceChangeListener listener) {
		if (spf != null) {
			spf.registerOnSharedPreferenceChangeListener(listener);
		}
	}

	public void unRegisterChangeListener(
			OnSharedPreferenceChangeListener listener) {
		if (spf != null) {
			spf.unregisterOnSharedPreferenceChangeListener(listener);
		}
	}

	public Preferences(Context context) {
		this.context = context;
		spf = context.getSharedPreferences(CONFIG_NAME, 0);
		editor = spf.edit();
	}

	public void clear() {
		editor.clear();
		editor.commit();
	}

	/** 保存access_token */
	public void setToken(String token) {
		editor.putString("token", token);
		editor.commit();
	}

	/** 获得access_token */
	public String getToken() {
		return spf.getString("token", "");
	}

    public int getLastExitPageIndex(String bookName){
        return  spf.getInt(bookName+"_exitpageindex",0);
    }
    public void setExitPageIndex(String bookName,int pageindex){
        editor.putInt(bookName+"_exitpageindex",pageindex);
        editor.commit();
    }
    public int getBookSumPageCount(String bookName){
        return  spf.getInt(bookName+"_sumpagecount",0);
    }
    public void setBookSumPageCount(String bookName,int sumPageCount){
        editor.putInt(bookName+"_sumpagecount",sumPageCount);
        editor.commit();
    }
    public boolean getNightStyle(){
        return  spf.getBoolean("nightSytle_key",false);
    }
    public void setNightStyle(boolean nightStyle){
        editor.putBoolean("nightSytle_key",nightStyle);
        editor.commit();
    }
    public void clearBookCacheByKey(String bookName){
        editor.putInt(bookName+"_exitpageindex",0);
        editor.putInt(bookName+"_sumpagecount",0);
        editor.commit();
    }



}
