/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch.page;

public enum PageID {
    MAIN_COVER(0),
    SELECT_SLOW(1),
    SELECT_FAST(2),
    SELECT_PAYMENT_METHOD(3),
    CARD_TAG(4),
    SELECT_CHATGING_OPTION(5),
    SET_CHARING_OPTION(6),
    INSERT_CREDIT_CARD(7),
    CREDIT_APPROVAL_WAIT(8),
    CONNECTOR_WAIT(9),
    CHARGING(10),
    FINISH_CHARGING(11),
    UNPLUG(12),
    PAGE_END(13);

    int id;
    private PageID(int id) { this.id = id; }
    public int getID() { return id;}
    public boolean Compare(int i){return id == i;}
    public static PageID getValue(int _id)
    {
        PageID[] As = PageID.values();
        for(int i = 0; i < As.length; i++)
        {
            if(As[i].Compare(_id))
                return As[i];
        }
        return PageID.PAGE_END;
    }
}
