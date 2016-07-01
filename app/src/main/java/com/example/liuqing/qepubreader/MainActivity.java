package com.example.liuqing.qepubreader;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import epub.EpubActivity;

public class MainActivity extends AppCompatActivity {
    ListView listView;
    BookAdapter adapter;
    SwipeRefreshLayout swipeRefreshLayout;
    List<BookPathInfo> infos = new ArrayList<>();
    private static final String basePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "QEpubReadCache";
    TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        empty = (TextView) findViewById(R.id.empty);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getAllBook();
            }
        });
        listView = (ListView) findViewById(R.id.allbooklist);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String epubFile = infos.get(position).path;
                Intent intent = new Intent(MainActivity.this, EpubActivity.class);
                intent.putExtra("filePath", epubFile);
                intent.putExtra("saveFilePath", epubFile.substring(0, epubFile.lastIndexOf(".")));
                startActivity(intent);
            }
        });
        adapter = new BookAdapter();
        listView.setAdapter(adapter);
        getAllBook();
    }

    private void getAllBook() {

        new Thread() {
            @Override
            public void run() {
                super.run();
                infos.clear();
                File tarFile = new File(basePath);
                File[] files = tarFile.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        String fileName = files[i].getAbsolutePath();
                        if (fileName.endsWith(".epub") && !files[i].isDirectory()) {
                            String name = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());
                            BookPathInfo info = new BookPathInfo();
                            info.name = name;
                            info.path = fileName;
                            infos.add(info);
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            if (!isDestroyed() && !isFinishing()) {
                                swipeRefreshLayout.setRefreshing(false);
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            swipeRefreshLayout.setRefreshing(false);
                            adapter.notifyDataSetChanged();
                        }

                    }
                });
            }
        }.start();


    }

    public class BookAdapter extends BaseAdapter {

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            if (infos.isEmpty()) {
                empty.setVisibility(View.VISIBLE);
            } else {
                empty.setVisibility(View.GONE);
            }
        }

        @Override
        public int getCount() {
            return infos.size();
        }

        @Override
        public Object getItem(int position) {
            return infos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(MainActivity.this);
            }
            TextView textView = (TextView) convertView;
            convertView.setPadding(10, 10, 10, 10);
            ((TextView) convertView).setTextSize(25);
            BookPathInfo info = infos.get(position);
            textView.setText(info.name);
            return convertView;
        }
    }

    public static class BookPathInfo {
        String name;
        String path;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
