package com.sc.camera.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.sc.camera.R;


@SuppressWarnings("deprecation")
public class CameraContainer extends RelativeLayout {

	public final static String TAG = "CameraContainer";
	/** 相机绑定的SurfaceView */
	private CameraView mCameraView;
	/** 缩放级别拖动条 */
	private SeekBar mZoomSeekBar;
	/** 用以执行定时任务的Handler对象 */
	private Handler mHandler;

	@SuppressLint("ClickableViewAccessibility")
	public CameraContainer(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
		mHandler = new Handler();
		setOnTouchListener(new TouchListener());
	}

	/**
	 * 初始化子控件
	 * 
	 * @param context
	 */
	private void initView(Context context) {
		inflate(context, R.layout.cameracontainer, this);
		mCameraView = (CameraView) findViewById(R.id.cameraView);

		mZoomSeekBar = (SeekBar) findViewById(R.id.zoomSeekBar);
		// 获取当前照相机支持的最大缩放级别，值小于0表示不支持缩放。当支持缩放时，加入拖动条。
		int maxZoom = mCameraView.getMaxZoom();
		if (maxZoom > 0) {
			mZoomSeekBar.setMax(maxZoom);
			mZoomSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		}
	}

	public boolean startRecord() {
		return true;
	}

	public Bitmap stopRecord() {
		return null;
	}
	public void startPreview(){
		mCameraView.startPreview() ;
	}
	/**
	 * 改变相机模式 在拍照模式和录像模式间切换 两个模式的初始缩放级别不同
	 * 
	 * @param zoom
	 *            缩放级别
	 */
	public void switchMode(int zoom) {
		mZoomSeekBar.setProgress(zoom);
		mCameraView.setZoom(zoom);
		// 自动对焦
		mCameraView.onFocus(new Point(getWidth() / 2, getHeight() / 2), autoFocusCallback);
	}

	/**
	 * 前置、后置摄像头转换
	 */
	public void switchCamera() {
		mCameraView.switchCamera();
	}



	public void takePicture(PictureCallback callback) {
		mCameraView.takePicture(callback);
	}

	
	public void start(){
		mCameraView.startPreview() ;
	}
	
	public int getMaxZoom() {
		return mCameraView.getMaxZoom();
	}

	public void setZoom(int zoom) {
		mCameraView.setZoom(zoom);
	}

	public int getZoom() {
		return mCameraView.getZoom();
	}

	private final OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			mCameraView.setZoom(progress);
			mHandler.removeCallbacksAndMessages(mZoomSeekBar);
			// ZOOM模式下 在结束两秒后隐藏seekbar 设置token为mZoomSeekBar用以在连续点击时移除前一个定时任务
			mHandler.postAtTime(new Runnable() {

				@Override
				public void run() {
					mZoomSeekBar.setVisibility(View.GONE);
				}
			}, mZoomSeekBar, SystemClock.uptimeMillis() + 2000);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {

		}
	};

	private final AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// 聚焦之后根据结果修改图片

			
			// 恢复到 FOCUS_MODE_CONTINUOUS_PICTURE 模式
			camera.cancelAutoFocus(); 
		}
	};

	private final class TouchListener implements OnTouchListener {

		/** 记录是拖拉照片模式还是放大缩小照片模式 */

		private static final int MODE_INIT = 0;
		/** 放大缩小照片模式 */
		private static final int MODE_ZOOM = 1;
		private int mode = MODE_INIT;// 初始状态

		/** 用于记录拖拉图片移动的坐标位置 */

		private float startDis;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			/** 通过与运算保留最后八位 MotionEvent.ACTION_MASK = 255 */
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			// 手指压下屏幕
			case MotionEvent.ACTION_DOWN:
				mode = MODE_INIT;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				// 如果mZoomSeekBar为null 表示该设备不支持缩放 直接跳过设置mode Move指令也无法执行
				if (mZoomSeekBar == null)
					return true;
				// 移除token对象为mZoomSeekBar的延时任务
				mHandler.removeCallbacksAndMessages(mZoomSeekBar);
				mZoomSeekBar.setVisibility(View.VISIBLE);

				mode = MODE_ZOOM;
				/** 计算两个手指间的距离 */
				startDis = distance(event);
				break;
			case MotionEvent.ACTION_MOVE:
				if (mode == MODE_ZOOM) {
					// 只有同时触屏两个点的时候才执行
					if (event.getPointerCount() < 2)
						return true;
					float endDis = distance(event);// 结束距离
					// 每变化10f zoom变1
					int scale = (int) ((endDis - startDis) / 10f);
					if (scale >= 1 || scale <= -1) {
						int zoom = mCameraView.getZoom() + scale;
						// zoom不能超出范围
						if (zoom > mCameraView.getMaxZoom())
							zoom = mCameraView.getMaxZoom();
						if (zoom < 0)
							zoom = 0;
						mCameraView.setZoom(zoom);
						mZoomSeekBar.setProgress(zoom);
						// 将最后一次的距离设为当前距离
						startDis = endDis;
					}
				}
				break;
			// 手指离开屏幕
			case MotionEvent.ACTION_UP:
				if (mode != MODE_ZOOM) {
					// 设置聚焦
					Point point = new Point((int) event.getX(), (int) event.getY());
					mCameraView.onFocus(point, autoFocusCallback);
				} else {
					// ZOOM模式下 在结束两秒后隐藏seekbar
					// 设置token为mZoomSeekBar用以在连续点击时移除前一个定时任务
					mHandler.postAtTime(new Runnable() {

						@Override
						public void run() {
							mZoomSeekBar.setVisibility(View.GONE);
						}
					}, mZoomSeekBar, SystemClock.uptimeMillis() + 2000);
				}
				break;
			}
			return true;
		}

		/** 计算两个手指间的距离 */
		private float distance(MotionEvent event) {
			float dx = event.getX(1) - event.getX(0);
			float dy = event.getY(1) - event.getY(0);
			/** 使用勾股定理返回两点之间的距离 */
			return (float) Math.sqrt(dx * dx + dy * dy);
		}

	}

}