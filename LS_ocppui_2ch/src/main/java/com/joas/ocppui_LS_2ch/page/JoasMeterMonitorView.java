/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.OCPPUI2CHActivity;
import com.joas.ocppui_LS_2ch.R;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.DecimalFormat;


public class JoasMeterMonitorView extends FrameLayout {
    public static final String TAG = "JoasMeterMonitorView";
    OCPPUI2CHActivity mainActivity;
    MultiChannelUIManager flowManager;

    RelativeLayout frameCommViewBox;

    ImageView imageViewMove;

    float orgX = 0;
    float orgY = 0;
    LayoutParams orgParam;

    TextView tvVolt[] = new TextView[2];
    TextView tvAmp[] = new TextView[2];
    TextView tvPower[] = new TextView[2];

    TimeoutTimer timer;


    public JoasMeterMonitorView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        mainActivity = (OCPPUI2CHActivity) activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_meter_monitor, this, true);

        timer = new TimeoutTimer(500, new TimeoutHandler() {
            @Override
            public void run() {
                updateDispInfo();
            }
        });

        initCommMonitor();

        initComponents();

    }

    void initCommMonitor() {
        frameCommViewBox = (RelativeLayout) findViewById(R.id.frameCommViewBox);

        imageViewMove = (ImageView) findViewById(R.id.imageCommArrowMove);
        imageViewMove.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                moveView(motionEvent);
                return true;
            }
        });

        Button btCommMNTHide = (Button) findViewById(R.id.btCommMNTHide);
        btCommMNTHide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHide();
            }
        });
    }

    void initComponents() {
        tvVolt[0] = (TextView) findViewById(R.id.tvVolt1);
        tvAmp[0] = (TextView) findViewById(R.id.tvAmp1);
        tvPower[0] = (TextView) findViewById(R.id.tvPower1);

        tvVolt[1] = (TextView) findViewById(R.id.tvVolt2);
        tvAmp[1] = (TextView) findViewById(R.id.tvAmp2);
        tvPower[1] = (TextView) findViewById(R.id.tvPower2);
    }

    void moveView(MotionEvent motionEvent) {
        //LogWrapper.v("MNT", "act:"+motionEvent.getAction()+", x:"+motionEvent.getX() + ", y:"+motionEvent.getY());
        float xpos = motionEvent.getRawX();
        float ypos = motionEvent.getRawY();

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                orgX = xpos;
                orgY = ypos;
                orgParam = (LayoutParams) frameCommViewBox.getLayoutParams();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffX = xpos - orgX;
                float diffY = ypos - orgY;
                orgParam.leftMargin += diffX;
                orgParam.topMargin += diffY;
                frameCommViewBox.setLayoutParams(orgParam);
                orgX = xpos;
                orgY = ypos;
                break;
        }
    }

    public void onHide() {
        this.setVisibility(INVISIBLE);
    }

    void startTimer() {
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if (timer != null) timer.cancel();
    }

    void updateDispInfo() {
        DecimalFormat decimalFormat = new DecimalFormat("####.##");
        for (int i = 0; i < 2; i++) {
            try {
                double meterVolt = mainActivity.getMeterService().readMeterVoltageCh(i);
                double meterCurrent = mainActivity.getMeterService().readMeterCurrentCh(i);
                double meterPower = (meterVolt * meterCurrent) / 1000.0;
                long meterVal = mainActivity.getMeterService().readMeterCh(i);


                if (meterVal == -1) {
                    tvVolt[i].setText("계량 통신 에러");
                    tvAmp[i].setText("계량 통신 에러");
                    tvPower[i].setText("계량 통신 에러");
                } else {
                    tvVolt[i].setText(decimalFormat.format(meterVolt) + " V");
                    tvAmp[i].setText(decimalFormat.format(meterCurrent) + " A");
                    tvPower[i].setText(decimalFormat.format(meterPower) + "kW");
                }


            } catch (Exception e) {
                tvVolt[i].setText("계량 통신 에러");
                tvAmp[i].setText("계량 통신 에러");
                tvPower[i].setText("계량 통신 에러");

                LogWrapper.e(TAG, "Meter err:" + e);
            }

        }

    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (visibility == VISIBLE) {
            updateDispInfo();

            startTimer();
        } else {
            stopTimer();
        }
    }


}
