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
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;

import java.util.Timer;
import java.util.TimerTask;

public class AdminPasswordInputView extends FrameLayout {
    Timer timer = null;
    Activity mainActivity;
    int timerCount = 0;
    MultiChannelUIManager uiManager;
    String password = "";
    TextView tvAdminPwdInput;

    public AdminPasswordInputView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        uiManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_admin_pwd, this, true);

        initComponents();
    }

    void initComponents() {
        tvAdminPwdInput = findViewById(R.id.tvAdminPwdInput);
        tvAdminPwdInput.setText("");

        int[] btNumIds = { R.id.btNum0, R.id.btNum1, R.id.btNum2, R.id.btNum3, R.id.btNum4,
                        R.id.btNum5, R.id.btNum6, R.id.btNum7, R.id.btNum8, R.id.btNum9 };
        Button[] btNums = new Button[10];

        for (int i=0; i<10; i++) {
            btNums[i] = findViewById(btNumIds[i]);
            final int finalI = i;
            btNums[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    OnClickNum(""+finalI);
                }
            });
        }

        Button btStar = findViewById(R.id.btNumStar);
        btStar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum("*");
            }
        });

        Button btSharp = findViewById(R.id.btNumSharp);
        btSharp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum("#");
            }
        });


        Button btBS = findViewById(R.id.btNumBS);
        btBS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnNumBS();
            }
        });

        Button btClear = findViewById(R.id.btNumClear);
        btClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnNumClear();
            }
        });

        Button btCancel= findViewById(R.id.btNumCancel);

        final View viewThis = this;
        btCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                viewThis.setVisibility(INVISIBLE);
            }
        });

        Button btOK= findViewById(R.id.btNumOK);
        btOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                uiManager.onAdminPasswordOK(password);
            }
        });
    }

    void init() {
        password = "";
        tvAdminPwdInput.setText("");
        startTimer();
    }

    void OnClickNum(String number) {
        password += number;
        tvAdminPwdInput.setText(password);
    }

    void OnNumBS() {
        try {
            password = password.replaceFirst(".$", "");
        }
        catch(Exception e) {}
        tvAdminPwdInput.setText(password);
    }

    void OnNumClear() {
        password = "";
        tvAdminPwdInput.setText(password);
    }

    void startTimer() {
        if ( timer != null ) timer.cancel();
        timer = new Timer();
        timerCount = 60;

        final View viewThis = this;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerCount--;
                if ( timerCount == 0 ) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewThis.setVisibility(INVISIBLE);
                        }
                    });
                    if ( timer != null ) timer.cancel();
                }
            }
        }, 1000, 1000);
    }

    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            init();
        }
        else {
            if ( timer != null ) timer.cancel();
        }
    }

}