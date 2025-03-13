/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_LS_2ch;

public class FaultInfo {
    public int id = 0;
    public int errorCode = 0;
    public String errorMsg = "";
    public boolean isRepair = false;

    public FaultInfo() {

    }

    public FaultInfo(int faultId, int code, String msg, boolean repair) {
        this.id = faultId;
        this.errorCode = code;
        this.errorMsg = msg;
        this.isRepair = repair;
    }
}
