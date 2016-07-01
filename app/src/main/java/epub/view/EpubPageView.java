package epub.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import epub.entity.Content;
import epub.entity.ContentPage;
import epub.tool.BaseUtil;

/**
 * Created by liuqing on 15/1/9.
 */
public class EpubPageView extends View implements GestureDetector.OnGestureListener {
    private ContentPage curPage;
    private Paint paint,selectPaint;
    private static int textSize_default = 30;
    private static final int textColor_whiteStyle = Color.BLACK;
    private static final int textColor_gray_whiteStyle = Color.GRAY;

    private static final int textColor_nightStyle = Color.parseColor("#7b8094");
    private static final int textColor_gray_nightStyle = Color.GRAY;

    private static final int tagColor_whiteStyle = Color.parseColor("#F7971C");
    private static final int tagColor_nightStyle = Color.parseColor("#956141");

    private int display_TextColor = textColor_whiteStyle;
    private int display_gray_TextColor = textColor_gray_whiteStyle;

    private boolean isBold = false;

    private List<BitmapRectEntity> bitmapRects = new ArrayList<BitmapRectEntity>();
    private List<ReMarkRectEntity> reMarkRectEntities = new ArrayList<ReMarkRectEntity>();
    private List<LinkRectEntity> linkRectEntities = new ArrayList<LinkRectEntity>();
    private List<LineRectEntity> lineRectEntities = new ArrayList<LineRectEntity>();
    private List<LineRectEntity> selectLines = new ArrayList<LineRectEntity>();
    //默认的文字大小 dp属性 在构造中有转为px的操作
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
    private int TEXTSIZE_FOOTNOTELINK = 12;
    private int FootNoteLinkR = 9;
    private int FootNoteLinkOffset = 2;

    private int LINEHEIGHT = 30;//单行高度
    private onContentClickListener listener = null;
    private GestureDetector gestureDetector = null;

    public int paddingLeft = 3;//水品方向间隔距离
    public int paddingRight = 17;
    private int paddingTop = 33;
    private int paddingBottom = 33;

    private boolean nightStyle = false;

    private static int lineHeight = 0;
    private float initSelectX,initSelectY;




    public EpubPageView(Context context) {
        super(context);
        init();
    }

    public EpubPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EpubPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void init() {
        TEXTSIZE_H1 = BaseUtil.dip2px(getContext(), TEXTSIZE_H1);
        TEXTSIZE_H2 = BaseUtil.dip2px(getContext(), TEXTSIZE_H2);
        TEXTSIZE_H3 = BaseUtil.dip2px(getContext(), TEXTSIZE_H3);
        TEXTSIZE_H4 = BaseUtil.dip2px(getContext(), TEXTSIZE_H4);
        TEXTSIZE_H5 = BaseUtil.dip2px(getContext(), TEXTSIZE_H5);
        TEXTSIZE_H6 = BaseUtil.dip2px(getContext(), TEXTSIZE_H6);

        TEXTSIZE_I = BaseUtil.dip2px(getContext(), TEXTSIZE_I);
        TEXTSIZE_OTHER = BaseUtil.dip2px(getContext(), TEXTSIZE_OTHER);
        TEXTSIZE_SUPB = BaseUtil.dip2px(getContext(), TEXTSIZE_SUPB);
        TEXTSIZE_UL = BaseUtil.dip2px(getContext(), TEXTSIZE_UL);
        TEXTSIZE_LI = BaseUtil.dip2px(getContext(), TEXTSIZE_LI);
        TEXTSIZE_FOOTNOTELINK = BaseUtil.dip2px(getContext(), TEXTSIZE_FOOTNOTELINK);
        LINEHEIGHT = BaseUtil.dip2px(getContext(), LINEHEIGHT);

        FootNoteLinkOffset = BaseUtil.dip2px(getContext(), FootNoteLinkOffset);
        FootNoteLinkR = BaseUtil.dip2px(getContext(), FootNoteLinkR);
        paddingLeft = BaseUtil.dip2px(getContext(), paddingLeft);
        paddingRight = BaseUtil.dip2px(getContext(), paddingRight);
        paddingTop = BaseUtil.dip2px(getContext(), paddingTop);
        paddingBottom = BaseUtil.dip2px(getContext(), paddingBottom);
        textSize_default = TEXTSIZE_OTHER;
        paint = new Paint();
        paint.setColor(display_TextColor);
        paint.setTextSize(textSize_default);
        paint.setFakeBoldText(isBold);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setAntiAlias(true);
        selectPaint = new Paint();
        selectPaint.setColor(Color.parseColor("#880000FF"));
        selectPaint.setAlpha(120);

        lineHeight = 40;//getFontHeight(paint);
        gestureDetector = new GestureDetector(this);
        try {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } catch (java.lang.NoSuchMethodError e) {
        }
    }

