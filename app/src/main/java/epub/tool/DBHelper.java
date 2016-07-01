package epub.tool;


import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;

/**
 * Created by liuqing on 15/1/24.
 */
public class DBHelper {
    private DB db;
    String dbPath = "";
    public static final int DBVersion = 5;//2015.7.17对图书p class＝qt进行支持

    public DBHelper(String sdPath) {
        dbPath = sdPath + File.separator + "db_" + DBVersion;
        File mpDbFile = new File(sdPath);
        if (!mpDbFile.exists()) {
            mpDbFile.mkdir();
        }
        initDB();
    }

    private void initDB() {
        try {
            this.db = DBMaker.newFileDB(new File(dbPath)).compressionEnable().asyncWriteEnable().closeOnJvmShutdown().mmapFileEnableIfSupported().make();
        } catch (Throwable e) {
            try {
                this.db = DBMaker.newFileDB(new File(dbPath)).compressionEnable().asyncWriteEnable().closeOnJvmShutdown().mmapFileEnableIfSupported().make();
            } catch (Throwable error2) {
            }
        }
    }

    public BTreeMap getMap(String chapterDBName) {

        BTreeMap map = null;
        if (db != null && !db.isClosed()) {
            try {
                map = db.getTreeMap(chapterDBName);
            } catch (RuntimeException e) {
                e.printStackTrace();
//                db.close();
                initDB();
                try {
                    map = db.getTreeMap(chapterDBName);
                } catch (RuntimeException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return map;
    }

    public void commit() {
        if (db != null && !db.isClosed()) {
            try {
                db.commit();
            } catch (Throwable throwable) {
            }
        }
    }

    public boolean isClose() {
        if (db != null && !db.isClosed()) {
            return false;
        }
        return true;
    }


    public void close() {
        if (db != null && !db.isClosed()) {
            try {
                db.commit();
                db.close();
            } catch (Throwable throwable) {
            }

        }
    }
}
