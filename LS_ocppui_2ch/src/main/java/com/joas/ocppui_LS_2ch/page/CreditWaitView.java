/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 6. 7. 오후 7:23
 *
 */

package com.joas.ocppui_LS_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.joas.ocppui_LS_2ch.R;
import com.joas.ocppui_LS_2ch.UIFlowManager;

public class CreditWaitView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    public CreditWaitView(Context context, UIFlowManager manager, Activity activity){
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_wait_credit_approval,this, true);

//        initComponents();
    }

    @Override
    public void onPageActivate(int chan) {

    }

    @Override
    public void onPageDeactivate() {

    }
}
