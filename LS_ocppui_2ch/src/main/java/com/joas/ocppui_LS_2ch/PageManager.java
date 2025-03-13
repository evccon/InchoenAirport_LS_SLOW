/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_LS_2ch;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.joas.ocppui_LS_2ch.page.AdminPasswordInputView;
import com.joas.ocppui_LS_2ch.page.AuthAlarmView;
import com.joas.ocppui_LS_2ch.page.AuthWaitView2;
import com.joas.ocppui_LS_2ch.page.CardTagView;
import com.joas.ocppui_LS_2ch.page.ChargingView;
import com.joas.ocppui_LS_2ch.page.ConnectCarWaitView2;
import com.joas.ocppui_LS_2ch.page.ConnectorWaitView;
import com.joas.ocppui_LS_2ch.page.CreditWaitView;
import com.joas.ocppui_LS_2ch.page.EmergencyBoxView;
import com.joas.ocppui_LS_2ch.page.FaultBoxView;
import com.joas.ocppui_LS_2ch.page.FinishChargingView;
import com.joas.ocppui_LS_2ch.page.JoasCommMonitorView;
import com.joas.ocppui_LS_2ch.page.JoasDSPMonitorView;
import com.joas.ocppui_LS_2ch.page.JoasDebugMsgView;
import com.joas.ocppui_LS_2ch.page.JoasMeterMonitorView;
import com.joas.ocppui_LS_2ch.page.MainCoverView;
import com.joas.ocppui_LS_2ch.page.MessageBoxView;
import com.joas.ocppui_LS_2ch.page.MessageSingleBoxView;
import com.joas.ocppui_LS_2ch.page.PageActivateListener;
import com.joas.ocppui_LS_2ch.page.PageID;
import com.joas.ocppui_LS_2ch.page.PageInputCreditCard;
import com.joas.ocppui_LS_2ch.page.PageUnplugView;
import com.joas.ocppui_LS_2ch.page.SelectChargingOption;
import com.joas.ocppui_LS_2ch.page.SelectPaymentMethod;
import com.joas.ocppui_LS_2ch.page.SelectSlowView;
import com.joas.ocppui_LS_2ch.page.SetChargingOption;
import com.joas.ocppui_LS_2ch.page.SettingView;
import com.joas.ocppui_LS_2ch.page.StopAskBoxView;
import com.joas.ocppui_LS_2ch.page.UnavailableConView;

public class PageManager {
    public static final String TAG = "PageManager";

    Context baseContext;
    MultiChannelUIManager flowManager;

    MainCoverView mainCoverView;
    SelectSlowView selectSlowView;
    CardTagView cardTagView;
    ConnectorWaitView connectorWaitView;
    ChargingView chargingView;
    FinishChargingView finishChargingView;


    EmergencyBoxView emergencyBoxView;
    MessageBoxView messageBoxView;
    FaultBoxView faultBoxView;

    UnavailableConView unavailableConView;

    AuthAlarmView authAlarmView;
    AuthWaitView2 authWaitView2;
    ConnectCarWaitView2 connectCarWaitView2;
    StopAskBoxView stopAskBoxView;
    SelectPaymentMethod selectPaymentMethod;
    SetChargingOption setChargingOption;
    PageInputCreditCard pageInputCreditCard;
    CreditWaitView creditWaitView;
    PageUnplugView pageUnplugView;
    SelectChargingOption selectChargingOption;

    SettingView settingView;
    JoasCommMonitorView commMonitorView;
    JoasMeterMonitorView meterMonitorView;
    JoasDSPMonitorView joasDSPMonitorView;
    JoasDebugMsgView joasDebugMsgView;
    MessageSingleBoxView messageSingleBoxView;
    AdminPasswordInputView adminPasswordInputView;

    PageID prevPageID = PageID.PAGE_END;
    PageID curPageID = PageID.PAGE_END;

    Activity mainActivity;

