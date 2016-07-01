package epub;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.mapdb.BTreeMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import epub.entity.BookDirEntity;
import epub.entity.EpubBook;
import epub.entity.PageEntity;
import epub.tool.ChapterParser;
import epub.tool.ContainerSAXHandler;
import epub.tool.ContentOpfSAXHandler;
import epub.tool.DBHelper;
import epub.tool.FileUtil;
import epub.tool.LostPagePreferences;
import epub.tool.OpfSAXHandler;
import epub.tool.ZipUtil;

/**
 * Created by liuqing on 15/1/9.
 */
public class EpubLoader {
    private static final String TAG = "EpubLoader";
    private static final boolean isDebug = false;
    private String saveFilePath;
    private int displayWidth = 0;
    private int displayHeight = 0;
    private EpubBook book;
    private DBHelper dbHelper;
    private ExecutorService epubLoaderTask = Executors.newFixedThreadPool(1);
    private Context ctx;
    private EpubLoaderCallBack callBack;
    private CloseCallBack ccb;
    int bookPageIndex = 0;
    private boolean stop = false;
    private boolean isRunning = false;
    private LostPagePreferences lostPagePreferences = null;
    private boolean errorBreak;

    ChapterParser.ParsePageListener parsePageListener = new ChapterParser.ParsePageListener() {

        @Override
        public void onParsePageEnd(PageEntity pageEntity) {
            if (callBack != null)
                callBack.onEachPageParseEnd(pageEntity);
        }

        @Override
        public void onParsePageStart(int pageIndex) {
            if (callBack != null)
                callBack.onPagingStart(pageIndex);
        }
    };

    EpubParseErrorListener errorListener = new EpubParseErrorListener() {

        @Override
        public void onErrorHappen(List<EpubBook.ChapterEntity> chapterEntities) {
            if (callBack != null) {
                callBack.onResetStart();
                deleteDbCache(saveFilePath);
                callBack.onResetEnd();
            }
            startLoadContentPages(EpubLoader.this.saveFilePath, chapterEntities);
        }
    };

    /**
     * 预读数
     */
    private int preFetchOffset = 3;

    public EpubLoader(Context ctx) {
        this.ctx = ctx;
        this.lostPagePreferences = new LostPagePreferences(ctx);
    }

    public void setEpubLoaderCallBack(EpubLoaderCallBack callBack) {
        this.callBack = callBack;
    }


    /**
     * must call before load
     */
    public void setPreFetchOffset(int preFetchOffset) {
        this.preFetchOffset = preFetchOffset;
    }

    public void load(String FilePath, String saveFilePath) {
        if (isDebug) {
            Log.d(TAG, "load start----------------------------");
        }
        this.saveFilePath = saveFilePath;
        startUnZip(FilePath, saveFilePath);
    }

    public void parseChapter(EpubBook book, int startLoadHtmlIndex) {
        if (!TextUtils.isEmpty(saveFilePath) && book != null) {
            this.book = book;
            startLoadContentPages(saveFilePath, book.getChapterEntities());
        }
    }

    public void setWH(int width, int height) {
        this.displayWidth = width;
        this.displayHeight = height;
    }

    public void close(CloseCallBack ccb) throws Exception {
        if (isRunning) {
            stop = true;
            this.ccb = ccb;
        } else {
            ccb.onCloseEnd();
            shutdown();
        }
    }

    public void shutdown() {
        epubLoaderTask.shutdown();
        dbHelper.close();
    }

    public boolean isRunning(){
        return isRunning;
    }


    public void startUnZip(String FilePath, String saveFilePath) {
        if (isDebug) {
            Log.d(TAG, "startUnZip");
        }
        new Thread(new UnZipRunnable(FilePath, saveFilePath)).start();
    }

    public void startGetEpubInfo(String unZipFilePath) {
        if (isDebug) {
            Log.d(TAG, "startGetEpubInfo");
        }
        new Thread(new GetEpubInfoRunnable(unZipFilePath)).start();
    }

    public void startLoadContentPages(String rootPath, List<EpubBook.ChapterEntity> chapterEntities) {
        if (chapterEntities != null && !chapterEntities.isEmpty()) {
            epubLoaderTask.execute(new Load2ContentPages(rootPath, chapterEntities));
        } else {
            if (callBack != null)
                callBack.onGetInfoError("html paths is empty!");
        }
    }


    /**
     * 解压epub文件
     */
    public class UnZipRunnable implements Runnable {
        String epubFilePath;
        String unZipFilePath;

        public UnZipRunnable(String epubFilePath, String unZipFilePath) {
            this.epubFilePath = epubFilePath;
            this.unZipFilePath = unZipFilePath;
        }

