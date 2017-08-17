package com.sc.camera.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.sc.camera.CameraUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
public class CameraView extends SurfaceView  {

	public final static String TAG = "CameraView";
	/** 和该View绑定的Camera对象 */
	private Camera mCamera;
	/** 当前闪光灯类型，默认为自动 */
	private FlashMode mFlashMode = FlashMode.OFF;
	/** 当前缩放级别 默认为0 */
	private int mZoom = 0;
	/** 当前屏幕旋转角度 */
	private int mOrientation = 0;
	/** 是否打开前置相机,true为前置,false为后置 */
	private boolean mIsFrontCamera;
	/** 录像类 */
	private MediaRecorder mMediaRecorder;
	/** 相机配置，在录像前记录，用以录像结束后恢复原配置 */
	private Camera.Parameters mParameters;
	
	private static final int CAMERA_BACK_ID =  CameraInfo.CAMERA_FACING_BACK;

	public CameraView(Context context) {
		super(context);
		getHolder().addCallback(callback);
		// openCamera();
		mIsFrontCamera = false;
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// 初始化容器
		getHolder().addCallback(callback);
		// openCamera();
		mIsFrontCamera = false;
	}

	private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				if (mCamera == null) {
					openCamera();
				}
				setCameraParameters();
				mCamera.setPreviewDisplay(getHolder());
			} catch (Exception e) {
				Toast.makeText(getContext(), "打开相机失败", Toast.LENGTH_SHORT).show();
				Log.e(TAG, e.getMessage() + "");
			}
			try {
				mCamera.startPreview();
			} catch (Exception e) {
				Toast.makeText(getContext(), "打开相机失败", Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			updateCameraOrientation();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// 停止录像
			stopRecord();
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}

		}
	};

	protected boolean isRecording() {
		return mMediaRecorder != null;
	}

	/**
	 * 开始录像
	 *
	 * @return 开始录像是否成功
	 */
	public boolean startRecord() {
		return false ;
	}

	public Bitmap stopRecord() {
		return null ;
	}


	/**
	 * 获取帧缩略图，根据容器的高宽进行缩放
	 *
	 * @param filePath
	 * @return
	 */
	public Bitmap getVideoThumbnail(String filePath) {
		Bitmap bitmap = null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			retriever.setDataSource(filePath);
			bitmap = retriever.getFrameAtTime(-1);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} finally {
			try {
				retriever.release();
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		if (bitmap == null)
			return null;
		// Scale down the bitmap if it's too large.
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Log.i(TAG, "bitmap:" + width + " " + height);
		int pWidth = getWidth();// 容器宽度
		int pHeight = getHeight();// 容器高度
		Log.i(TAG, "parent:" + pWidth + " " + pHeight);
		// 获取宽高跟容器宽高相比较小的倍数，以此为标准进行缩放
		float scale = Math.min((float) width / pWidth, (float) height / pHeight);
		Log.i(TAG, scale + "");
		int w = Math.round(scale * pWidth);
		int h = Math.round(scale * pHeight);
		Log.i(TAG, "parent:" + w + " " + h);
		bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
		return bitmap;
	}

	/**
	 * 转换前置和后置照相机
	 */
	public void switchCamera() {
		mIsFrontCamera = !mIsFrontCamera;
		openCamera();
		if (mCamera != null) {
			setCameraParameters();
			updateCameraOrientation();
			try {
				mCamera.setPreviewDisplay(getHolder());
				mCamera.startPreview();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 根据当前照相机状态(前置或后置)，打开对应相机
	 */
	private boolean openCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		if (mIsFrontCamera) {
			CameraInfo cameraInfo = new CameraInfo();
			for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
				Camera.getCameraInfo(i, cameraInfo);
				if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
					try {
						mCamera = Camera.open(i);
					} catch (Exception e) {
						mCamera = null;
						return false;
					}

				}
			}
		} else {
			try {
				mCamera = Camera.open();
			} catch (Exception e) {
				mCamera = null;
				return false;
			}

		}
		return true;
	}

	/**
	 * 获取当前闪光灯类型
	 *
	 * @return
	 */
	public FlashMode getFlashMode() {
		return mFlashMode;
	}

	/**
	 * 设置闪光灯类型
	 *
	 * @param flashMode
	 */
	public void setFlashMode(FlashMode flashMode) {
		if (mCamera == null)
			return;
		mFlashMode = flashMode;
		Camera.Parameters parameters = mCamera.getParameters();
		switch (flashMode) {
		case ON:
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
			break;
		case AUTO:
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			break;
		case TORCH:
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			break;
		default:
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			break;
		}
		mCamera.setParameters(parameters);
	}

	public void takePicture(PictureCallback callback) {
		try {
			mCamera.takePicture(null, null, callback);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startPreview(){
		mCamera.startPreview() ;
	}

	/**
	 * 手动聚焦
	 *
	 * @param point
	 *            触屏坐标
	 */
	protected void onFocus(Point point, AutoFocusCallback callback) {
		Camera.Parameters parameters = mCamera.getParameters();
		// 不支持设置自定义聚焦，则使用自动聚焦，返回
		if (parameters.getMaxNumFocusAreas() <= 0) {
			mCamera.autoFocus(callback);
			return;
		}
		List<Area> areas = new ArrayList<Area>();
		int left = point.x - 300;
		int top = point.y - 300;
		int right = point.x + 300;
		int bottom = point.y + 300;
		left = left < -1000 ? -1000 : left;
		top = top < -1000 ? -1000 : top;
		right = right > 1000 ? 1000 : right;
		bottom = bottom > 1000 ? 1000 : bottom;
		areas.add(new Area(new Rect(left, top, right, bottom), 100));
		parameters.setFocusAreas(areas);
		try {
			// 本人使用的小米手机在设置聚焦区域的时候经常会出异常，看日志发现是框架层的字符串转int的时候出错了，
			// 目测是小米修改了框架层代码导致，在此try掉，对实际聚焦效果没影响
			mCamera.setParameters(parameters);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		mCamera.autoFocus(callback);
	}

	/**
	 * 获取最大缩放级别，最大为40
	 *
	 * @return
	 */
	public int getMaxZoom() {
		if (mCamera == null)
			return -1;
		Camera.Parameters parameters = mCamera.getParameters();
		if (!parameters.isZoomSupported())
			return -1;
		return parameters.getMaxZoom() > 40 ? 40 : parameters.getMaxZoom();
	}

	/**
	 * 设置相机缩放级别
	 *
	 * @param zoom
	 */
	public void setZoom(int zoom) {
		if (mCamera == null)
			return;
		Camera.Parameters parameters;
		// 注意此处为录像模式下的setZoom方式。在Camera.unlock之后，调用getParameters方法会引起android框架底层的异常
		// stackoverflow上看到的解释是由于多线程同时访问Camera导致的冲突，所以在此使用录像前保存的mParameters。
		if (mParameters != null)
			parameters = mParameters;
		else {
			parameters = mCamera.getParameters();
		}

		if (!parameters.isZoomSupported())
			return;
		parameters.setZoom(zoom);
		mCamera.setParameters(parameters);
		mZoom = zoom;
	}

	public int getZoom() {
		return mZoom;
	}

	/**
	 * 设置照相机参数
	 */
	private void setCameraParameters() {
		Camera.Parameters parameters = mCamera.getParameters();

		// XXX imknown: 让系统决定, 已再 开源项目中 提交了 issue
		// https://github.com/LinJZong/AndroidProject
//		// 选择合适的预览尺寸
//		List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
//		if (sizeList.size() > 0) {
//			Size cameraSize = sizeList.get(0);
//			// 预览图片大小
//			parameters.setPreviewSize(cameraSize.width, cameraSize.height);
//		}

		// 设置生成的图片大小
		List<Size> sizeList = parameters.getSupportedPictureSizes();
		if (sizeList.size() > 0) {
			Size cameraSize = sizeList.get(0);
			for (Size size : sizeList) {
				int resolution = size.width * size.height;
				if (100_0000 <= resolution && resolution <= 200_0000) {
					cameraSize = size;
					break;
				}
			}
			parameters.setPictureSize(cameraSize.width, cameraSize.height);
		}
		// 设置图片格式
		parameters.setPictureFormat(ImageFormat.JPEG);
		parameters.setJpegQuality(100);
		parameters.setJpegThumbnailQuality(100);

		// 自动聚焦模式
		if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		}

		// API 14 之后, 可以配合 camera.autoFocus(AutoFocusCallback) 使用
		if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

		mCamera.setParameters(parameters);
		// 设置闪光灯模式。此处主要是用于在相机摧毁后又重建，保持之前的状态
		setFlashMode(mFlashMode);
		// 设置缩放级别
		setZoom(mZoom);
		// 开启屏幕朝向监听
		startOrientationChangeListener();
	}

	/**
	 * 启动屏幕朝向改变监听函数 用于在屏幕横竖屏切换时改变保存的图片的方向
	 */
	private void startOrientationChangeListener() {
		OrientationEventListener mOrEventListener = new OrientationEventListener(getContext()) {
			@Override
			public void onOrientationChanged(int rotation) {

				if (((rotation >= 0) && (rotation <= 45)) || (rotation > 315)) {
					rotation = 0;
				} else if ((rotation > 45) && (rotation <= 135)) {
					rotation = 90;
				} else if ((rotation > 135) && (rotation <= 225)) {
					rotation = 180;
				} else if ((rotation > 225) && (rotation <= 315)) {
					rotation = 270;
				} else {
					rotation = 0;
				}
				if (rotation == mOrientation)
					return;
				mOrientation = rotation;
				updateCameraOrientation();
			}
		};
		mOrEventListener.enable();
	}

	/**
	 * 根据当前朝向修改保存图片的旋转角度
	 */
	private void updateCameraOrientation() {
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			// rotation参数为 0、90、180、270。水平方向为0。
			int rotation = 90 + mOrientation == 360 ? 0 : 90 + mOrientation;
			// 前置摄像头需要对垂直方向做变换，否则照片是颠倒的
			if (mIsFrontCamera) {
				if (rotation == 90)
					rotation = 270;
				else if (rotation == 270)
					rotation = 90;
			}
			parameters.setRotation(rotation);// 生成的图片转90°

			// XXX imknown: 并不是 所有手机 竖屏拍照 就是 旋转90°, 我的 LG Nexus 5X 就是 270°
//			// 预览图片旋转90°
//			mCamera.setDisplayOrientation(90);// 预览转90°

			int orientation = CameraUtils.getCameraDisplayOrientation((Activity) getContext(), CAMERA_BACK_ID);
			mCamera.setDisplayOrientation(orientation);

			mCamera.setParameters(parameters);
		}
	}

	/**
	 * @Description: 闪光灯类型枚举 默认为关闭
	 */
	public enum FlashMode {
		/** ON:拍照时打开闪光灯 */
		ON,
		/** OFF：不打开闪光灯 */
		OFF,
		/** AUTO：系统决定是否打开闪光灯 */
		AUTO,
		/** TORCH：一直打开闪光灯 */
		TORCH
	}
}