    FrameLayout frameViewSub;
    CoordinatorLayout MainLayout;
    FrameLayout maincoverview;


    public int channel = 0;

    public PageManager(Context context) {
        baseContext = context;
    }


    public void init(MultiChannelUIManager uiManager, Activity activity) {
        flowManager = uiManager;
        mainActivity = activity;
        initPages();
    }

    void initPages() {
        mainCoverView = new MainCoverView(baseContext,flowManager,mainActivity);

        selectSlowView = new SelectSlowView(baseContext, flowManager, mainActivity);
        cardTagView = new CardTagView(baseContext, flowManager, mainActivity);
        connectorWaitView = new ConnectorWaitView(baseContext, flowManager, mainActivity);
        chargingView = new ChargingView(baseContext, flowManager, mainActivity);
        finishChargingView = new FinishChargingView(baseContext, flowManager, mainActivity);

        emergencyBoxView = new EmergencyBoxView(baseContext, flowManager, mainActivity);
        messageBoxView = new MessageBoxView(baseContext, flowManager, mainActivity);
        faultBoxView = new FaultBoxView(baseContext, flowManager, mainActivity);
        unavailableConView = new UnavailableConView(baseContext, flowManager, mainActivity);
        authAlarmView = new AuthAlarmView(baseContext, flowManager, mainActivity);
        authWaitView2 = new AuthWaitView2(mainActivity, flowManager, mainActivity);
        connectCarWaitView2 = new ConnectCarWaitView2(mainActivity, flowManager, mainActivity);
        stopAskBoxView = new StopAskBoxView(baseContext, flowManager, mainActivity);

        selectPaymentMethod = new SelectPaymentMethod(baseContext, flowManager, mainActivity);
        setChargingOption = new SetChargingOption(baseContext, flowManager, mainActivity);
        pageInputCreditCard = new PageInputCreditCard(baseContext, flowManager, mainActivity);
        pageUnplugView = new PageUnplugView(baseContext, flowManager, mainActivity);
        selectChargingOption = new SelectChargingOption(baseContext, flowManager, mainActivity);

        settingView = new SettingView(mainActivity, flowManager, mainActivity);
        commMonitorView = new JoasCommMonitorView(mainActivity, flowManager, mainActivity);
        meterMonitorView = new JoasMeterMonitorView(mainActivity, flowManager, mainActivity);
        joasDSPMonitorView = new JoasDSPMonitorView(mainActivity, flowManager, mainActivity);
        joasDebugMsgView = new JoasDebugMsgView(mainActivity, flowManager, mainActivity);
        messageSingleBoxView = new MessageSingleBoxView(mainActivity, flowManager, mainActivity);
        adminPasswordInputView = new AdminPasswordInputView(mainActivity, flowManager, mainActivity);

        MainLayout = (CoordinatorLayout)mainActivity.findViewById(R.id.layoutMain);
        frameViewSub = (FrameLayout) mainActivity.findViewById(R.id.viewsub_main);
        maincoverview = (FrameLayout) mainActivity.findViewById(R.id.mainviewframe);

        MainLayout.addView(messageBoxView);
        messageBoxView.setVisibility(View.INVISIBLE);

        MainLayout.addView(faultBoxView);
        faultBoxView.setVisibility(View.INVISIBLE);

        MainLayout.addView(emergencyBoxView);
        emergencyBoxView.setVisibility(View.INVISIBLE);

        MainLayout.addView(unavailableConView);
        unavailableConView.setVisibility(View.INVISIBLE);

        MainLayout.addView(authAlarmView);
        authAlarmView.setVisibility(View.INVISIBLE);

        MainLayout.addView(authWaitView2);
        authWaitView2.setVisibility(View.INVISIBLE);

        MainLayout.addView(connectCarWaitView2);
        connectCarWaitView2.setVisibility(View.INVISIBLE);

        MainLayout.addView(stopAskBoxView);
        stopAskBoxView.setVisibility(View.INVISIBLE);

        MainLayout.addView(settingView);
        settingView.setVisibility(View.INVISIBLE);

        MainLayout.addView(commMonitorView);
        commMonitorView.setVisibility(View.INVISIBLE);

        MainLayout.addView(meterMonitorView);
        meterMonitorView.setVisibility(View.INVISIBLE);

        MainLayout.addView(joasDSPMonitorView);
        joasDSPMonitorView.setVisibility(View.INVISIBLE);

        MainLayout.addView(joasDebugMsgView);
        joasDebugMsgView.setVisibility(View.INVISIBLE);

        MainLayout.addView(messageSingleBoxView);
        messageSingleBoxView.setVisibility(View.INVISIBLE);

        MainLayout.addView(adminPasswordInputView);
        adminPasswordInputView.setVisibility(View.INVISIBLE);

        changePage(PageID.MAIN_COVER,0);
    }



