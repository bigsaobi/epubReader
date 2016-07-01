package epub;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.liuqing.qepubreader.ContentLoadingProgressDialog;
import com.example.liuqing.qepubreader.R;

import org.mapdb.BTreeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

import epub.entity.BookDirEntity;
import epub.entity.ContentPage;
import epub.entity.EpubBook;
import epub.entity.PageEntity;
import epub.tool.BaseUtil;
import epub.tool.DBHelper;
import epub.tool.LostPagePreferences;
import epub.tool.Preferences;
import epub.view.EpubPageView;
import epub.view.PopoverView;


public class EpubActivity extends Activity implements PopoverView.PopoverViewDelegate {
    private ViewPager viewPager;
    private List<PageEntity> pages = new ArrayList<PageEntity>();
    private List<PageEntity> cachePages = new ArrayList<PageEntity>();
    private ArrayList<BookDirEntity> dirEntities = new ArrayList<BookDirEntity>();
    private PageEntity loadingPageEntity;
    private ProgressBar loadingProgressBar;
    private MyPagerAdapter pagerAdapter;
    private Preferences preferences;
    private int startIndex = 0;
    private EpubLoader epubLoader;
    private DBHelper dbHelper;
    private LeftBottomPupoWindow btw;
    boolean autoSkip = false;
    private int saveSumPageCount = 0;
    private long lastNotifyTime = 0;
    private Handler _handler = new Handler();
    private RelativeLayout rootView;
    private LostPagePreferences lostPagePreferences;
    private ContentLoadingProgressDialog dialog;
    private boolean nightStyle = false;

    private String filePath = "";
    private String saveFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_epublayout);
        preferences = new Preferences(this);
        lostPagePreferences = new LostPagePreferences(this);
        initViews();
        initEpubLoader();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        if (btw != null && btw.isShowing()) {
            try {
                btw.dismiss();
            } catch (Throwable e) {
            }
        } else {
            finishActivity();
        }

    }


    private void finishActivity() {
        try {
            dialog = ContentLoadingProgressDialog.showDelayed(this, null, "正在关闭...", false, false);
            EpubLoader.CloseCallBack ccb = new EpubLoader.CloseCallBack() {
                @Override
                public void onCloseEnd() {
                    if (dialog != null) {
                        dialog.dismiss();
                        dialog.cancel();
                        dialog = null;
                    }
                    EpubActivity.this.finish();
                }
            };
            epubLoader.close(ccb);
        } catch (Exception e) {
            e.printStackTrace();
            EpubActivity.this.finish();
        }
    }

    @Override
    public void finish() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.finish();

    }

    private void initViews() {
        nightStyle = preferences.getNightStyle();
        rootView = (RelativeLayout) findViewById(R.id.rootview);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        loadingProgressBar = (ProgressBar) findViewById(R.id.loading_progressbar);
        initPopMenu();
        pagerAdapter = new MyPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setOnPageChangeListener(onPageChangeListener);
        postNightSytle();
        loadingPageEntity = new PageEntity();
        loadingPageEntity.setChapterDBkey("loadingPageKey");
    }

    int offsetHeight;
    int offsetWidth;

    private void initEpubLoader() {
        epubLoader = new EpubLoader(this);
        offsetHeight = BaseUtil.dip2px(this, 65);
        offsetWidth = BaseUtil.dip2px(this, 30);
        epubLoader.setWH(getResources().getDisplayMetrics().widthPixels - offsetWidth, getResources().getDisplayMetrics().heightPixels - offsetHeight);
        epubLoader.setEpubLoaderCallBack(new EpubLoader.EpubLoaderCallBack() {
            @Override
            public void onUnZipFinish(String FilePath, String saveFilePath) {

            }

            @Override
            public void onUnZipError(String errorMsg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toastMsg("图书解压失败");
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onGetInfoFinish(EpubBook book, DBHelper dbHelper) {
                EpubActivity.this.dbHelper = dbHelper;
                epubLoader.parseChapter(book, 0);
                saveSumPageCount = preferences.getBookSumPageCount(filePath);
                startIndex = preferences.getLastExitPageIndex(filePath);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMenu();
                    }
                });
            }

            @Override
            public void onGetInfoError(final String errorMsg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("QQ", "errorMsg:" + errorMsg);
                        toastMsg("读取图书信息失败");
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                });

            }

            @Override
            public void onPagingStart(int startPageIndex) {
            }

            @Override
            public void onEachPageParseEnd(PageEntity pageEntity) {

            }

            @Override
            public void onParseFileEnd(EpubBook.ChapterEntity entity, final List<PageEntity> pageEntities, final List<BookDirEntity> dirs) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cachePages.addAll(pageEntities);
                        dirEntities.addAll(dirs);
                        if (startIndex == 0) {
                            if (pages.isEmpty()) {
                                notifyData();
                            } else {
                                if ((System.currentTimeMillis() - lastNotifyTime) >= 500) {
                                    notifyData();
                                }
                            }
                        } else {
                            if (cachePages.size() > startIndex && !autoSkip) {
                                notifyData();
                                autoSkip = true;
                                viewPager.setCurrentItem(startIndex, false);
                            }
                        }
                    }
                });
            }


            @Override
            public void onParseBookEnd(final int sumPageCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent onParseBookEndIntent = new Intent(EpubDirActivity.PRASEBOOKENDFILTER_KEY);
                        onParseBookEndIntent.putExtra(EpubDirActivity.chapterpageindexs_key, dirEntities);
                        sendBroadcast(onParseBookEndIntent);
                        preferences.setBookSumPageCount(filePath, sumPageCount);
                        notifyData();
                    }
                });

            }

            @Override
            public void onResetStart() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("qq", "onResetStart");
                        cachePages.clear();
                        dirEntities.clear();
                        pagerAdapter.notifyDataSetChanged();
                        loadingProgressBar.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onResetEnd() {
//                loadingProgressBar.setVisibility(View.GONE);
                Log.d("qq", "onResetEnd");
            }

            @Override
            public void onIOError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toastMsg("图书缓存生成失败，请检查存储卡状态后再试...");
                    }
                });
            }

        });
        filePath = getIntent().getStringExtra("filePath");
        saveFilePath = getIntent().getStringExtra("saveFilePath");
        Log.d("qq", "filePath:" + filePath);
