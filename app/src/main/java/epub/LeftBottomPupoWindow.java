package epub;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import com.example.liuqing.qepubreader.R;

import epub.tool.Preferences;


public class LeftBottomPupoWindow extends PopupWindow {

	private ImageButton btn_black, btn_dir;
    private ImageButton btn_nightstyle;
	private View mMenuView;
	private PopWindowsDismiss popWindowsDismiss;
    Preferences preferences;
    boolean nightStyle = false;
	public LeftBottomPupoWindow(Activity context, final OnClickListener itemsOnClick, PopWindowsDismiss popWindowsDismiss) {
		super(context);

		this.popWindowsDismiss = popWindowsDismiss;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        preferences = new Preferences(context);
        nightStyle = preferences.getNightStyle();
		mMenuView = inflater.inflate(R.layout.bottom_alert_dialog, null);
		btn_black = (ImageButton) mMenuView.findViewById(R.id.black);
		btn_dir = (ImageButton) mMenuView.findViewById(R.id.dir);
        btn_nightstyle = (ImageButton) mMenuView.findViewById(R.id.nightstyle);
		String phoneName = android.os.Build.BRAND + "|" + android.os.Build.MODEL + "|" + android.os.Build.PRODUCT;
		if (phoneName.toLowerCase().contains("meizu")) {
			btn_black.setVisibility(View.VISIBLE);
		} else {
			btn_black.setVisibility(View.GONE);
		}

		// 设置按钮监听
		btn_black.setOnClickListener(itemsOnClick);
		btn_dir.setOnClickListener(itemsOnClick);
        btn_nightstyle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                itemsOnClick.onClick(v);
                postNightStyle();
            }
        });
        postNightStyle();
		// 设置SelectPicPopupWindow的View
		this.setContentView(mMenuView);
		// 设置SelectPicPopupWindow弹出窗体的宽
		this.setWidth(LayoutParams.MATCH_PARENT);
		// 设置SelectPicPopupWindow弹出窗体的高
		this.setHeight(LayoutParams.MATCH_PARENT);
		// 设置SelectPicPopupWindow弹出窗体可点击
		this.setFocusable(false);
		// 设置SelectPicPopupWindow弹出窗体动画效果
		this.setAnimationStyle(R.style.AnimBottom);
		// 实例化一个ColorDrawable颜色为透明
		ColorDrawable dw = new ColorDrawable(0x00000000);
		// 设置SelectPicPopupWindow弹出窗体的背景
		this.setBackgroundDrawable(dw);
		// mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
		mMenuView.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {

				int height = mMenuView.findViewById(R.id.pop_layout).getHeight();
				int y = (int) event.getY();
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (y < height) {
						dismiss();
					}
				}
				return true;
			}
		});

	}


    private void postNightStyle(){
        nightStyle = preferences.getNightStyle();
        if (nightStyle){
            btn_black.setBackgroundResource(R.drawable.btn_pages_back_night);
            btn_dir.setBackgroundResource(R.drawable.btn_pages_contents_night);
            btn_nightstyle.setBackgroundResource(R.drawable.btn_pages_nightstyle_night);
        }else{
            btn_black.setBackgroundResource(R.drawable.btn_pages_back);
            btn_dir.setBackgroundResource(R.drawable.btn_pages_contents);
            btn_nightstyle.setBackgroundResource(R.drawable.btn_pages_nightstyle);
        }
    }

	@Override
	public void showAsDropDown(View anchor) {
		super.showAsDropDown(anchor);
	}

	@Override
	public void showAsDropDown(View anchor, int xoff, int yoff) {
		super.showAsDropDown(anchor, xoff, yoff);
	}

	@Override
	public void showAtLocation(View parent, int gravity, int x, int y) {
		super.showAtLocation(parent, gravity, x, y);
	}

	@Override
	public void dismiss() {
		popWindowsDismiss.dismiss();
		super.dismiss();
	}


	public interface PopWindowsDismiss {
		void dismiss();
	}

}