        @Override
        public void run() {
            try {
                File unZipFile = new File(unZipFilePath);
                File zipFile = new File(epubFilePath);
                long unZipFileSize = FileUtil.getFileContentSize(unZipFile);
                long zipFileSize = FileUtil.getFileContentSize(zipFile);
                if (!unZipFile.exists()||unZipFileSize<zipFileSize){
                    ZipUtil.unZipFiles(epubFilePath, unZipFilePath);
                }else{
                    Log.d("qq", "file exist and skip unzip step");
                }
                startGetEpubInfo(unZipFilePath);
                if (callBack != null)
                    callBack.onUnZipFinish(epubFilePath, unZipFilePath);
            } catch (IOException e) {
                if (callBack != null)
                    callBack.onUnZipError(e.getMessage());
            }
        }
    }


    /**
     * 根据解压出来的文件读取epub图书信息
     */
    public class GetEpubInfoRunnable implements Runnable {
        String unZipFilePath;

        public GetEpubInfoRunnable(String unZipFilePath) {
            this.unZipFilePath = unZipFilePath;
        }

        @Override
        public void run() {
            try {
                dbHelper = new DBHelper(this.unZipFilePath);
            } catch (Exception e) {
//                Log.d("qq","error");
                if (e instanceof IOException) {
                    callBack.onIOError();
                    deleteDbCache(this.unZipFilePath);
                    dbHelper = new DBHelper(this.unZipFilePath);
                }
            }

            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser parser = factory.newSAXParser();
                File metaInfPathFile = new File(unZipFilePath + File.separator + "META-INF" + File.separator + "container.xml");

                ContainerSAXHandler containerSAXHandler = new ContainerSAXHandler();
                parser.parse(metaInfPathFile, containerSAXHandler);
                String containerFullPath = containerSAXHandler.getContainerFullPath();
                String contentOpfPath = unZipFilePath + File.separator + containerFullPath;
                if (isDebug) {
                    Log.d(TAG, "contentOpfPath:" + contentOpfPath);
                }
                File contentOpfFile = new File(contentOpfPath);
                ContentOpfSAXHandler contentOpfSAXHandler = new ContentOpfSAXHandler();
                parser.parse(contentOpfFile, contentOpfSAXHandler);
                EpubBook opfContent = contentOpfSAXHandler.getOpfContent();
                String opsDirPath = new File(contentOpfPath).getParent() + File.separator;
                if (isDebug) {
                    Log.d(TAG, "opsDirPath:" + opsDirPath);
                }
                if (isDebug) {
                    for (int i = 0; i < opfContent.getChapterEntities().size(); i++) {
                        Log.d(TAG, "ncxTocs:" + opfContent.getChapterEntities().get(i).toString());
                    }
                }
                opfContent.setNcxPath(opsDirPath + opfContent.getNcxPath());
                if (isDebug) {
                    Log.d(TAG, "opfContent.getNcxPath():" + opfContent.getNcxPath());
                }
                OpfSAXHandler opfSAXHandler = new OpfSAXHandler(opfContent.getChapterEntities());
                parser.parse(new File(opfContent.getNcxPath()), opfSAXHandler);
                opfContent.getChapterEntities().remove(0);
                if (isDebug) {
                    for (int i = 0; i < opfContent.getChapterEntities().size(); i++) {
                        Log.d(TAG, "ncxTocs:" + opfContent.getChapterEntities().get(i).chapter_Title + "--" + opfContent.getChapterEntities().get(i).chapter_shortPath);
                    }
                }
                for (EpubBook.ChapterEntity entity : opfContent.getChapterEntities()) {
                    entity.chapter_FullPath = opsDirPath + entity.chapter_shortPath;
                }
                if (isDebug) {
                    Log.d(TAG, "opfContent.toString:" + opfContent.toString());
                }

                if (callBack != null)
                    callBack.onGetInfoFinish(opfContent, dbHelper);

            } catch (Exception e) {
                e.printStackTrace();
                if (callBack != null)
                    callBack.onGetInfoError(e.getMessage());
            }
        }
    }

    public class Load2ContentPages implements Runnable {
        String rootPath = "";
        //        EpubBook.ChapterEntity chapterEntity = null;
        List<EpubBook.ChapterEntity> chapterEntities = null;

        public Load2ContentPages(String rootPath, List<EpubBook.ChapterEntity> chapterEntities) {
            this.chapterEntities = chapterEntities;
            this.rootPath = rootPath;
        }

        @Override
        public void run() {
            parse(rootPath, chapterEntities);
        }

    }

    //    boolean firstError = true;
    private void parse(String rootPath, List<EpubBook.ChapterEntity> chapterEntities) {
        errorBreak = false;
        isRunning = true;
        bookPageIndex = 0;
        for (int i = 0; i < chapterEntities.size(); i++) {
            if (stop) {
                if (ccb != null) {
                    ccb.onCloseEnd();
                }
                shutdown();
                break;
            }
            EpubBook.ChapterEntity chapterEntity = chapterEntities.get(i);
            BTreeMap chapterMap = null;
            chapterMap = dbHelper.getMap(chapterEntity.chapter_FullPath);
            ArrayList<PageEntity> pageEntities = new ArrayList<PageEntity>();
            ArrayList<BookDirEntity> dirEntities = new ArrayList<BookDirEntity>();
            if (chapterMap == null) {
                errorBreak = true;
                break;
            }
            if (chapterMap.isEmpty() || !chapterMap.containsKey(chapterEntity.getChapterPageListKey()) || (chapterMap.get(chapterEntity.getChapterPageListKey()) == null)) {
                ChapterParser cp = new ChapterParser(ctx, rootPath, chapterEntity, bookPageIndex, 0, parsePageListener);
                cp.getFootNoteEntities();
                cp.setDisPlayWH(displayWidth, displayHeight);
                cp.setStoreMap(chapterMap, lostPagePreferences);
                ArrayList<PageEntity> subPageEntities = cp.parseHtmlFile();
                if (subPageEntities != null && !subPageEntities.isEmpty()) {
                    pageEntities.addAll(subPageEntities);
                    dirEntities.addAll(cp.getDirEntities());
                    String pageEntitiesJsonString = JSONObject.toJSON(pageEntities).toString();
                    chapterMap.put(chapterEntity.getChapterPageListKey(), pageEntitiesJsonString);
                    String dirEntitiesJsonString = JSONObject.toJSON(dirEntities).toString();
                    chapterMap.put(chapterEntity.getChapterDirListKey(), dirEntitiesJsonString);
                    dbHelper.commit();
                }
            } else {
                pageEntities.addAll(getPageEntities((String) chapterMap.get(chapterEntity.getChapterPageListKey())));
                dirEntities.addAll(getDirEntities((String) chapterMap.get(chapterEntity.getChapterDirListKey())));
            }
            bookPageIndex = bookPageIndex + pageEntities.size();
            if (callBack != null)
                callBack.onParseFileEnd(chapterEntity, pageEntities, dirEntities);
        }
        if (!errorBreak) {
            if (!stop) {
                if (callBack != null) {
                    callBack.onParseBookEnd(bookPageIndex);
                }
            }
            isRunning = false;
            if (stop) {
                if (ccb != null) {
                    ccb.onCloseEnd();
                }
                shutdown();
            }
        } else {
            if (errorListener != null) {
                errorListener.onErrorHappen(chapterEntities);
            }
        }

    }


    private ArrayList<PageEntity> getPageEntities(String jsonString) {
        ArrayList<PageEntity> pageEntities = new ArrayList<PageEntity>();
        if (!TextUtils.isEmpty(jsonString)) {
            JSONArray array = JSON.parseArray(jsonString);
            int count = array.size();
            for (int i = 0; i < count; i++) {
                PageEntity entity = new PageEntity();
                entity.initObj(array.getJSONObject(i));
                pageEntities.add(entity);
            }
        }
        return pageEntities;
    }

    public List<BookDirEntity> getDirEntities(String jsonString) {
        List<BookDirEntity> entities = new ArrayList<BookDirEntity>();
        if (!TextUtils.isEmpty(jsonString)) {
            JSONArray array = JSONArray.parseArray(jsonString);
            for (int i = 0; i < array.size(); i++) {
                BookDirEntity entity = new BookDirEntity();
                entity.initObj(array.getJSONObject(i));
                entities.add(entity);
            }
        }
        return entities;
    }

    public void deleteDbCache(String path) {
        File tarFile = new File(path);
        File[] files = tarFile.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                if (fileName.startsWith("db") && !files[i].isDirectory()) {
                    files[i].delete();
                }
            }
        }
    }


    public interface EpubLoaderCallBack {
         void onUnZipFinish(String FilePath, String saveFilePath);

         void onUnZipError(String errorMsg);

         void onGetInfoFinish(EpubBook book, DBHelper dbHelper);

         void onGetInfoError(String errorMsg);

         void onPagingStart(int startPageIndex);

         void onEachPageParseEnd(PageEntity pageEntity);

         void onParseFileEnd(EpubBook.ChapterEntity entity, List<PageEntity> pageEntities, List<BookDirEntity> dirs);

         void onParseBookEnd(int sumPageCount);

         void onResetStart();

         void onResetEnd();

         void onIOError();
    }

    public interface EpubParseErrorListener {
         void onErrorHappen(List<EpubBook.ChapterEntity> chapterEntities);
    }


    public interface CloseCallBack {
         void onCloseEnd();
    }
}
