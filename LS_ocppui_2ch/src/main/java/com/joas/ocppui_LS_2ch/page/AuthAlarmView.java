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
import android.os.Build;
import androidx.core.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.ChargeData;
import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class AuthAlarmView extends FrameLayout {
    TimeoutTimer timer = null;
    Activity mainActivity;
    int timerCount;
    MultiChannelUIManager flowManager;

    final View viewThis = this;
    ChargeData cData;
    TextView tvtimercount;
    ImageView ivAlarmicon;
    Button btRetry;
    Button btCert;

    boolean firstvisible = true;
    int channel;

    public AuthAlarmView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_message_box2, this, true);

        timerCount = flowManager.getUIFlowManager(channel).getChargeData().messageBoxTimeout;
        initComponents();
        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerCount--;
                tvtimercount.setText(String.valueOf(timerCount)+"초");
                if ( timerCount == 0 ) {
                    timerCount = flowManager.getUIFlowManager(channel).getChargeData().messageBoxTimeout;
                    viewThis .setVisibility(INVISIBLE);
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                    if ( timer != null ) timer.cancel();
                }

            }
        });
    }

    void initComponents(){
        tvtimercount = findViewById(R.id.tvtimercount);
        ivAlarmicon = (findViewById(R.id.ivAlarmicon));
        btRetry = findViewById(R.id.btretry);

        btCert = findViewById(R.id.btCert);

    }

    void initMessageBox() {
        cData = flowManager.getUIFlowManager(channel).getChargeData();

        tvtimercount.setText(String.valueOf(timerCount)+"초");

        TextView tvMessageBoxContent = findViewById(R.id.tvMessageBoxContent3);
        tvMessageBoxContent.setText(cData.messageBoxContent);


        if(cData.messageBoxTitle.equals("authtimeout")){
            ivAlarmicon.setImageResource(R.drawable.img_timeout);
            btRetry.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewThis.setVisibility(INVISIBLE);
                    flowManager.getUIFlowManager(channel).onRetryAuthclickEvent();
                }
            });


            btCert.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewThis.setVisibility(INVISIBLE);
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                }
            });

            btRetry.setVisibility(VISIBLE);
            btCert.setVisibility(VISIBLE);

        }
        else if(cData.messageBoxTitle.equals(mainActivity.getResources()
                .getString(R.string.str_auth_fail_title))){
            ivAlarmicon.setImageResource(R.drawable.img_autyfail);
            btRetry.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewThis.setVisibility(INVISIBLE);
                    flowManager.getUIFlowManager(channel).onRetryAuthclickEvent();
                }
            });
            btCert.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewThis.setVisibility(INVISIBLE);
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                }
            });


            btRetry.setVisibility(VISIBLE);
            btCert.setVisibility(VISIBLE);
        }
        else if(cData.messageBoxTitle.equals("serverconnecterror")){
            ivAlarmicon.setImageResource(R.drawable.img_network_error);
            btCert.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewThis.setVisibility(INVISIBLE);
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                }
            });

            btCert.setVisibility(VISIBLE);
            btRetry.setVisibility(GONE);
        }
        else if(cData.messageBoxTitle.equals("최초결제 실패")){
            ivAlarmicon.setImageResource(R.drawable.img_inserterror);

            btRetry.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewThis.setVisibility(INVISIBLE);
                }
            });


            btRetry.setVisibility(VISIBLE);
            btCert.setVisibility(GONE);
        }
        else if(cData.messageBoxTitle.equals("취소결제 실패")){
            ivAlarmicon.setImageResource(R.drawable.img_inserterror);
            btCert.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewThis.setVisibility(INVISIBLE);
                    flowManager.getUIFlowManager(channel).onPageCommonEvent(PageEvent.GO_HOME);
                }
            });

            btCert.setVisibility(VISIBLE);
            btRetry.setVisibility(GONE);
        }
        else{
            ivAlarmicon.setImageResource(R.drawable.img_inserterror);
            btCert.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewThis.setVisibility(INVISIBLE);
                }
            });

            btCert.setVisibility(VISIBLE);
            btRetry.setVisibility(GONE);
        }

        startTimer();
    }

    void startTimer() {
        timerCount = flowManager.getUIFlowManager(channel).getChargeData().messageBoxTimeout;
        stopTimer();
        timer.start();
    }

    void stopTimer(){
        if ( timer != null ) timer.cancel();
    }


    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            channel = flowManager.pageManager.channel;
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                if(firstvisible){
                    firstvisible =false;
                    return;
                }
            }
            initMessageBox();
        }
        else {
            stopTimer();
        }
    }
}