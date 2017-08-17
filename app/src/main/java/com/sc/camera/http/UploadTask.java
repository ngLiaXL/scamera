package com.sc.camera.http;

import android.os.AsyncTask;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class UploadTask extends AsyncTask<String, Integer, String> {

	private final UploadCallback mCallback;

	private MultipartEntity mEntity;

	public UploadTask(MultipartEntity entity, UploadCallback callback) {
		this.mEntity = entity;
		this.mCallback = callback;
		//new File("abc").delete();
		// 将文件添加到MultipartEntity
		//mEntity.addPart("key",new FileBody(new File("abc")));
		// new File("abc").delete();
	}

	@Override
	protected String doInBackground(String... sUrl) {
		String result = "";
		DefaultHttpClient httpClient = getHttpClient(sUrl[0]);
		try {
			HttpPost httpPost = new HttpPost(sUrl[0]);
			httpPost.addHeader("Accept-Encoding", "gzip");
			httpPost.addHeader("enctype", "multipart/form-data;");

			if (mEntity != null) {
				httpPost.setEntity(mEntity);
			}
			httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 300000); // 请求超时
			httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 300000);
			HttpResponse httpResp = httpClient.execute(httpPost);
			int res = httpResp.getStatusLine().getStatusCode();
			if (res == 200) {
				result = getJsonObject(httpResp);
			}

		} catch (Exception e) {
		} finally {
			if (httpClient != null && httpClient.getConnectionManager() != null) {
				httpClient.getConnectionManager().shutdown();
				httpClient = null;
			}
		}
		return result;

	}
	
	public static DefaultHttpClient getHttpClient(String url) {
		DefaultHttpClient httpClient = null;
		httpClient = new DefaultHttpClient();
		return httpClient;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (mCallback != null) {
			mCallback.onStart();
		}
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);
		if (mCallback != null) {
			mCallback.onProgress(progress);
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (mCallback != null) {
			mCallback.onFinished(result);
		}
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		if (mCallback != null) {
			mCallback.onCancelled();
		}
	}

	private String getJsonObject(HttpResponse httpResponse) {
		InputStream is = null;
		HttpEntity entity = httpResponse.getEntity();
		Header header = entity.getContentEncoding();
		try {
			if (header != null && header.getValue().equalsIgnoreCase("gzip")) { // 判断返回内容是否为gzip压缩格式
				is = new GZIPInputStream(entity.getContent());
			} else {
				is = entity.getContent();
			}
			String result = new String(readInputStream(is), "UTF-8");
			return result;
		} catch (Exception e) {
		}
		return null;
	}

	public byte[] readInputStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		while ((len = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, len);
		}
		inStream.close();
		return outStream.toByteArray();
	}

}
