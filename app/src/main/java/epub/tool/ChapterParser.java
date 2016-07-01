package epub.tool;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Log;


import org.mapdb.BTreeMap;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import epub.entity.BookDirEntity;
import epub.entity.Content;
import epub.entity.ContentPage;
import epub.entity.EpubBook;
import epub.entity.FootNoteEntity;
import epub.entity.PageEntity;

/**
 * Created by liuqing on 15/1/15.
 */
public class ChapterParser {

    private static final String QQ_FOOTNOTECLASS = "xsl-footnote-link";//"citic-footnote";//"xsl-footnote";//

    private Context ctx;
    private String rootPath;
    private EpubBook.ChapterEntity entity;
    private int startParsingByteNumber;

    public static final int paddingV = 50;
    private List<ContentPage> pages = new ArrayList<ContentPage>();
    private Stack<TagStackEntity> tagStack = new Stack<TagStackEntity>();
    private Paint paint;

    private float displayHeight;
    private float displayWidth;


    private boolean tagging;
    private String tagName = "";
    //    private String curTagName = "";
//    private String curTagClass = "";
    private TagStackEntity curTagEntity = null;
    private boolean curHtmlSinglePage = false;
    //默认的文字颜色是黑色
    private int TEXTCOLOR = Color.BLACK;
    //行高
    private int LINEHEIGHT_S = 22;
    private int LINEHEIGHT = 30;//单行高度
    private int SECTIONHEIGHT = 40;
    private int TEXTSIZE_H1 = 22;
    private int TEXTSIZE_H2 = 21;
    private int TEXTSIZE_H3 = 20;
    private int TEXTSIZE_H4 = 19;
    private int TEXTSIZE_H5 = 18;
    private int TEXTSIZE_H6 = 17;
    private int TEXTSIZE_SUPB = 10;
    private int TEXTSIZE_I = 18;
    private int TEXTSIZE_UL = 18;
    private int TEXTSIZE_LI = 18;
    private int TEXTSIZE_OTHER = 18;

    //标记单个文字属性
    int curTextSize = 40;
    //当前行宽度在当前字号下一行能显示多少字
    int sinlgeLineTextCount = 0;
    //标记当前是否是<body>
    boolean isTextBody = false;
    boolean isLoadFootNote = false;
    private static float singleTextWidth = 55;//单个文字宽度
    private float curTextWidth = 0;
    private float curTextHeight = 0;
    public int paddingLeft = 3;//水品方向间隔距离
    public int paddingRight = 17;
    private int paddingTop = 33;
    private int paddingBottom = 33;


    private float x = paddingLeft;
    private float y = paddingTop;

    private int pageIndex = 0;
    private int lineIndex = 0;

    ParsePageListener parseListener;
    private ArrayList<Content> curPageContents;
    private ArrayList<PageEntity> pageDBKeys = new ArrayList<PageEntity>();
    private ArrayList<FootNoteEntity> footNoteEntities = new ArrayList<FootNoteEntity>();
    FootNoteEntity curFootNoteEntity = null;
    Content curContent = null;
    BTreeMap pageStoreMap = null;
    LostPagePreferences lostPagePreferences = null;
    ArrayList<BookDirEntity> dirEntities = new ArrayList<BookDirEntity>();

    public ChapterParser(Context ctx, String rootPath, EpubBook.ChapterEntity entity, int startParsePageIndex, int startParsingByteNumber, ParsePageListener parseListener) {
        this.ctx = ctx;
        this.rootPath = rootPath;
        this.entity = entity;
        this.displayHeight = ctx.getResources().getDisplayMetrics().heightPixels;
        this.displayWidth = ctx.getResources().getDisplayMetrics().widthPixels;
        this.startParsingByteNumber = startParsingByteNumber;
        this.pageIndex = startParsePageIndex;
        this.parseListener = parseListener;
        paint = new Paint();
        paint.setTextSize(25);
        paint.setColor(Color.WHITE);

        TEXTSIZE_H1 = BaseUtil.dip2px(ctx, TEXTSIZE_H1);
        TEXTSIZE_H2 = BaseUtil.dip2px(ctx, TEXTSIZE_H2);
        TEXTSIZE_H3 = BaseUtil.dip2px(ctx, TEXTSIZE_H3);
        TEXTSIZE_H4 = BaseUtil.dip2px(ctx, TEXTSIZE_H4);
        TEXTSIZE_H5 = BaseUtil.dip2px(ctx, TEXTSIZE_H5);
        TEXTSIZE_H6 = BaseUtil.dip2px(ctx, TEXTSIZE_H6);

        TEXTSIZE_I = BaseUtil.dip2px(ctx, TEXTSIZE_I);
        TEXTSIZE_OTHER = BaseUtil.dip2px(ctx, TEXTSIZE_OTHER);
        TEXTSIZE_SUPB = BaseUtil.dip2px(ctx, TEXTSIZE_SUPB);
        TEXTSIZE_UL = BaseUtil.dip2px(ctx, TEXTSIZE_UL);
        TEXTSIZE_LI = BaseUtil.dip2px(ctx, TEXTSIZE_LI);
        SECTIONHEIGHT = BaseUtil.dip2px(ctx, SECTIONHEIGHT);
        LINEHEIGHT_S = BaseUtil.dip2px(ctx, LINEHEIGHT_S);
        LINEHEIGHT = BaseUtil.dip2px(ctx, LINEHEIGHT);
        paddingLeft = BaseUtil.dip2px(ctx, paddingLeft);
        paddingRight = BaseUtil.dip2px(ctx, paddingRight);
        paddingTop = BaseUtil.dip2px(ctx, paddingTop);
        paddingBottom = BaseUtil.dip2px(ctx, paddingBottom);


        curTextSize = TEXTSIZE_OTHER;
        singleTextWidth = getTextWidth("啊");
        curTagEntity = new TagStackEntity();
        curTagEntity.set_tagClass("");
        curTagEntity.set_tagName("p");

        isLoadFootNote = false;
    }