    public SelectSlowView getSelectSlowView() { return selectSlowView; }

    public CardTagView getCardTagView() { return cardTagView; }
    public ConnectorWaitView getConnectorWaitView() { return connectorWaitView; }
    public ChargingView getChargingView() { return chargingView; }
    public FinishChargingView getFinishChargingView() { return finishChargingView; }

    public AuthWaitView2 getAuthWaitView2() { return authWaitView2; }
    public AuthAlarmView getauthAlarmView() {return authAlarmView;}
    public MainCoverView getMainCoverView(){ return mainCoverView;}
    public SelectPaymentMethod getSelectPaymentMethod() { return selectPaymentMethod; }
    public SetChargingOption getSetChargingOption() { return setChargingOption; }
    public PageInputCreditCard getPageInputCreditCard() { return pageInputCreditCard; }
    public CreditWaitView getCreditWaitView() { return creditWaitView; }
    public PageUnplugView getPageUnplugView() { return pageUnplugView; }
    public SelectChargingOption getSelectChargingOption() {return selectChargingOption;}
    public SettingView getSettingView() { return settingView; }
    public JoasDebugMsgView getJoasDebugMsgView() { return joasDebugMsgView; }
    public JoasCommMonitorView getCommMonitorView() { return commMonitorView; }

    public PageID getCurPageID() {return curPageID;}


    public synchronized void doUiChangePage(PageID prev, PageID cur, int chan) {
        channel = chan;
        if ( prev != PageID.PAGE_END) {
            ((PageActivateListener) getPageViewByID(prev)).onPageDeactivate();
            ((View)getPageViewByID(prev)).setVisibility(View.INVISIBLE);
            try {
                if(prev == PageID.MAIN_COVER)
                    maincoverview.removeView((View) getPageViewByID(prev));
                else
                    frameViewSub.removeView((View) getPageViewByID(prev));
            }
            catch (Exception e) {
                Log.e(TAG, "doUIChangePage Remove ex:"+cur.name());
            }
//            Log.v(TAG, "UiChange Deact: ch["+chan+"] "+prev.name());
        }
        if ( cur != PageID.PAGE_END) {
            try {
                if(cur == PageID.MAIN_COVER)
                    maincoverview.addView((View) getPageViewByID(cur));
                else
                    frameViewSub.addView((View) getPageViewByID(cur));
            }catch (Exception e){
                Log.e(TAG, "doUIChangePage Add ex:"+cur.name());
            }
            ((View)getPageViewByID(cur)).setVisibility(View.VISIBLE);
            ((PageActivateListener) getPageViewByID(cur)).onPageActivate(chan);
//            Log.v(TAG, "UiChange Act: ch["+chan+"] "+cur.name());
        }
    }