    public void setNightStyle(boolean isNight) {
        nightStyle = isNight;
        if (isNight) {
            display_TextColor = textColor_nightStyle;
            display_gray_TextColor = textColor_gray_nightStyle;
        } else {
            display_TextColor = textColor_whiteStyle;
            display_gray_TextColor = textColor_gray_whiteStyle;
        }
    }


    private void setPaintStyle(Content content) {
        if (!TextUtils.equals(content.getTagName(), "img")) {
            paint.setFakeBoldText(false);
            paint.setTextAlign(Paint.Align.LEFT);
            if (TextUtils.equals(content.getTagName(), "p")) {
                paint.setTextSize(TEXTSIZE_OTHER);
                paint.setColor(display_TextColor);
                if (!TextUtils.isEmpty(content.getTagClass()) && (content.getTagClass().equals("tp") || (content.getTagClass().equals("qt")))) {
                    if (content.getTagClass().equals("tp")) {
                        paint.setTextSize(TEXTSIZE_OTHER - 13);
                        paint.setColor(display_gray_TextColor);
                    } else {
                        paint.setTextSize(TEXTSIZE_OTHER - 6);
                        paint.setColor(display_gray_TextColor);
                    }
                }
            } else if (TextUtils.equals(content.getTagName(), "h1")) {
                paint.setTextSize(TEXTSIZE_H1);
                paint.setFakeBoldText(true);
                paint.setColor(display_TextColor);
                if (!TextUtils.isEmpty(content.getTagClass())) {
                    paint.setTextSize(TEXTSIZE_H1 - 20);
                }
            } else if (TextUtils.equals(content.getTagName(), "h2")) {
                paint.setTextSize(TEXTSIZE_H2);
                paint.setFakeBoldText(true);
                paint.setColor(display_TextColor);
                if (!TextUtils.isEmpty(content.getTagClass())) {
                    paint.setTextSize(TEXTSIZE_H2 - 20);
                }
            } else if (TextUtils.equals(content.getTagName(), "h3")) {
                paint.setTextSize(TEXTSIZE_H3);
                paint.setFakeBoldText(true);
                paint.setColor(display_TextColor);
                if (!TextUtils.isEmpty(content.getTagClass())) {
                    paint.setTextSize(TEXTSIZE_H3 - 20);
                }
            } else if (TextUtils.equals(content.getTagName(), "h4")) {
                paint.setTextSize(TEXTSIZE_H4);
                paint.setFakeBoldText(true);
                paint.setColor(display_TextColor);
                if (!TextUtils.isEmpty(content.getTagClass())) {
                    paint.setTextSize(TEXTSIZE_H4 - 20);
                }
            } else if (TextUtils.equals(content.getTagName(), "h5")) {
                paint.setTextSize(TEXTSIZE_H5);
                paint.setFakeBoldText(true);
                paint.setColor(display_TextColor);
                if (!TextUtils.isEmpty(content.getTagClass())) {
                    paint.setTextSize(TEXTSIZE_H5 - 20);
                }
            } else if (TextUtils.equals(content.getTagName(), "h6")) {
                paint.setTextSize(TEXTSIZE_H6);
                paint.setFakeBoldText(true);
                paint.setColor(display_TextColor);
                if (!TextUtils.isEmpty(content.getTagClass())) {
                    paint.setTextSize(TEXTSIZE_H6 - 20);
                }
            } else if (TextUtils.equals(content.getTagName(), "ul")) {
                paint.setColor(display_TextColor);
                paint.setTextSize(TEXTSIZE_UL);
            } else if (TextUtils.equals(content.getTagName(), "li")) {
                paint.setColor(display_TextColor);
                paint.setTextSize(TEXTSIZE_LI);
            } else if (TextUtils.equals(content.getTagName(), "sub")) {
                paint.setColor(display_TextColor);
                paint.setTextSize(TEXTSIZE_SUPB);
            } else if (TextUtils.equals(content.getTagName(), "sup")) {
                paint.setColor(display_TextColor);
                paint.setTextSize(TEXTSIZE_SUPB);
            } else if (TextUtils.equals(content.getTagName(), "i")) {
                paint.setColor(display_TextColor);
                paint.setTextSize(TEXTSIZE_I);
                if (!TextUtils.isEmpty(content.getTagClass())) {
                    paint.setTextSize(TEXTSIZE_I - 20);
                }
            } else if (TextUtils.equals(content.getTagName(), "b")) {
                paint.setColor(display_TextColor);
                paint.setTextSize(TEXTSIZE_OTHER);
                paint.setFakeBoldText(true);
                if (!TextUtils.isEmpty(content.getTagClass()) && (TextUtils.equals(content.getTagClass(), "tp") || TextUtils.equals(content.getTagClass(), "qt"))) {
                    paint.setTextSize(TEXTSIZE_OTHER - 20);
                    paint.setColor(display_gray_TextColor);
                }
            } else if (TextUtils.equals(content.getTagName(), "a")) {
                if (content.getTagClass().startsWith("xsl-footnote-link")) {
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(TEXTSIZE_FOOTNOTELINK);
                } else {
                    if (content.getTagName().equals("a") && content.getTagClass().startsWith("http")) {
                        paint.setColor(Color.BLUE);
                    }
                }

            } else {
                paint.setColor(display_TextColor);
                paint.setTextSize(TEXTSIZE_OTHER);
            }

        }
    }

