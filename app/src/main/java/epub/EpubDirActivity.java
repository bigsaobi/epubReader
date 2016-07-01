package epub;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.liuqing.qepubreader.R;

import java.util.List;

import epub.entity.BookDirEntity;
import epub.tool.Preferences;


/**
 * Created by liuqing on 15/1/27.
 */
public class EpubDirActivity extends Activity {
    public static final String curPageindex_key = "curpageindex_key";
    public static final String chapterpageindexs_key = "chapterpageindexs_key";
    public static final String PRASEBOOKENDFILTER_KEY = "prasebookendfilterkey";
    ListView listView;
    LinearLayout rootView;
    List<BookDirEntity> pageIndexs = null;
    DirAdapter dirAdapter = null;
    int curPageIndex = 0;
    TextView emptyView = null;
    TextView title;
    View titlediv;
    Preferences preferences;
    private boolean nightStyle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epubdir);
        preferences = new Preferences(this);
        nightStyle = preferences.getNightStyle();
        emptyView = new TextView(this);
        emptyView.setText("未读取到目录信息");
        emptyView.setTextSize(30);
        emptyView.setTextColor(Color.WHITE);
        pageIndexs = (List<BookDirEntity>) getIntent().getSerializableExtra(chapterpageindexs_key);
        curPageIndex = getIntent().getIntExtra(curPageindex_key, 0);
        rootView = (LinearLayout) findViewById(R.id.activity_epubdir_rootView);
        title = (TextView) findViewById(R.id.epubDirTitle);
        titlediv = findViewById(R.id.activity_title_div);
        listView = (ListView) findViewById(R.id.activity_epubdirListView);
        listView.setEmptyView(emptyView);
        dirAdapter = new DirAdapter();
        listView.setAdapter(dirAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i >= pageIndexs.size()) {
                    Toast.makeText(EpubDirActivity.this, "图书还未解析完成，请稍后", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(EpubDirActivity.this, EpubActivity.class);
                    intent.putExtra("pageindex_key", pageIndexs.get(i).getPageIndex());
                    setResult(RESULT_OK, intent);
                    finish();
                }

            }
        });
        postNightStyle();
        registerReceiver(praseBookEndRecvier, praseBookEndfilter);
        selListIndexFromPageIndex();

    }

    private void selListIndexFromPageIndex() {
        int listindex = 0;
        for (int i = 0; i < pageIndexs.size(); i++) {
            BookDirEntity entity = pageIndexs.get(i);
            if (entity.getPageIndex() >= curPageIndex) {
                listindex = i - 1;
                break;
            }
        }
        if (listindex >= 0) {
            listView.setSelection(listindex);
        }

    }

    private void postNightStyle() {
        if (nightStyle) {
            rootView.setBackgroundResource(R.color.epub_book_background_nightsytle);
            listView.setSelector(R.drawable.epubreader_dir_item_nightstyle);
            titlediv.setBackgroundResource(R.color.epub_bookdir_listitembg_nightstyle);
            title.setTextColor(Color.parseColor("#7b8094"));
        } else {
            rootView.setBackgroundResource(R.color.epub_book_background_whitesytle);
            listView.setSelector(R.drawable.epubreader_dir_item);
            titlediv.setBackgroundResource(R.color.main_bg);
            title.setTextColor(Color.WHITE);
        }
        dirAdapter.notifyDataSetChanged();
    }


    IntentFilter praseBookEndfilter = new IntentFilter(PRASEBOOKENDFILTER_KEY);
    BroadcastReceiver praseBookEndRecvier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (pageIndexs != null) {
                pageIndexs.clear();
                pageIndexs.addAll((List<BookDirEntity>) intent.getSerializableExtra(chapterpageindexs_key));
                dirAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(praseBookEndRecvier);
    }

    private class DirAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return pageIndexs.size();
        }

        @Override
        public Object getItem(int i) {
            return pageIndexs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            DirItemCache cache = null;
            if (view == null) {
                cache = new DirItemCache();
                view = View.inflate(EpubDirActivity.this,
                        R.layout.epubreader_listview_item, null);
                cache.name = (TextView) view
                        .findViewById(R.id.epubDirName);
                cache.pageNumber = (TextView) view
                        .findViewById(R.id.epubPageNumber);
                cache.div = (TextView) view.findViewById(R.id.epubDirDiv);
                view.setTag(cache);
            } else {
                cache = (DirItemCache) view.getTag();
            }
            if (nightStyle) {
                cache.name.setTextColor(Color.parseColor("#7b8094"));
                cache.pageNumber.setTextColor(getResources().getColorStateList(R.color.epub_book_titleandpageindex_nightsytle));
                cache.div.setBackgroundResource(R.color.epub_bookdir_listitembg_nightstyle);
            } else {
                cache.name.setTextColor(Color.BLACK);
                cache.pageNumber.setTextColor(getResources().getColorStateList(R.color.epub_book_titleandpageindex_whitesytle));
                cache.div.setBackgroundResource(R.color.epub_bookdir_listitembg_whitestyle);
            }
            if (i<pageIndexs.size()){
                BookDirEntity dirEntity = pageIndexs.get(i);
                if (dirEntity.getTagName().equals("h1")) {
                    cache.name.setPadding(0, 0, 0, 0);
                    cache.name.setText(dirEntity.getDirName());
                } else if (dirEntity.getTagName().equals("h2")) {
                    cache.name.setPadding(50, 0, 0, 0);
                    cache.name.setText(dirEntity.getDirName());
                } else if (dirEntity.getTagName().equals("h3")) {
                    cache.name.setPadding(100, 0, 0, 0);
                    cache.name.setText(dirEntity.getDirName());
                }
                if (i >= pageIndexs.size()) {
                    cache.pageNumber.setText("");
                } else {
                    cache.pageNumber.setText(String.valueOf(pageIndexs.get(i).getPageIndex() + 1));
                }
            }else{
                view.setVisibility(View.GONE);
            }

            return view;
        }

        public class DirItemCache {
            TextView name;
            TextView pageNumber;
            TextView div;
        }
    }


}