    public void setDisPlayWH(int w, int h) {
        this.displayWidth = w;
        this.displayHeight = h;
    }

    public void setStoreMap(BTreeMap map, LostPagePreferences lostPagePreferences) {
        this.pageStoreMap = map;
        this.lostPagePreferences = lostPagePreferences;
    }

    /**
     * 开始逐字读取Html文件内容，并且分页
     */
    public ArrayList<PageEntity> parseHtmlFile() {
        curPageContents = new ArrayList<Content>();
        lineIndex = 0;
        tagging = false;
        FileReader fr = null;
        // 一个换行算一个字符
        File file = null;
        file = new File(entity.chapter_FullPath);
//        try {
//            file = decodeContent(entity.chapter_FullPath, entity.chapter_FullPath + "_decode", AES_KEY);
//        } catch (OutOfMemoryError oom) {
//            System.gc();
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            try {
//                file = decodeContent(entity.chapter_FullPath, entity.chapter_FullPath + "_decode", AES_KEY);
//            } catch (OutOfMemoryError oom1) {
//            }
//        }
        if (file != null) {
            try {
                fr = new FileReader(file);
                fr.skip(startParsingByteNumber);
                if (parseListener != null) {
                    parseListener.onParsePageStart(pageIndex);
                }
                int ch = 0;
                char[] chArray = new char[1];
                while ((ch = fr.read(chArray)) != -1) {
                    char curChar = chArray[0];
                    if (curChar != '\n' && curChar != '\r') {
                        if (curChar == '>' && tagging) {
                            boolean isTagResult = isTag(tagName + curChar);
                            if (isTagResult) {
                                if ((!tagName.startsWith("</") && !tagName.endsWith("/")) || tagName.equals("<br/")) {
                                    //处理普通开头标签
                                    if (!isLoadFootNote || tagName.startsWith("<li id") || tagName.equals("<br/")) {
                                        setTextStyle(tagName + curChar, false, true, false);
                                    }
                                } else {
                                    if (!isLoadFootNote) {
                                        if (tagName.startsWith("<img")) {
                                            dealImageTag(tagName);
                                        }
                                    }
                                    //退出标签操作
                                    if (!isLoadFootNote || tagName.startsWith("</li") || tagName.startsWith("</ol")) {
                                        popLastTag(tagName + curChar);
                                    }

                                }
                                if (!isLoadFootNote) {
                                    if (curContent != null && !TextUtils.isEmpty(curContent.getContent())) {
                                        x = x + singleTextWidth;//退出标签后无法后退一格
                                    }
                                }
                                endCurContent();
                            }
                            tagName = "";
                            tagging = false;
                            if (!isLoadFootNote) {
                                continue;
                            }

                        }
                        if (curChar == '<' || tagging == true) {
                            tagName = tagName + curChar;
                            if (!isLoadFootNote) {
                                if (tagging && isTextBody) {
                                    boolean likeTag = likeTag(tagName);
                                    if (!likeTag) {
                                        int size = tagName.length();
                                        char[] tagNameChars = tagName.toCharArray();
                                        for (int i = 0; i < size; i++) {
                                            calculateLocation(tagNameChars[i]);
                                            appendText2CurContent(String.valueOf(tagNameChars[i]));
                                        }
                                        tagging = false;
                                        tagName = "";
                                        continue;
                                    }
                                }
                                tagging = true;
                                continue;
                            } else {
                                tagging = true;
                            }
                        }

                        if (isLoadFootNote) {
                            appendText2CurFootNote(String.valueOf(curChar));
                            continue;
                        }
                        //正常处理文本 如果不属于body内容中的可以不做处理
                        if (isTextBody) {
                            calculateLocation(curChar);
                            appendText2CurContent(String.valueOf(curChar));
                        }
                    }
                }
                x = paddingLeft;
                y = paddingTop;
                endLine();
                curHtmlSinglePage = false;
                endPage(true);
                for (int i = 0; i < footNoteEntities.size(); i++) {
                    FootNoteEntity item = footNoteEntities.get(i);
                    pageStoreMap.put(item.getId(), item.getContent());
                }
                return pageDBKeys;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fr != null) {
                    try {
                        fr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (file != null && file.exists()) {
                    FileUtil.deleteSDCardFolder(file);
                }
            }
        }
        return null;

    }

    public int getPageIndex() {
        return this.pageIndex;
    }

    /**
     * 将属于当前文本对象的文字添加进去，因为是逐字读取，所以是一个字一个字添加进去的
     */
    private void appendText2CurContent(String curString) {
        if (curContent == null) {
            curContent = new Content();
            curContent.setContent(curString);
            curContent.setX(x);
            curContent.setY(y);
            curContent.setTagName(curTagEntity.get_tagName());
            curContent.setTagClass(curTagEntity.get_tagClass());
            curPageContents.add(curContent);
        } else {
            curContent.appendText(curString);
        }
    }

    /**
     * 讲属于当前注解对象的文字添加进入
     */
    private void appendText2CurFootNote(String curString) {
        if (isLoadFootNote) {
            if (curFootNoteEntity == null) {
                if (!curString.equals(">")) {
                    curFootNoteEntity = new FootNoteEntity();
                    curFootNoteEntity.setContent(curString);
                    footNoteEntities.add(curFootNoteEntity);
                }
            } else {

                if (!TextUtils.isEmpty(curTagEntity.get_tagClass())) {
                    curFootNoteEntity.setId(curTagEntity.get_tagClass());
                }
                curFootNoteEntity.appentContent(curString);
            }
        }
    }

    /**
     * 结束当前的文本对象
     */
    private void endCurContent() {
        if (curContent != null) {
            curContent = null;
        }
    }

    /**
     * 遇到一个标签设置这个标签的画笔属性
     */
    private String setTextStyle(String tagName, boolean hasChangeLine, boolean addStack, boolean isPopReset) {
        String resultTag = "";
        String resultClass = "";
        String resultOther = "";

        tagName = tagName.trim();
        tagName = tagName.replace("<", "").replace(">", "");
        String originalTagName = tagName;
        if (tagName.contains(" ")) {
            String[] tagAttrs = tagName.split(" ");
            tagName = tagAttrs[0];
            if (addStack) {

                for (int i = 0; i < tagAttrs.length; i++) {
                    if (!isLoadFootNote) {
                        if (tagAttrs[i].startsWith("class=")) {
                            resultClass = tagAttrs[i].split("=\"")[1].replace("\"", "");
                            if (resultClass.equals("singlepage")) {
                                curHtmlSinglePage = true;
                            }
                        } else if (originalTagName.startsWith("a class=\"xsl-footnote-link\"") && tagAttrs[i].startsWith("href=")) {
                            resultOther = tagAttrs[i].split("=\"")[1].replace("\"", "");
                        } else if (originalTagName.startsWith("a href=") && tagAttrs[i].startsWith("href=")){
                            resultClass = tagAttrs[i].split("=\"")[1].replace("\"", "");
                        }
                    } else {
                        if (tagAttrs[i].startsWith("id=")) {
                            resultClass = tagAttrs[i].split("=\"")[1].replace("\"", "");
                        }
                    }
                }
            }
        }

        if (TextUtils.equals(tagName, "body")) {
            isTextBody = true;
            resultTag = "body";
            x = paddingLeft;
            y = paddingTop;
        } else if (TextUtils.equals(tagName, "p")) {
            resultTag = "p";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "h1")) {
            resultTag = "h1";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_H1);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
            dealIndentation(hasChangeLine);
        } else if (TextUtils.equals(tagName, "h2")) {
            resultTag = "h2";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_H2);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "h3")) {
            resultTag = "h3";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_H3);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "h4")) {
            resultTag = "h4";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_H4);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "h5")) {
            resultTag = "h5";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_H5);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "h6")) {
            resultTag = "h6";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_H6);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "ul")) {
            resultTag = "ul";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_UL);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "ol")) {
            resultTag = "ol";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_UL);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "li")) {
            resultTag = "li";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_LI);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "sub")) {
            resultTag = "sub";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_SUPB);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "sup")) {
            resultTag = "sup";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_SUPB);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "i")) {
            resultTag = "i";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_I);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "b")) {
            resultTag = "b";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "div")) {
            resultTag = "div";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "span")) {
            resultTag = "span";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "hr")) {
            resultTag = "hr";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "a")) {
            resultTag = "a";
            resultClass = resultClass + resultOther;
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "br/")) {
            resultTag = "br/";
            setCurTag(resultTag, "");
            setPaintStyle(TEXTSIZE_OTHER);
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "table")) {
            resultTag = "table";
            setCurTag(resultTag, "");
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
        } else if (TextUtils.equals(tagName, "td")) {
            resultTag = "td";
            setCurTag(resultTag, "");
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "tr")) {
            resultTag = "tr";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        } else if (TextUtils.equals(tagName, "blockquote")){
            resultTag = "blockquote";
            setCurTag(resultTag, resultClass);
            setPaintStyle(TEXTSIZE_OTHER);
            if (addStack) {
                addTag2Stack();
            }
            if (!isPopReset) {
                dealIndentation(hasChangeLine);
            }
        }else {
            setPaintStyle(TEXTSIZE_OTHER);
            resultTag = "p";
        }

        return resultTag;
    }

    /**
     * 将当前的标签属性压入栈
     */
    private void addTag2Stack() {
        tagStack.add(curTagEntity);
    }

    /**
     * 遇到</xx>的时候弹出标签页
     */
    private TagStackEntity popLastTag(String tagName) {
        tagName = tagName.replace("<", "").replace(">", "");
        TagStackEntity resultTag = null;
        if (tagName.startsWith("/") && !tagStack.isEmpty()) {
            if (TextUtils.equals(tagName.substring(1, tagName.length()), tagStack.lastElement().get_tagName())) {
                if (tagName.equals("/ol") && isLoadFootNote) {
                    isLoadFootNote = false;
                    if (!footNoteEntities.isEmpty() && footNoteEntities.get(footNoteEntities.size() - 1).getContent().startsWith("</ol")) {
                        footNoteEntities.remove(footNoteEntities.size() - 1);
                    }
                }
                TagStackEntity popTagEntity = tagStack.pop();
                if (popTagEntity.get_tagName().equals("h1") || popTagEntity.get_tagName().equals("h2") || popTagEntity.get_tagName().equals("h3")) {
                    if (parseListener != null) {
                        BookDirEntity dirEntity = new BookDirEntity();
                        dirEntity.setTagName(popTagEntity.get_tagName());
                        dirEntity.setDirName(getDirName(popTagEntity.get_tagName()));
                        dirEntity.setPageIndex(pageIndex);
                        dirEntities.add(dirEntity);
                    }
                }
                if (popTagEntity.get_tagName().equals("li") && isLoadFootNote) {
                    endCurFootNote();
                }
                if (!tagStack.isEmpty()) {
                    resultTag = tagStack.lastElement();
                    setTextStyle(resultTag.get_tagName(), true, false, true);
                } else {
                    setPaintStyle(TEXTSIZE_OTHER);
                    curTagEntity.set_tagName("p");
                    curTagEntity.set_tagClass("");
                    resultTag = curTagEntity;
                }
            } else {
                setPaintStyle(TEXTSIZE_OTHER);
                curTagEntity.set_tagName("p");
                curTagEntity.set_tagClass("");
            }
        }
        return resultTag;
    }

    private String getDirName(String popTagName) {
        String dirNameString = "";
        try {
            if (curPageContents != null && !curPageContents.isEmpty()) {
                boolean curPageBreak = false;
                int count = curPageContents.size();
                Content lastContent = null;
                for (int i = (count - 1); i >= 0; i--) {
                    if (lastContent == null) {
                        dirNameString = curPageContents.get(i).getContent();
                        lastContent = curPageContents.get(i);
                    } else {
                        if (lastContent.getTagName().equals(curPageContents.get(i).getTagName()) || curPageContents.get(i).getTagName().equals("sup") || curPageContents.get(i).getTagName().equals("sub") || curPageContents.get(i).getTagName().equals(popTagName)) {
                            dirNameString = curPageContents.get(i).getContent() + dirNameString;
                            lastContent = curPageContents.get(i);
                        } else {
                            curPageBreak = true;
                            break;
                        }
                    }
                }
                if (!pages.isEmpty()&&!curPageBreak){
                    List<Content> lastContents = pages.get(pages.size()-1).getContents();
                    int lastCount = lastContents.size();
                    for (int i = (lastCount - 1); i >= 0; i--) {
                        if (lastContent != null) {
                            if (lastContent.getTagName().equals(lastContents.get(i).getTagName()) || lastContents.get(i).getTagName().equals("sup") || lastContents.get(i).getTagName().equals("sub") || lastContents.get(i).getTagName().equals(popTagName)) {
                                dirNameString = lastContents.get(i).getContent() + dirNameString;
                                lastContent = lastContents.get(i);
                            } else {
                                break;
                            }
                        }else {
                            break;
                        }
                    }
                }

            }
        } catch (Exception e) {
        }
        return dirNameString;
    }

    /**
     * 判断这个标签是否是我们支持的标签
     */
    private boolean isTag(String tagName) {
        boolean result = false;
        tagName = tagName.replace("<", "").replace(">", "");
        if (tagName.startsWith("/")) {
            tagName = tagName.substring(1, tagName.length());
        }
        if (tagName.contains(" ")) {
            String[] tagNamePatterns = tagName.split(" ");
            tagName = tagNamePatterns[0];
        }
        if (TextUtils.equals(tagName, "body")) {
            result = true;
        } else if (TextUtils.equals(tagName, "p")) {
            result = true;
        } else if (TextUtils.equals(tagName, "h1")) {
            result = true;
        } else if (TextUtils.equals(tagName, "h2")) {
            result = true;
        } else if (TextUtils.equals(tagName, "h3")) {
            result = true;
        } else if (TextUtils.equals(tagName, "h4")) {
            result = true;
        } else if (TextUtils.equals(tagName, "h5")) {
            result = true;
        } else if (TextUtils.equals(tagName, "h6")) {
            result = true;
        } else if (TextUtils.equals(tagName, "ul")) {
            result = true;
        } else if (TextUtils.equals(tagName, "ol")) {
            result = true;
        } else if (TextUtils.equals(tagName, "li")) {
            result = true;
        } else if (TextUtils.equals(tagName, "sub")) {
            result = true;
        } else if (TextUtils.equals(tagName, "sup")) {
            result = true;
        } else if (TextUtils.equals(tagName, "i")) {
            result = true;
        } else if (TextUtils.equals(tagName, "b")) {
            result = true;
        } else if (TextUtils.equals(tagName, "img")) {
            result = true;
        } else if (TextUtils.equals(tagName, "br/")) {
            result = true;
        } else if (TextUtils.equals(tagName, "html")) {
            result = true;
        } else if (TextUtils.equals(tagName, "div")) {
            result = true;
        } else if (TextUtils.equals(tagName, "span")) {
            result = true;
        } else if (TextUtils.equals(tagName, "hr")) {
            result = true;
        } else if (TextUtils.equals(tagName, "a")) {
            result = true;
        } else if (TextUtils.equals(tagName, "table")) {
            result = true;
        } else if (TextUtils.equals(tagName, "tr")) {
            result = true;
        } else if (TextUtils.equals(tagName, "td")) {
            result = true;
        } else if (TextUtils.equals(tagName, "blockquote")){
            result = true;
        }
        return result;
    }

    /**
     * 计算当前文字的位置，需要换行或者换页等等
     */
    private void calculateLocation(char curChar) {
        curTextWidth = getTextWidth(String.valueOf(curChar));

        if ((x + curTextWidth) < displayWidth - paddingRight) {//只能小于 不能等于，会出现多一个字的情况
            //本行还有余地 只需要在这一行后面显示就够了
            //这是方式刚处理标签缩进/换行后进来的是标题文字，又进行一次位移，导致空三格或者空一格的情况出现
            if (curContent != null && !TextUtils.isEmpty(curContent.getContent())) {
                x += curTextWidth;
            }
        } else {
            //本行没有余地需要
            if ((y + LINEHEIGHT) > displayHeight - paddingBottom) {
                //完结这一页
                endLine();
                endPage(false);
                x = paddingLeft;
                y = paddingTop;
            } else {
                //换行
                endLine();
                y += LINEHEIGHT;
                x = paddingLeft;
            }
        }
    }

    /**
     * 这个是判断当前标签疑似我们支持的标签
     */
    private boolean likeTag(String tagNameLike) {
        boolean result = false;
        tagNameLike = tagNameLike.replace("<", "").replace(">", "");
        if (tagNameLike.startsWith("/")) {
            tagNameLike = tagNameLike.substring(1, tagNameLike.length());
        }
        if ("h1".contains(tagNameLike) || "h2".contains(tagNameLike) || "h3".contains(tagNameLike) || "h4".contains(tagNameLike) || "h5".contains(tagNameLike) || "h6".contains(tagNameLike) || "img src".contains(tagNameLike) || "li".contains(tagNameLike) || "ul".contains(tagNameLike) || "ol".contains(tagNameLike) || "sup".contains(tagNameLike) || "sub".contains(tagNameLike) || "br/".contains(tagNameLike) || "body".contains(tagNameLike) || "html".contains(tagNameLike) || "div".contains(tagNameLike) || "span".contains(tagNameLike) || "hr".contains(tagNameLike) || "table".contains(tagNameLike) || "tr".contains(tagNameLike) || "td".contains(tagNameLike)||"blockquote".contains(tagNameLike)) {
            result = true;
        } else if (tagNameLike.startsWith("img") || tagNameLike.startsWith("a") || tagNameLike.startsWith("p") || tagNameLike.startsWith("div") || tagNameLike.startsWith("span") || tagNameLike.startsWith("h1") || tagNameLike.startsWith("h2") || tagNameLike.startsWith("h3") || tagNameLike.startsWith("h4") || tagNameLike.startsWith("h5") || tagNameLike.startsWith("h6") || tagNameLike.startsWith("ol") || tagNameLike.startsWith("li") || tagNameLike.startsWith("hr") || tagNameLike.startsWith("ul") || tagNameLike.contains("table") || tagNameLike.contains("tr") || tagNameLike.contains("td")||tagNameLike.contains("blockquote")) {
            result = true;
        }

        return result;
    }

    /**
     * 对这个标签进行对应的缩进或者换行等操作
     */
    private void dealIndentation(boolean hasChangeLine) {
        TagStackEntity tagStackEntity = curTagEntity;

        if (TextUtils.equals(tagStackEntity.get_tagName(), "ol")&&tagStackEntity.get_tagClass().equals(QQ_FOOTNOTECLASS)) {
            isLoadFootNote = true;
        }else{
            int _lineHeight = 0;
            if (!TextUtils.isEmpty(tagStackEntity.get_tagClass()) && TextUtils.equals(tagStackEntity.get_tagClass(), "qt")) {
                _lineHeight = LINEHEIGHT_S;
            } else {
                _lineHeight = LINEHEIGHT;
            }


            if ((y + _lineHeight) > displayHeight - paddingBottom) {
                endLine();
                endPage(false);
                //集中处理换页到新页中 缩进问题
                if (tagStackEntity.get_tagName().equals("p") || tagStackEntity.get_tagName().equals("ul") || tagStackEntity.get_tagName().equals("ol") || tagStackEntity.get_tagName().equals("li") || tagStackEntity.get_tagName().equals("tr") || tagStackEntity.get_tagName().equals("td")) {
                    x = paddingLeft;//+ singleTextWidth + singleTextWidth;
                    y = paddingTop;
                } else {
                    x = paddingLeft;
                    y = paddingTop;
                }
            } else {
                if (TextUtils.equals(tagStackEntity.get_tagName(), "p")) {
                    endLine();
                    x = paddingLeft;
                    y += SECTIONHEIGHT;

                } else if (TextUtils.equals(tagStackEntity.get_tagName(), "h1") || TextUtils.equals(tagStackEntity.get_tagName(), "h2") || TextUtils.equals(tagStackEntity.get_tagName(), "h3") || TextUtils.equals(tagStackEntity.get_tagName(), "h4") || TextUtils.equals(tagStackEntity.get_tagName(), "h5") || TextUtils.equals(tagStackEntity.get_tagName(), "h6")) {
                    endLine();
                    //这儿是刚好一页开始就是标题不需要换行
                    if (!curPageContents.isEmpty()) {
                        y += SECTIONHEIGHT;
                    } else if (curContent != null && !TextUtils.isEmpty(curContent.getContent())) {
                        y += SECTIONHEIGHT;
                    }
                    x = paddingLeft;
                } else if (TextUtils.equals(tagStackEntity.get_tagName(), "ul")) {
                    endLine();
                    if (y != paddingTop) {
                        y += _lineHeight;
                    } else if (!curPageContents.isEmpty()) {
                        y += _lineHeight;
                    }
                    x = paddingLeft;//+ singleTextWidth + singleTextWidth;
                } else if (TextUtils.equals(tagStackEntity.get_tagName(), "ol")) {
                    endLine();
                    if (y != paddingTop) {
                        y += _lineHeight;
                    } else if (!curPageContents.isEmpty()) {
                        y += _lineHeight;
                    }
                    x = paddingLeft;//+ singleTextWidth + singleTextWidth;
//                }
                } else if (TextUtils.equals(tagStackEntity.get_tagName(), "li")) {
                    if (isLoadFootNote) {
                    } else {
                        endLine();
                        if (y != paddingTop) {
                            y += _lineHeight;
                        } else if (!curPageContents.isEmpty()) {
                            y += _lineHeight;
                        }
                        x = paddingLeft;//+ singleTextWidth + singleTextWidth;
                        appendLiPoint();
                    }

                } else if (TextUtils.equals(tagStackEntity.get_tagName(), "br/")) {
                    endLine();
                    if (!hasChangeLine && y != LINEHEIGHT) {
                        y += _lineHeight;
                    }
                    x = paddingLeft;
                } else if (TextUtils.equals(tagStackEntity.get_tagName(), "tr") || TextUtils.equals(tagStackEntity.get_tagName(), "td")) {
                    endLine();
                    if (y != paddingTop) {
                        y += _lineHeight;
                    } else if (!curPageContents.isEmpty()) {
                        y += _lineHeight;
                    }
                    x = paddingLeft;//+ singleTextWidth + singleTextWidth;
                } else if(TextUtils.equals(tagStackEntity.get_tagName(), "blockquote")){
                    x = paddingLeft+ singleTextWidth + singleTextWidth;
                }else {
                    x += singleTextWidth;
                }
            }

        }


    }

    /**
     * 处理图片
     */
    private void dealImageTag(String tagName) throws Exception {
        //这里是处理图片内容因为图片内容是以<p class="center"><img src="images/Figure-0000-96.jpg" alt=""/></p>出现
        String curImagePath = getImagePath(tagName);
        if (!TextUtils.isEmpty(curImagePath)) {
            double[] data = loadDisplayImageContent(curImagePath);
            if (data[0] != 0 && data[1] != 0) {
                String tagClass = getImageTagClass(tagName);
                boolean isinline = TextUtils.equals(tagClass, "in-line");
                Content imageContent = new Content();
                if (isinline) {
                    if ((x + data[0]) < displayWidth - paddingRight) {//只能小于 不能等于，会出现多一个字的情况
                        //本行还有余地 只需要在这一行后面显示就够了
                        //这是方式刚处理标签缩进/换行后进来的是标题文字，又进行一次位移，导致空三格或者空一格的情况出现
                        x += data[0];
                    } else {
                        //本行没有余地需要
                        if ((y + LINEHEIGHT) > displayHeight - paddingBottom) {
                            //完结这一页
                            endLine();
                            endPage(false);
                            x = paddingLeft;
                            y = paddingTop;
                        } else {
                            //换行
                            endLine();
                            y += LINEHEIGHT;
                            x = paddingLeft;
                        }
                    }
                    imageContent.setX(x);
                    imageContent.setY(y);
                    imageContent.setTagName("img");
                    imageContent.setTagClass(tagClass);
                    imageContent.setContent(curImagePath);
                    curPageContents.add(imageContent);
                    x += data[0];
                } else {
                    if (y + data[1] > displayHeight - paddingBottom) {
                        endLine();
                        endPage(false);
                        x = paddingLeft;
                        y = paddingTop;
                    } else {
                        //这儿读取到宽高之后会比较当前页面剩余的宽高，然后决定是否换页显示，如果不换页，就需要换行
                        endLine();
                        x = paddingLeft;
                    }
                    imageContent.setX(x);
                    imageContent.setY(y);
                    imageContent.setTagName("img");
                    imageContent.setTagClass(tagClass);
                    imageContent.setContent(curImagePath);
                    curPageContents.add(imageContent);
                    y = (float) (y + data[1]);
                    endLine();
                }


            }
        }

    }

    /**
     * 设置画笔属性
     */
    private void setPaintStyle(int curTextSize) {
        if (!TextUtils.isEmpty(curTagEntity.get_tagName()) && ((TextUtils.equals(curTagEntity.get_tagClass(), "tp") || TextUtils.equals(curTagEntity.get_tagClass(), "qt")))) {
            if (TextUtils.equals(curTagEntity.get_tagClass(), "tp")) {
                this.curTextSize = (curTextSize - 13);
            } else {
                this.curTextSize = (curTextSize - 6);
            }
        } else {
            this.curTextSize = curTextSize;
        }
    }

    /**
     * 更新当前标签，遇到新标签时
     */
    private void setCurTag(String name, String classname) {
        curTagEntity = new TagStackEntity();
        curTagEntity.set_tagName(name);
        curTagEntity.set_tagClass(classname);
    }

    /**
     * 换行
     */
    private void endLine() {
        if (curContent != null) {
            curContent = null;
        }
    }

    /**
     * 结束当前注解
     */
    private void endCurFootNote() {
        if (curFootNoteEntity != null && isLoadFootNote) {
            curFootNoteEntity = null;
        }
    }

    /**
     * 对普通的li标签加一个点标注
     */
    private void appendLiPoint() {
        appendText2CurContent("•");
    }

    /**
     * 结束当前页，换到下一页
     */
    private void endPage(boolean fileEnd) {
        if (!curHtmlSinglePage) {
            if (curPageContents != null && !curPageContents.isEmpty()) {
                ContentPage page = new ContentPage();
                page.setPageTitle(entity.chapter_Title);
                page.setPageNumber(pageIndex);
                page.setContents(curPageContents);
                pages.add(page);
                try {
                    pageStoreMap.put(page.getDBKey(), page.toJsonString());
                } catch (java.lang.IllegalAccessError error) {
                    lostPagePreferences.saveLostPage(this.rootPath + page.getDBKey(), page.toJsonString());
                }
                PageEntity pageEntity = new PageEntity();
                pageEntity.setChapterDBkey(entity.chapter_FullPath);
                pageEntity.setPageDBkey(page.getDBKey());
                pageDBKeys.add(pageEntity);
                pageIndex++;
                this.curPageContents = new ArrayList<Content>();
                if (parseListener != null) {
                    parseListener.onParsePageEnd(pageEntity);
                    if (!fileEnd) {
                        parseListener.onParsePageStart(pageIndex + 1);
                    }
                }
            }
        }


    }

    /**
     * 读取图片属性，进行缩放
     */
    private double[] loadDisplayImageContent(String imagePath) {
        File imageFile = new File(imagePath);
        double[] data = {0, 0};
        if (imageFile.exists() && !imageFile.isDirectory()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);
            double originalW = options.outWidth;
            double originalH = options.outHeight;
            double w = 0, h = 0;
            double f = originalW / originalH;
            if (f > 1) {
                if (originalW < (getDisplayW() / 1.8)) {
                    originalW = originalW * 1.2;
                    originalH = originalH * 1.2;
                }
                if (originalW > getDisplayW()) {
                    w = getDisplayW();
                    h = w / f;
                    if (h > getDisplayH()) {
                        h = getDisplayH();
                        w = h * f;
                    }
                } else {
                    w = originalW;
                    h = originalH;
                }
            } else {
                if (originalW < (getDisplayW() / 1.8)) {
                    originalW = originalW * 1.2;
                    originalH = originalH * 1.2;
                }
                if (originalH > getDisplayH()) {
                    h = getDisplayH();
                    w = h * f;
                    if (w > getDisplayW()) {
                        w = getDisplayW();
                        h = w / f;
                    }
                } else {
                    w = originalW;
                    h = originalH;
                    if (w > getDisplayW()) {
                        w = getDisplayW();
                        h = w / f;
                    }
                }
            }
            data[0] = w;
            data[1] = h;
        }
        return data;
    }

    private float getDisplayH() {
        return displayHeight - paddingBottom - paddingTop;
    }

    private float getDisplayW() {
        return displayWidth;
    }


    /**
     * 解析图片标签，得到图片路径
     */
    private String getImagePath(String imageTag) throws Exception {
        String imagePath = "";
        String[] patterns = imageTag.split(" ");
        for (String item : patterns) {
            if (item.startsWith("src=")) {
                String[] srcPatterns = item.replace("\"", "").split("=");
                imagePath = srcPatterns[1];
                break;
            }
        }
        if (!TextUtils.isEmpty(imagePath)) {
            imagePath = rootPath + File.separator + "OPS" + File.separator + imagePath;
        }
        return imagePath;
    }

    /**
     * 解析图片标签，读取到图片的class属性
     */
    private String getImageTagClass(String imageTag) throws Exception {
        String tagClass = "";
        String[] patterns = imageTag.split(" ");
        for (String item : patterns) {
            if (item.startsWith("class=")) {
                tagClass = item.split("=\"")[1].replace("\"", "");
                break;
            }
        }
        return tagClass;
    }

    /**
     * 获取文本在当前画笔风格下宽度
     */
    private float getTextWidth(String text) {
        paint.setTextSize(curTextSize);
        return paint.measureText(text);
    }

    private float getTextHeight(String text) {
        paint.setTextSize(curTextSize);
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) Math.ceil(fm.descent - fm.top) + 2;
    }

    /**
     * 返回当前html中h1,h2,h3等级的标题
     */
    public ArrayList<BookDirEntity> getDirEntities() {
        return dirEntities;
    }

    /**
     * 返回当前html中所用到的所有注解
     */
    public ArrayList<FootNoteEntity> getFootNoteEntities() {
        Log.d("qq", "getFootNoteEntities.size" + footNoteEntities.size());
        for (int i = 0; i < footNoteEntities.size(); i++) {
            FootNoteEntity item = footNoteEntities.get(i);
            Log.d("qq", "getFootNoteEntities:" + item.toString());
        }
        return footNoteEntities;
    }

    public interface ParsePageListener {
        public void onParsePageEnd(PageEntity entity);

        public void onParsePageStart(int pageIndex);

    }

