package epub.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    public static boolean deleteSDCardFolder(File dir) {


        File to = new File(dir.getAbsolutePath() + System.currentTimeMillis());
        dir.renameTo(to);
        if (to.isDirectory()) {
            String[] children = to.list();
            for (int i = 0; i < children.length; i++) {
                File temp = new File(to, children[i]);
                if (temp.isDirectory()) {
                    deleteSDCardFolder(temp);
                } else {
                    boolean b = deleteSDCardFolder(temp);
//                    boolean b = temp.delete();
                    if (b == false) {
//                        Log.d("deleteSDCardFolder", "DELETE FAIL");
                        return false;
                    }
                }
            }
            return to.delete();
        } else {
            return to.delete();
        }
    }

    public static void deleteSDCardFolder(File dir, Preferences preferences) {
        File to = new File(dir.getAbsolutePath() + System.currentTimeMillis());
        dir.renameTo(to);
        if (to.isDirectory()) {
            String[] children = to.list();
            for (int i = 0; i < children.length; i++) {
                File temp = new File(to, children[i]);
                if (temp.isDirectory()) {
                    deleteSDCardFolder(temp);
                } else {
                    if (temp.getAbsolutePath().endsWith(".epub")) {
                        preferences.clearBookCacheByKey(temp.getAbsolutePath());
                    }
                    boolean b = temp.delete();
                    if (b == false) {
//                        Log.d("deleteSDCardFolder", "DELETE FAIL");
                    }
                }
            }
            to.delete();
        }
    }


    /**
     * 获取指定文件大小
     *
     * @param file
     * @return
     * @throws Exception
     */
    private static long getFileSize(File file) {
        FileInputStream fis = null;
        long size = 0;
        try {
            if (file.exists()) {


                fis = new FileInputStream(file);
                size = fis.available();
            }
        } catch (Throwable e) {}
        finally {
            if (fis!=null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return size;
    }

    /**
     * 获取指定文件夹
     *
     * @param f
     * @return
     * @throws Exception
     */
    public static long getFileContentSize(File f) {
        long size = 0;
        File flist[] = f.listFiles();
        if (flist != null) {
            for (int i = 0; i < flist.length; i++) {
                if (flist[i].isDirectory()) {
                    size = size + getFileContentSize(flist[i]);
                } else {
                    size = size + getFileSize(flist[i]);
                }
            }
        }
        return size;
    }


    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The file copy buffer size (10 MB) （原为30MB，为更适合在手机上使用，将其改为10MB，by
     * Geek_Soledad)
     */
    private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 10;

    public static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0;
            long count = 0;
            while (pos < size) {
                count = (size - pos) > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : (size - pos);
                pos += output.transferFrom(input, pos, count);
            }
        }catch (Throwable e){}finally {
            if (output!=null){
                output.close();
            }
            if (fos!=null){
                fos.close();
            }
            if (input!=null){
                input.close();
            }
            if (fis!=null){
                fis.close();
            }
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
        }
        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified());
        }
    }


    public static List<String> getAllFileInfo(File dirFile) {
        List<String> allPath = new ArrayList<String>();
        if (dirFile != null && dirFile.exists()) {
            File[] files = dirFile.listFiles();
            if (files != null) {
                //开始遍历所有文件
                for (int i = 0; i < files.length; i++) {

                    if (files[i].isFile()) {
                        File f = files[i];
                        allPath.add(f.getAbsolutePath());
                    } else {
                        File f = files[i];
                        allPath.addAll(getAllFileInfo(f));
                    }
                }
            }
        }
        return allPath;
    }

}
