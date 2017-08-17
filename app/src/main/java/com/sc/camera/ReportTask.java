package com.sc.camera;

import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;


public class ReportTask extends TimerTask {

    private Context mContext;


    public ReportTask(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        SService.runIntentInService(mContext, new Intent(mContext, SService.class));

    }
}