//        Log.d("qq","saveFilePath:"+saveFilePath);
        epubLoader.load(filePath, saveFilePath);
    }


    private void notifyData() {
        if (pages.contains(loadingPageEntity)){
            pages.remove(loadingPageEntity);
        }
        pages.addAll(cachePages);
        if (epubLoader.isRunning()){
            pages.add(loadingPageEntity);
        }
        pagerAdapter.notifyDataSetChanged();
        lastNotifyTime = System.currentTimeMillis();
        cachePages.clear();
        if (loadingProgressBar.getVisibility() == View.VISIBLE) {
            loadingProgressBar.setVisibility(View.GONE);
        }
    }



    private void initPopMenu() {
        btw = new LeftBottomPupoWindow(this, new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                switch (view.getId()) {
                    case R.id.black:
                        finishActivity();
                        if (btw.isShowing()) {
                            try {
                                btw.dismiss();
                            } catch (Throwable e) {
                            }
                        }
                        break;
                    case R.id.dir:
                        Intent intent = new Intent(EpubActivity.this, EpubDirActivity.class);
                        intent.putExtra(EpubDirActivity.chapterpageindexs_key, dirEntities);
                        intent.putExtra(EpubDirActivity.curPageindex_key, viewPager.getCurrentItem());
                        startActivityForResult(intent, 200);
                        _handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (btw.isShowing()) {
                                    try {
                                        btw.dismiss();
                                    } catch (Throwable e) {
                                    }
                                }
                            }
                        }, 2000);
                        break;
                    case R.id.nightstyle:
                        nightStyle = !nightStyle;
                        preferences.setNightStyle(nightStyle);
                        postNightSytle();
                        break;
                }
            }
        }, new LeftBottomPupoWindow.PopWindowsDismiss() {
            @Override
            public void dismiss() {
                setFull(false);
            }
        });
    }

    private void postNightSytle() {
        if (nightStyle) {
            rootView.setBackgroundResource(R.color.epub_book_background_nightsytle);
        } else {
            rootView.setBackgroundResource(R.color.epub_book_background_whitesytle);
        }
        pagerAdapter.notifyDataSetChanged();
    }

    private void toNext() {
        int index = viewPager.getCurrentItem();
        if (index < (pages.size() - 1)) {
            viewPager.setCurrentItem(index + 1, true);
        }
    }

    private void toLast() {
        int index = viewPager.getCurrentItem();
        if (index > 0) {
            viewPager.setCurrentItem(index - 1, true);
        }
    }

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            preferences.setExitPageIndex(filePath, position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
    public EpubPageView.onContentClickListener listener = new EpubPageView.onContentClickListener() {
        @Override
        public void onPageCenterClick(ContentPage page) {
            showMenu();
        }

        @Override
        public void onPageLeftClick() {
            toLast();
        }

        @Override
        public void onPageRightClick() {
            toNext();
        }

        @Override
        public void onImageClick(String imagePath) {
            Intent imageIntent = new Intent(EpubActivity.this, ImageDetailActivity.class);
            imageIntent.putExtra("imagepath_key", imagePath);
            startActivity(imageIntent);
        }

        @Override
        public void onReMarkClick(String mapDbKey, String dbKey, Rect rect) {
            String htmlString = getFootNoteContent(mapDbKey, dbKey);
            if (htmlString != null) {
                Rect popoverRect = new Rect();
                int hoffset = 0;
                int voffset = 0;
                boolean down = rect.centerY() < (getResources().getDisplayMetrics().heightPixels / 2);
                if (down) {
                    hoffset = rect.width() / 2 - 10;
                    voffset = rect.height() - BaseUtil.dip2px(EpubActivity.this, 4);
                } else {
                    hoffset = rect.width() / 2 - 10;
                    voffset = rect.height() + BaseUtil.dip2px(EpubActivity.this, 2);
                }
                popoverRect.set(rect.left + hoffset, rect.top + voffset, rect.right + hoffset, rect.bottom + voffset);
                PopoverView popoverView = new PopoverView(EpubActivity.this, R.layout.popover_showed_view, nightStyle);
                popoverView.setContentText(htmlString);
                popoverView.setContentSizeForViewInPopover(new Point(1500, 1500));
                popoverView.setDelegate(EpubActivity.this);
                popoverView.showPopoverFromRectInViewGroup(rootView, popoverRect, PopoverView.PopoverArrowDirectionV, true);
            }
        }

        @Override
        public void onLinkClick(String linkString) {
            if (!TextUtils.isEmpty(linkString)) {
                Uri uri = Uri.parse(linkString);
                Intent it = new Intent(Intent.ACTION_VIEW, uri);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(it);
            }

        }
    };

    @Override
    public void popoverViewWillShow(PopoverView view) {
    }

    @Override
    public void popoverViewDidShow(PopoverView view) {
    }

    @Override
    public void popoverViewWillDismiss(PopoverView view) {
    }

    @Override
    public void popoverViewDidDismiss(PopoverView view) {
    }

    public class MyPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return pages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PageEntity entity = pages.get(position);
            View view = null;
            if (TextUtils.equals(entity.getChapterDBkey(), "loadingPageKey")){
                view = getLayoutInflater().inflate(R.layout.layout_loadingpage, null);
            }else{
                ContentPage page = getContentPage(entity);
                view = getLayoutInflater().inflate(R.layout.layout_epubpage, null);
                TextView title = (TextView) view.findViewById(R.id.chapter_name);
                TextView pageIndex = (TextView) view.findViewById(R.id.pageindex);
                EpubPageView pageView = (EpubPageView) view.findViewById(R.id.pageview);
                if (nightStyle) {
                    title.setTextColor(getResources().getColorStateList(R.color.epub_book_titleandpageindex_nightsytle));
                    pageIndex.setTextColor(getResources().getColorStateList(R.color.epub_book_titleandpageindex_nightsytle));
                } else {
                    title.setTextColor(getResources().getColorStateList(R.color.epub_book_titleandpageindex_whitesytle));
                    pageIndex.setTextColor(getResources().getColorStateList(R.color.epub_book_titleandpageindex_whitesytle));
                }
                pageView.setNightStyle(nightStyle);
                pageView.setOnContentClickListener(listener);

                if (page != null) {
                    pageView.setContentPage(page);
                    title.setText(page.getPageTitle());
                    if (saveSumPageCount != 0 && saveSumPageCount > pages.size()) {
                        pageIndex.setText((page.getPageNumber() + 1) + "/" + (saveSumPageCount));
                    } else {
                        pageIndex.setText((page.getPageNumber() + 1) + "/" + (pages.size()));
                    }
                }
            }
            view.setTag(position);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    public ContentPage getContentPage(PageEntity entity) {
        BTreeMap chapterMap = dbHelper.getMap(entity.getChapterDBkey());
        ContentPage page = null;
        if (chapterMap != null) {
            String jsonString = (String) chapterMap.get(entity.getPageDBkey());
            if (TextUtils.isEmpty(jsonString)) {
                jsonString = lostPagePreferences.getLostPageByPageKey(saveFilePath + entity.getPageDBkey());
                if (!TextUtils.isEmpty(jsonString)) {
                    page = new ContentPage();
                    page.initObj(jsonString);
                    page.setMapDBKey(entity.getChapterDBkey());
                }
            } else {
                page = new ContentPage();
                page.initObj(jsonString);
                page.setMapDBKey(entity.getChapterDBkey());
            }

        } else {
            String jsonString = lostPagePreferences.getLostPageByPageKey(saveFilePath + entity.getPageDBkey());
            if (!TextUtils.isEmpty(jsonString)) {
                page = new ContentPage();
                page.initObj(jsonString);
                page.setMapDBKey(entity.getChapterDBkey());
            }
        }
        return page;
    }

    public String getFootNoteContent(String mapDbKey, String key) {
        if (key.startsWith("xsl-footnote-link")) {
            key = key.split("#")[1];
        }
        ConcurrentNavigableMap chapterMap = dbHelper.getMap(mapDbKey);
        String footnoteString = (String) chapterMap.get(key);
        Log.d("qq", "getFootNoteContent:" + footnoteString);
        if (!TextUtils.isEmpty(footnoteString)) {
            if (!footnoteString.endsWith(">")) {
                footnoteString = footnoteString + ">";
            }
            String replaceKey = "<li id=\"" + key + "\">";
            footnoteString = footnoteString.substring(replaceKey.length(), footnoteString.length());
            footnoteString = footnoteString.substring(0, footnoteString.length() - 5);
        }

        return footnoteString;
    }

    private void showMenu() {
        try {
            btw.showAtLocation(viewPager, Gravity.LEFT, 0, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setFull(true);
    }


    protected void setFull(boolean enable) {
        if (enable) {
            showStatusBar();
        } else {
            hideStatusBar();
        }
    }


    private void hideStatusBar() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);
    }

    private void showStatusBar() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            int index = data.getIntExtra("pageindex_key", -1);
            if (index != -1) {
                if (index >= pages.size()) {
                    toastMsg("章节未解析完，请稍等...");
                }
                viewPager.setCurrentItem(index, false);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (btw.isShowing()) {
                        try {
                            btw.dismiss();
                        } catch (Throwable e) {
                        }
                    }
                    toNext();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (btw.isShowing()) {
                        try {
                            btw.dismiss();
                        } catch (Throwable e) {
                        }
                    }
                    toLast();
                }
                return true;
        }
        return super.dispatchKeyEvent(event);
    }


    private void toastMsg(final String msg) {
        Toast.makeText(EpubActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

}