    public void setContentPage(ContentPage page) {
        this.curPage = page;
        postInvalidate();
    }

    public void setOnContentClickListener(onContentClickListener listener) {
        this.listener = listener;
    }

    public ContentPage getContentPage() {
        return curPage;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (curPage == null || curPage.getContents() == null || curPage.getContents().isEmpty()) {
            return;
        }
        for (LineRectEntity entity : selectLines) {
            canvas.drawRect(entity.getRect(), selectPaint);
        }
        bitmapRects.clear();
        if (curPage.getContents().size() == 1) {
            Content content = curPage.getContents().get(0);
            if (!TextUtils.equals(content.getTagName(), "img")) {
                Content textContent = content;
                setPaintStyle(textContent);
                if (TextUtils.equals(textContent.getTagName(), "sub")) {
                    canvas.drawText(textContent.getContent(), textContent.getX(), textContent.getY() + 10, paint);
                } else if (TextUtils.equals(textContent.getTagName(), "sup")) {
                    canvas.drawText(textContent.getContent(), textContent.getX(), textContent.getY() - 25, paint);
                } else {
                    canvas.drawText(textContent.getContent(), textContent.getX(), textContent.getY(), paint);
                }
            } else {
                Content imageContent = content;
                setPaintStyle(imageContent);
                try {
                    Bitmap bt = getImageBitmap(imageContent);
                    int startX = 0;
                    int startY = 0;
                    if (getWidth() > bt.getWidth()) {
                        startX = (getWidth() - bt.getWidth()) / 2;
                    }
                    if (bt != null) {
                        Rect rect = new Rect();
                        rect.set(startX, (int) imageContent.getY(), startX + bt.getWidth(), (int) imageContent.getY() + bt.getHeight());
                        BitmapRectEntity entity = new BitmapRectEntity();
                        entity.setRect(rect);
                        entity.setImageContent(imageContent.getContent());
                        entity.setTagClass(imageContent.getTagClass());
                        bitmapRects.add(entity);
                        startY = (int) imageContent.getY();
                        canvas.drawBitmap(bt, startX, startY, null);
                        bt.recycle();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (OutOfMemoryError oom) {
                } catch (Exception ee) {
                }
            }

        } else {
            for (int i = 0; i < curPage.getContents().size(); i++) {
                Content content = curPage.getContents().get(i);
                if (!TextUtils.equals(content.getTagName(), "img")) {
                    Content textContent = content;
                    addContent2Line(textContent);
                    if (TextUtils.equals(textContent.getTagName(), "sub")) {
                        setPaintStyle(textContent);
                        canvas.drawText(textContent.getContent(), textContent.getX(), textContent.getY() + 10, paint);
                    } else if (TextUtils.equals(textContent.getTagName(), "sup")) {
                        setPaintStyle(textContent);
                        canvas.drawText(textContent.getContent(), textContent.getX(), textContent.getY() - 25, paint);
                    } else if (textContent.getTagClass().startsWith("xsl-footnote-link")) {
                        textContent.setY(textContent.getY() - 12);
                        ReMarkRectEntity reMarkRectEntity = new ReMarkRectEntity();
                        reMarkRectEntity.setKey(textContent.getTagClass());
                        Rect rect = getTextRect((int) textContent.getX(), (int) textContent.getY(), paint, content.getContent());
                        reMarkRectEntity.setRect(rect);
                        reMarkRectEntities.add(reMarkRectEntity);
                        if (nightStyle) {
                            paint.setColor(tagColor_nightStyle);
                        } else {
                            paint.setColor(tagColor_whiteStyle);
                        }
                        canvas.drawCircle(rect.centerX(), rect.centerY() + 2, FootNoteLinkR, paint);
                        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
                        int baseline = rect.top + (rect.bottom - rect.top - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
                        setPaintStyle(textContent);
                        paint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText("注", rect.centerX(), baseline - FootNoteLinkOffset, paint);
                    } else {
                        setPaintStyle(textContent);
                        if (textContent.getTagName().equals("a") && textContent.getTagClass().startsWith("http")) {
                            LinkRectEntity linkRectEntity = new LinkRectEntity();
                            linkRectEntity.setLinkUrl(textContent.getTagClass());
                            linkRectEntity.setRect(getLinkRect(textContent.getX(), textContent.getY(), paint, textContent.getContent()));
                            linkRectEntities.add(linkRectEntity);
                        }
                        canvas.drawText(textContent.getContent(), textContent.getX(), textContent.getY(), paint);
                    }
                } else {
                    Content imageContent = content;
                    setPaintStyle(imageContent);
                    try {
                        Bitmap bt = null;
                        if (content.getTagClass().equals("in-line")) {
                            bt = getInLineBitmap(imageContent);
                            canvas.drawBitmap(bt, imageContent.getX(), imageContent.getY() - bt.getHeight(), paint);
                        } else {
                            bt = getImageBitmap(imageContent);
                            int startX = 0;
                            int startY = 0;
                            if (getWidth() > bt.getWidth()) {
                                startX = (getWidth() - bt.getWidth()) / 2;
                            }
                            if (bt != null) {
                                Rect rect = new Rect();
                                rect.set(startX, (int) imageContent.getY(), startX + bt.getWidth(), (int) imageContent.getY() + bt.getHeight());
                                BitmapRectEntity entity = new BitmapRectEntity();
                                entity.setRect(rect);
                                entity.setImageContent(imageContent.getContent());
                                entity.setTagClass(imageContent.getTagClass());
                                bitmapRects.add(entity);
                                startY = (int) imageContent.getY();
                                canvas.drawBitmap(bt, startX, startY, null);
                                bt.recycle();
                            }
                        }
                    } catch (FileNotFoundException e) {
                    } catch (OutOfMemoryError oom) {
                    } catch (Exception ee) {
                    }
                }
            }
        }


        super.onDraw(canvas);
    }

    private Bitmap getImageBitmap(Content imageContent) throws FileNotFoundException, OutOfMemoryError {
        String imageFilePath = imageContent.getContent();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_4444;
        Bitmap resBitmap = BitmapFactory.decodeFile(imageFilePath);
        double[] data = loadDisplayImageContent(resBitmap, imageContent.getY());
        if (resBitmap.getWidth() == data[0] && resBitmap.getHeight() == data[1]) {
            return resBitmap;
        } else {
            return zoomImg(resBitmap, (int) data[0], (int) data[1]);
        }


    }

    /**
     * 读取图片属性，进行缩放
     */
    private double[] loadDisplayImageContent(Bitmap resBitmap, float curY) {
        double[] data = {0, 0};
        if (resBitmap != null) {
            double originalW = resBitmap.getWidth();
            double originalH = resBitmap.getHeight();
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
                    if (h > getHeight()) {
                        h = getDisplayH(curY);
                        w = h * f;
                    }
                } else {
                    w = originalW;
                    h = originalH;
                }
            } else {
                //如果比屏幕宽度的一半还小进行缩放 取1.8是为了让它比适配更多的小图
                if (originalW < (getDisplayW() / 1.8)) {
                    originalW = originalW * 1.2;
                    originalH = originalH * 1.2;
                }
                if (originalH > getDisplayH(curY)) {
                    h = getDisplayH(curY);
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

    private int getDisplayW() {
        return getWidth();
    }

    private int getDisplayH(float curY) {
        return (int) (getHeight() - curY - 66);
    }

    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    private Bitmap getInLineBitmap(Content imageContent) throws FileNotFoundException, OutOfMemoryError {
        String imageFilePath = imageContent.getContent();
        Bitmap resBitmap = BitmapFactory.decodeFile(imageFilePath);
        return resBitmap;
    }


    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d("qq", "textSelectStatus:" + textSelectStatus);
        if (!textSelectStatus) {
            boolean clickImage = false;
            boolean clickReMark = false;
            boolean isSinglepage = false;
            if (curPage != null && curPage.getContents() != null) {
                for (Content content : curPage.getContents()) {
                    if (content.getTagClass().equals("cover") || content.getTagClass().equals("singlepage")) {
                        isSinglepage = true;
                        break;
                    }
                }
                if (!isSinglepage) {
                    for (BitmapRectEntity entity : bitmapRects) {
                        if (entity.getRect().contains((int) e.getX(), (int) e.getY())) {
                            if (listener != null) {
                                if (!TextUtils.isEmpty(entity.getTagClass())) {
                                    if (!entity.getTagClass().equals("cover") && !entity.getTagClass().equals("singlepage")) {
                                        listener.onImageClick(entity.getImageContent());
//                                    listener.omImageClick(entity.getImageContent(),"");
                                        return true;
                                    }
                                } else {
                                    listener.onImageClick(entity.getImageContent());
                                    return true;
                                }
                            }
                            break;
                        }
                    }
                }
                for (ReMarkRectEntity entity : reMarkRectEntities) {
                    if (entity.getRect().contains((int) e.getX(), (int) e.getY())) {
                        if (listener != null) {
                            listener.onReMarkClick(curPage.getMapDBKey(), entity.getKey(), entity.getRect());
                            return true;
                        }
                    }
                }

                for (LinkRectEntity entity : linkRectEntities) {
                    if (entity.getRect().contains((int) e.getX(), (int) e.getY())) {
                        if (listener != null) {
                            listener.onLinkClick(entity.getLinkUrl());
                            return true;
                        }
                    }
                }
                if (!clickImage && !clickReMark) {
                    float x = e.getX();
                    if (x < getWidth() / 3) {
                        if (listener != null) {
                            listener.onPageLeftClick();
                        }
                    } else if (x > (getWidth() / 3 * 2)) {
                        if (listener != null) {
                            listener.onPageRightClick();
                        }
                    } else {
                        if (listener != null) {
                            listener.onPageCenterClick(curPage);
                        }
                    }
                }
            }
        } else {
            cancelSelectStatus();
        }

        return true;
    }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d("qq","onScroll-e1:"+e1.getX()+"--"+e2.getY()+"---e2:"+e2.getX()+"--"+e2.getY());
        Log.d("qq","onScroll-distanceX:"+distanceX+"--distanceY:"+distanceY);
        if (textSelectStatus){
            List<LineRectEntity> lines = getSelectLinesByTouchEvent(e2);
            selectLines.clear();
            selectLines.addAll(lines);
            postInvalidate();
            return true;
        }else{
            return false;
        }
    }


    @Override
    public void onLongPress(MotionEvent e) {
        if (!textSelectStatus) {
            initSelectStatus(e);
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        Log.d("qq","onFling-e1:"+e1.getX()+"--"+e2.getY()+"---e2:"+e2.getX()+"--"+e2.getY());
//        Log.d("qq","onFling-velocityX:"+velocityX+"--velocityY:"+velocityY);
        return false;

    }




    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private void cancelSelectStatus() {
        initSelectX = -1;
        initSelectY = -1;
        textSelectStatus = false;
        selectLines.clear();
        postInvalidate();
    }

    private void initSelectStatus(MotionEvent event) {

        LineRectEntity entity = getLineByEvent(event);
        Log.d("qq", "entity:" + (entity == null));
        if (entity != null) {
            initSelectX = event.getX();
            initSelectY = event.getY();
            selectLines.clear();
            textSelectStatus = true;
            selectLines.add(entity);
            postInvalidate();
        }

    }

    private LineRectEntity getLineByEvent(MotionEvent event) {
        for (LineRectEntity entity : lineRectEntities) {
            if (entity.getRect().contains((int) event.getX(), (int) event.getY())) {
                return entity;
            }
        }
        return null;
    }

    private List<LineRectEntity> getSelectLinesByTouchEvent(MotionEvent flingEvent){
        List<LineRectEntity> lines = new ArrayList<LineRectEntity>();
        float sX = initSelectX;
        float sY = initSelectY;
        float eX = flingEvent.getX();
        float eY = flingEvent.getY();
        if (eY>sY){
            for (LineRectEntity entity : lineRectEntities) {
                if (entity.getRect().top>= sY &&entity.getRect().bottom<=eY||entity.getRect().contains((int)sX,(int)sY)) {
                    lines.add(entity);
                }
            }
        }else {
            for (LineRectEntity entity : lineRectEntities) {
                if (entity.getRect().top>= eY &&entity.getRect().bottom<=sY ) {
                    lines.add(entity);
                }
            }
        }

        return lines;
    }


    boolean textSelectStatus = false;

    private void addContent2Line(Content textContent) {
        if (lineRectEntities.isEmpty() || lineRectEntities.get(lineRectEntities.size() - 1).getLineY() != textContent.getY()) {
            LineRectEntity entity = new LineRectEntity();
            Rect lineRect = new Rect();
            lineRect.set(0, (int) textContent.getY() - LINEHEIGHT +15, getWidth(), (int) textContent.getY() + 5);
            entity.setRect(lineRect);
            entity.appendLineString(textContent.getContent(), paint);
            entity.setLineY(textContent.getY());
            lineRectEntities.add(entity);
        } else {
            lineRectEntities.get(lineRectEntities.size() - 1).appendLineString(textContent.getContent(), paint);
        }
    }


    public interface onContentClickListener {
        public void onPageCenterClick(ContentPage page);

        public void onPageLeftClick();

        public void onPageRightClick();

        public void onImageClick(String imagePath);

        public void onReMarkClick(String mapDbKey, String dbKey, Rect rect);

        public void onLinkClick(String linkString);
    }
    public static class BitmapRectEntity {
        Rect rect;
        String imageContent = "";
        String tagClass = "";

        public String getTagClass() {
            return tagClass;
        }

        public void setTagClass(String tagClass) {
            this.tagClass = tagClass;
        }

        public Rect getRect() {
            return rect;
        }

        public void setRect(Rect rect) {
            this.rect = rect;
        }

        public String getImageContent() {
            return imageContent;
        }

        public void setImageContent(String imageContent) {
            this.imageContent = imageContent;
        }
    }

    public static class ReMarkRectEntity {
        Rect rect;
        String key;

        public Rect getRect() {
            return rect;
        }

        public void setRect(Rect rect) {
            this.rect = rect;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    public static class LinkRectEntity {
        Rect rect;
        String linkUrl;

        public Rect getRect() {
            return rect;
        }

        public void setRect(Rect rect) {
            this.rect = rect;
        }

        public String getLinkUrl() {
            return linkUrl;
        }

        public void setLinkUrl(String linkUrl) {
            this.linkUrl = linkUrl;
        }
    }

    public static class LineRectEntity {
        Rect rect;
        String lineString;
        float lineY;

        public Rect getRect() {
            return rect;
        }

        public void setRect(Rect rect) {
            this.rect = rect;
        }

        public String getLineString() {
            return lineString;
        }

        public void setLineString(String lineString) {
            this.lineString = lineString;
        }

        public void appendLineString(String appendString, Paint paint) {
            this.lineString = lineString + appendString;
        }

        public float getLineY() {
            return lineY;
        }

        public void setLineY(float lineY) {
            this.lineY = lineY;
        }
    }


    private static float getTextWidth(String text, Paint paint) {
        return paint.measureText(text);
    }

    private static int getFontHeight(Paint paint) {
        paint.setTextSize(textSize_default);
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) Math.ceil(fm.descent - fm.ascent);
    }

    private static Rect getTextRect(int x, int y, Paint paint, String string) {
        Rect rect = new Rect();
        int h = getFontHeight(paint);
        int w = h;
        rect.set(x - 25, y - h - 5, x + w + 15, y + 25);
        return rect;
    }

    private static Rect getLinkRect(float x, float y, Paint paint, String linkString) {
        Rect rect = new Rect();
        int h = getFontHeight(paint);
        int w = (int) getTextWidth(linkString, paint);
        rect.set((int) (x - 200), (int) (y - h - 75), (int) (x + w + 200), (int) (y + 75));
        return rect;
    }
}
