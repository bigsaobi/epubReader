package epub;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.example.liuqing.qepubreader.R;

import epub.photo.PhotoViewAttacher;

/**
 * Created by liuqing on 15/1/26.
 */
public class ImageDetailActivity extends Activity {
    ImageView imageView;
    LinearLayout rootView;
    PhotoViewAttacher mAttacher;
    Bitmap bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_epubimagedetail);
        imageView = (ImageView) findViewById(R.id.image);
        rootView = (LinearLayout) findViewById(R.id.rootview);
        String imagePath = getIntent().getStringExtra("imagepath_key");
        try{
            imageView.setImageBitmap(getBitmap(imagePath));
        }catch (OutOfMemoryError oom){
            System.gc();
            try{
                imageView.setImageBitmap(getBitmap(imagePath));
            }catch (OutOfMemoryError oom1){}

        }

        mAttacher = new PhotoViewAttacher(imageView);
        mAttacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                ImageDetailActivity.this.finish();
            }
        });
        mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                ImageDetailActivity.this.finish();
            }
        });
    }

    private Bitmap getBitmap(String imagePath) throws OutOfMemoryError {
        bt = BitmapFactory.decodeFile(imagePath);
        return bt;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bt!=null&&!bt.isRecycled()){
            bt.recycle();
        }
    }
}