    /**
     * 페이지를 바꾼다.
     * @param id 바꿀 페이지
     */
    public synchronized void changePage(PageID id, int chan) {
        //Log.d(TAG, "changePage:"+id.name());
        // * 주의점. runOnUiThread의 불려지는 시기가 일정치 않아서 같은 id가 2번 반복수행될 수 있음 따라서
        // 현재 pageID를 바꾸는 작업은 현 Thread안에서 수행되어야함.( prevPageID = curPageID; curPageID = id; )
        if ( curPageID != id ) {
            // Main Thread 체크 하여 동작 수행
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                prevPageID = curPageID;
                curPageID = id;
                doUiChangePage(prevPageID, curPageID,chan);
            } else {
                final PageID idFinal = id;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        prevPageID = curPageID;
                        curPageID = idFinal;
                        doUiChangePage(prevPageID, curPageID,chan);
                    }
                });
            }
        }
    }

    Object getPageViewByID(PageID id) {
        Object v = null;
        switch ( id ) {
            case MAIN_COVER: v = getMainCoverView();
                break;
            case SELECT_SLOW: v = getSelectSlowView();
                break;
            case SELECT_PAYMENT_METHOD: v = getSelectPaymentMethod();
                break;
            case CARD_TAG: v = getCardTagView();
                break;
            case SELECT_CHATGING_OPTION: v = getSelectChargingOption();
                break;
            case SET_CHARING_OPTION: v = getSetChargingOption();
                break;
            case INSERT_CREDIT_CARD: v = getPageInputCreditCard();
                break;
            case CREDIT_APPROVAL_WAIT: v = getCreditWaitView();
                break;
            case CONNECTOR_WAIT: v = getConnectorWaitView();
                break;
            case CHARGING: v = getChargingView();
                break;
            case FINISH_CHARGING: v = getFinishChargingView();
                break;
            case UNPLUG: v = getPageUnplugView();
                break;
        }
        return v;
    }

    public void changePreviousPage() {
        changePage(prevPageID,channel);
    }

    public void showEmergencyBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                emergencyBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideEmergencyBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                emergencyBoxView.setVisibility(View.INVISIBLE);
            }
        });
    }


    public void showMessageBox(int chan) {
        channel = chan;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideMessageBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageBoxView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showAuthalarmBox(int chan) {
        channel = chan;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                authAlarmView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideAuthalarmBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                authAlarmView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showFaultBox(int chan) {
        channel = chan;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                flowManager.getUIFlowManager(channel).fillFaultMessage();
                faultBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void showFaultBox2(int chan) {
        channel = chan;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faultBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideFaultBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faultBoxView.setVisibility(View.INVISIBLE);
            }
        });
    }


    public void showUnavailableConView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                unavailableConView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideUnavailableConView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                unavailableConView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showAuthWaitView(int chan) {
        channel = chan;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                authWaitView2.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideAuthWaitView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                authWaitView2.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showConwaitBox(int chan) {
        channel = chan;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectCarWaitView2.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideConwaitBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectCarWaitView2.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showStopAskBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopAskBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideStopAskBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopAskBoxView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void refreshFaultBox(){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faultBoxView.initMessageBox();
            }
        });
    }
    public void showSettingView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                settingView.onPageActivate(0);
                settingView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideSettingView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                settingView.setVisibility(View.INVISIBLE);
                settingView.onPageDeactivate();
            }
        });
    }
    public void showJoasCommMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                commMonitorView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void showJoasMeterMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                meterMonitorView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void showJoasDspMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDSPMonitorView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideJoasDspMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDSPMonitorView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public void showJoasDebugView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDebugMsgView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideJoasDebugView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDebugMsgView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public void showSingleMessageBoxView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageSingleBoxView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideSingleMessageBoxView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageSingleBoxView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public void showAdminPasswrodInputView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adminPasswordInputView.setVisibility(View.VISIBLE);
            }
        });
    }
    public boolean isShowAdminPassswordInputView() {
        return adminPasswordInputView.getVisibility() == View.VISIBLE;
    }
    public void hideAdminPasswrodInputView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adminPasswordInputView.setVisibility(View.INVISIBLE);
            }
        });
    }
}
