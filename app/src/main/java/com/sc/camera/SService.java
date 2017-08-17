/**
 *
 */
package com.sc.camera;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import java.util.Timer;

public class SService extends Service {

    private static PowerManager.WakeLock sWakeLock;
    private static final Object LOCK = SService.class;


    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private Timer mReportTimer;

    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
        }

        private void onHandleIntent(Intent intent) {
            try {
                if (intent == null) {
                    return;
                }

                //
                Log.d("SService", "==============");

                sendBroadcast(new Intent("start_camera"));

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // releaseWakeLock();
            }

        }

    }


    public static void runIntentInService(Context context, Intent intent) {
        synchronized (LOCK) {
            if (sWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(
                        Context.POWER_SERVICE);
                sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "st");
            }
        }
        if (!sWakeLock.isHeld()) {
            sWakeLock.acquire();
        }
        context.startService(intent);
    }

    public static void releaseWakeLock() {
        synchronized (LOCK) {
            if (sWakeLock != null) {
                try {
                    if (sWakeLock.isHeld()) {
                        sWakeLock.release();
                    }
                } catch (Exception e) {
                }
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("SService");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        startScheduleTask();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendServiceHandleMessage(startId, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void startScheduleTask() {
        if (mReportTimer == null) {
            mReportTimer = new Timer();
            ReportTask lockTask = new ReportTask(this);
            mReportTimer.schedule(lockTask, MainActivity.INTERVAL, MainActivity.INTERVAL);
        }

    }

    private void sendServiceHandleMessage(int startId, Intent intent) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
        releaseReportTimer();
        releaseWakeLock();

    }

    private void releaseReportTimer() {
        if (mReportTimer != null) {
            mReportTimer.cancel();
            mReportTimer.purge();
        }
    }


}
