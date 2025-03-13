/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch.page;

/**
 * Created by user on 2018-01-08.
 */

public interface PageActivateListener {
    public void onPageActivate(int channel);
    public void onPageDeactivate();
}
