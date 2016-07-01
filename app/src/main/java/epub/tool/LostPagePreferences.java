package epub.tool;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

/**
 * 保存常用数据
 * 
 * @author wcs 2014/6/30
 * 
 */
public class LostPagePreferences {
	private static final String CONFIG_NAME = "epubbook_lostpage_config";
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

	public LostPagePreferences(Context context) {
		this.context = context;
		spf = context.getSharedPreferences(CONFIG_NAME, 0);
		editor = spf.edit();
	}

	public void clear() {
		editor.clear();
		editor.commit();
	}


    public String getLostPageByPageKey(String pagekey){
        Log.d("qq", "getLostPageByPageKey:" + pagekey);
        return  spf.getString(pagekey,"");
    }
    public void saveLostPage(String pageKey,String pageString){
        Log.d("qq", "saveLostPage:" + pageKey);
        editor.putString(pageKey,pageString);
        editor.commit();
    }



}
