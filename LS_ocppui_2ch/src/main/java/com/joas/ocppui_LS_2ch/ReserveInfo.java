/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_LS_2ch;

import java.util.Calendar;

public class ReserveInfo {
    public int reservationId = 0;
    public String idTag = "";
    public String parentIdTag = "";
    public Calendar expiryDate = null;

    public void setInfo(int id, String idTag, String parent, Calendar expiry) {
        this.reservationId = id;
        this.idTag = idTag;
        this.parentIdTag = parent;
        this.expiryDate = expiry;
    }

    public void init() {
        int reservationId = 0;
        String idTag = "";
        String parentIdTag = "";
        Calendar expiryDate = null;
    }

    public boolean expiryCheck() {
        boolean ret = false;
        if ( expiryDate != null ) {
            Calendar curTime = Calendar.getInstance();
            if (curTime.getTimeInMillis() > expiryDate.getTimeInMillis()) {
                ret = true;
                init();
            }
        }
        return ret;
    }
}
