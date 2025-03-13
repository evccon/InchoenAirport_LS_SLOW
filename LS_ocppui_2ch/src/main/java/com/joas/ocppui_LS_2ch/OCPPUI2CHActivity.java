/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_LS_2ch;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import com.joas.metercertviewer.*;
import com.joas.metercertviewer.IMeterAidlInterface;
import com.joas.ocppui_LS_2ch.page.PageEvent;
import com.joas.ocppui_LS_2ch.webservice.WebService;
import com.joas.utils.LogWrapper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class OCPPUI2CHActivity extends ImmersiveAppCompatActivity {
    public static final String uiVersion = "v0.1";
    static OCPPUI2CHActivity mainActivity = null;

    PageManager pageManager;
    MultiChannelUIManager multiChannelUIManager;
    WebService webService;

    public int adminCount = 0;
    Timer timer;
    static Toast toast;

    /**
     * 저장용 Config UI 정의
     */
    CPConfig cpConfig;
    ChargeData[] chargeDatas = new ChargeData[TypeDefine.MAX_CHANNEL];
    TextView tvRemoteStartMsg;
    TextView tvReservedMsg;
    ImageView imageCommStatus;
    MeterConfig meterConfig;


    boolean commConnStatus = false;

    private IMeterAidlInterface meterService;
    Handler bindingMeterHandler;

    Button homeButton;
    Button backButton;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogWrapper.d("MeterAidl", " Service Connected!!");
            meterService = IMeterAidlInterface.Stub.asInterface(iBinder);
            try {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

                Log.d("DeviceInformation", "display => size: " + metrics.widthPixels + "x" + metrics.heightPixels + " density: " + metrics.densityDpi);

                meterService.startApp(1);
                if (metrics.densityDpi <= 120) {
                    // 4.3inch ldpi
                    meterService.startAppNewPos(1, 0, metrics.heightPixels - 20, metrics.widthPixels, 20, 23, 0xFF5D6C80, 0xFFFFFFFF);
                } else {
                    // 8inch mdpi
                    meterService.startAppNewPos(1, 0, metrics.heightPixels - 22, metrics.widthPixels, 22, 15, 0xFF5D6C80, 0xFFFFFFFF);
                }

//                Log.d("deviceInformation", "display => size.x : " + size.x + ", size.y : " + size.y);

            } catch (Exception e) {
                LogWrapper.d("MeterAidl", "error:" + e.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogWrapper.d("MeterAidl", " Service Disconnected!!");
            meterService = null;

            unbindService(serviceConnection);
            startBindConnect();
        }
    };

    public void startBindConnect() {
        bindingMeterHandler = new Handler(Looper.getMainLooper());
        final Handler runHandler = bindingMeterHandler;
        bindingMeterHandler.postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent();
                intent.setClassName("com.joas.metercertviewer", "com.joas.metercertviewer.MeterWindow");

                if (bindService(intent, serviceConnection, BIND_AUTO_CREATE) == false) {
                    LogWrapper.d("IOTVideoAct", "Meter Bind Error");
                    bindingMeterHandler.postDelayed(this, 1000);
                } else {
                    LogWrapper.d("IOTVideoAct", "Meter Bind Success!");
                }
            }
        }, 1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mainActivity = this;

        AlarmManager am = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);
        am.setTimeZone("UTC");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Force Close 이벤트 발생시 처리 (개발시에는 끄고/ 릴리즈시에 적용)
        /*
        Thread.setDefaultUncaughtExceptionHandler(
                new ForceCloseHandler(this,
                        Environment.getExternalStorageDirectory()+TypeDefine.FORCE_CLOSE_LOG_PATH,
                        true));
                        */

        hideNavBar();

        cpConfig = new CPConfig();
        // 맨먼저 설정값을 로딩한다.
        cpConfig.loadConfig(this);

        setContentView(R.layout.activity_ocpp_main_landscape);


        pageManager = new PageManager(this);

        for (int i = 0; i < TypeDefine.MAX_CHANNEL; i++) {
            chargeDatas[i] = new ChargeData();
            chargeDatas[i].dspChannel = i;
            chargeDatas[i].curConnectorId = i + 1;
        }

        meterConfig = new MeterConfig();
        meterConfig.loadConfig(this);

        String restartReason = getIntent().getStringExtra("RestartReason");

        multiChannelUIManager = new MultiChannelUIManager(this, pageManager, chargeDatas, cpConfig, meterConfig, restartReason);

        initComponents();

        setNavigationBarHomeIconDisable(true);

        startBindConnect();

        webService = new WebService(this.getApplicationContext());
    }

    static public OCPPUI2CHActivity getMainActivity() {
        return mainActivity;
    }

    public MultiChannelUIManager getMultiChannelUIManager() {
        return multiChannelUIManager;
    }

    public IMeterAidlInterface getMeterService() {
        return meterService;
    }

    public boolean getIsCommConnected() {
        return commConnStatus;
    }

    void initComponents() {
        TextView tvVer = (TextView) findViewById(R.id.tvContentView);
        tvVer.setText("" + TypeDefine.SW_VER);

        TextView tvCPID = findViewById(R.id.tvCPID);
        tvCPID.setText(cpConfig.chargerID);

        ImageView imgLogo = findViewById(R.id.imglogo);
        imgLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

        tvRemoteStartMsg = (TextView) findViewById(R.id.tvRemoteStartedMsg);
        tvRemoteStartMsg.setVisibility(View.INVISIBLE);
        tvReservedMsg = (TextView) findViewById(R.id.tvReservedMsg);
        tvReservedMsg.setVisibility(View.INVISIBLE);
        imageCommStatus = (ImageView) findViewById(R.id.imageCommStatus);
        imageCommStatus.setVisibility(View.INVISIBLE);

        homeButton = (Button) findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                multiChannelUIManager.getUIFlowManager(pageManager.channel).onPageCommonEvent(PageEvent.GO_HOME);

            }
        });
        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                multiChannelUIManager.getUIFlowManager(pageManager.channel).onPageCommonEvent(PageEvent.GO_BACK);
            }
        });
    }

    void onHomeClick() {
        adminCount++;
        if (adminCount > 6) {
            onCheckAdminMode();
        } else if (adminCount > 3) {
            if (toast != null) toast.cancel();
            toast = Toast.makeText(this, "Admin Mode Remains : " + (6 - adminCount), Toast.LENGTH_SHORT);
            toast.show();
        }
        startTimer();
    }

    void startTimer() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                adminCount = 0;
                timer.cancel();
            }
        }, 1000, 1000);
    }

    public void stopTimer() {
        try {
            if (timer != null) timer.cancel();
        } catch (Exception e) {
        }
    }

    public void setRemoteStartedVisible(int v) {
        final int visible = v;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (visible == View.VISIBLE)
                    tvRemoteStartMsg.setText(getResources().getString(R.string.string_remote_started));
                tvRemoteStartMsg.setVisibility(visible);
            }
        });
    }

    public void setReservedVisible(int v) {
        final int visible = v;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (visible == View.VISIBLE)
                    tvReservedMsg.setText(getResources().getString(R.string.string_reserved));
                tvReservedMsg.setVisibility(visible);
            }
        });
    }

    public void setCommConnStatus(boolean status) {
        commConnStatus = status;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshCommConStatus();
            }
        });
    }

    public void setCommConnActive() {
        commConnStatus = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageCommStatus.setImageResource(R.drawable.commicon_active);
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshCommConStatus();
                            }
                        });
                    }
                }, 300);
            }
        });
    }

    public void refreshCommConStatus() {
        if (commConnStatus) imageCommStatus.setImageResource(R.drawable.commicon);
        else imageCommStatus.setImageResource(R.drawable.commicon_fail);
    }

    public void onCheckAdminMode() {
        multiChannelUIManager.getPageManager().showAdminPasswrodInputView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        setNavigationBarHomeIconDisable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setNavigationBarHomeIconDisable(true);
    }

    @Override
    protected void onDestroy() {
        multiChannelUIManager.destoryManager();
        stopTimer();
        setNavigationBarHomeIconDisable(false);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // 마우스 오른쪽 버턴을 누를때 종료.
    @Override
    public void onBackPressed() {
        if (multiChannelUIManager.getPageManager().isShowAdminPassswordInputView())
            multiChannelUIManager.onFinishApp();
        else multiChannelUIManager.getPageManager().showAdminPasswrodInputView();
    }

    public void hideNavBar() {
        int currentApiVersion = Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }
    }

    public void setNavigationBarHomeIconDisable(boolean tf) {
        try {

            Process p = Runtime.getRuntime().exec("su");
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes("setprop persist.sys.navbar.disable " + (tf ? "true" : "false") + "\n");

            os.writeBytes("exit\n");
            os.flush();

            int read;
            byte[] buffer = new byte[4096];
            String output = new String();
            while ((read = es.read(buffer)) > 0) {
                output += new String(buffer, 0, read);
            }

            Log.e("SetProp", "Output:" + output);

            p.waitFor();
        } catch (IOException e) {
            Log.e("SetProp", e.toString());
        } catch (InterruptedException e) {
            Log.e("SetProp", e.toString());
        }
    }
}