//    public File decodeContent(String filePath, String decodeFilePath, String password) throws OutOfMemoryError {
//        File decodeFile = null;
//        try {
//            byte[] bytes = FileHelper.getInstance().readTobyte(filePath);
//            bytes = AES_ForAndroid.decrypt(bytes, Md5Encrypt.md5(password).toLowerCase());
//            decodeFile = byte2File(bytes, decodeFilePath);
//        } catch (Exception e) {
//            e.getStackTrace();
//        }
//        return decodeFile;
//    }

//    public File byte2File(byte[] buf, String filePath) {
//        BufferedOutputStream bos = null;
//        FileOutputStream fos = null;
//        File file = null;
//        try {
//            file = new File(filePath);
//            fos = new FileOutputStream(file);
//            bos = new BufferedOutputStream(fos);
//            bos.write(buf);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (bos != null) {
//                try {
//                    bos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (fos != null) {
//                try {
//                    fos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return file;
//    }


    private static class TagStackEntity {
        String _tagName;
        String _tagClass;

        public String get_tagName() {
            return _tagName;
        }

        public void set_tagName(String _tagName) {
            this._tagName = _tagName;
        }

        public String get_tagClass() {
            return _tagClass;
        }

        public void set_tagClass(String _tagClass) {
            this._tagClass = _tagClass;
        }

        @Override
        public String toString() {
            return "TagStackEntity{" +
                    "_tagName='" + _tagName + '\'' +
                    ", _tagClass='" + _tagClass + '\'' +
                    '}';
        }
    }

}
