/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 4. 21 오전 11:54
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.app.Activity;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.joas.ocppui_LS_2ch.MultiChannelUIManager;
import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.TypeDefine;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.DecimalFormat;

public class PageUnplugView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    int channel;

    public PageUnplugView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_unplug_land, this, true);


    }


    @Override
    public void onPageActivate(int chan) {
        channel = chan;

    }

    @Override
    public void onPageDeactivate() {

    }
}
