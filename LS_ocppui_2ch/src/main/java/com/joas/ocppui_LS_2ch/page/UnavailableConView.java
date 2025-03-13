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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;

public class UnavailableConView extends FrameLayout {
    Activity mainActivity;
    MultiChannelUIManager flowManager;

    boolean firstvisible = true;

    final View viewThis = this;
    int channel;

    Button btCert;

    public UnavailableConView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_unavailable_con2, this, true);

        initComponents();
    }


    void initComponents(){
        btCert = findViewById(R.id.btCert2);
        btCert.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                viewThis.setVisibility(INVISIBLE);

            }
        });
    }


    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            channel = flowManager.pageManager.channel;
        }
        else {
        }
    }

}