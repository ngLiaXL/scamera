package com.sc.camera.http;
public interface UploadCallback {

	void onStart();

	void onProgress(Integer... progress);

	void onCancelled();

	void onFinished(String result);
}