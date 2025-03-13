package com.joas.smartcharger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.joas.utils.LogWrapper;

import java.io.DataOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    public static final String ACTIVITY_CERT_TEST_UI = "com.joas.certtest.CertTestActivity";
    public static final String ACTIVITY_OCPP_1CH_UI = "com.joas.ocppui_1ch.OCPPUI1CHActivity";
    public static final String ACTIVITY_OCPP_2CH_UI = "com.joas.ocppui_LS_2ch.OCPPUI2CHActivity";
    public static final String COMBINE_UI = "com.joas.combine_UI.OCPPUI2CHActivity";

    Button btStartApp;
    Button btRebootSystem;
    Button btExitApp;
    Button btCertTest;
    Button btRotatePortrait;
    Button btRotateLandscape;

    TextView tvWaitLoadingMsg;

    HomeWatcher mHomeWatcher;

    /**
     * 시작할 어플리케이션을 컴파일시에 정한다. Module App의 Dependency에 해당 모듈에 포함되어 있어야 함!
     */


//    String callActivity = ACTIVITY_OCPP_1CH_UI;
//    String callActivity = ACTIVITY_OCPP_2CH_UI;
    String callActivity = ACTIVITY_OCPP_2CH_UI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogWrapper.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

         getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        startMeterApp();

        hideNavBar();

        btStartApp = (Button) findViewById(R.id.btStartChargerApp);
        btRebootSystem = (Button) findViewById(R.id.btRebootSystem);
        btExitApp = (Button) findViewById(R.id.btExitApp);
        btCertTest = (Button) findViewById(R.id.btCertTest);
        btRotatePortrait = findViewById(R.id.btRotatePortrait);
        btRotateLandscape = findViewById(R.id.btRotateLandscape);

        tvWaitLoadingMsg = (TextView) findViewById(R.id.tvWaitLoadingMsg);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mainAppCall();
                    }
                }, 500);

            }
        });

        homeWatcherInit();

        killEsExplorer();
    }

    //Android 11는 app에서 미터 프로그램 실행시킴
    void startMeterApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.joas.metercertviewer");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }

    void killEsExplorer() {
        final String commands = "am force-stop com.estrongs.android.pop";
        try {

            Process p = Runtime.getRuntime().exec("su");
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes(commands + "\n");

            os.writeBytes("exit\n");
            os.flush();

            int read;
            byte[] buffer = new byte[4096];
            String output = new String();
            while ((read = es.read(buffer)) > 0) {
                output += new String(buffer, 0, read);
            }

            p.waitFor();
        } catch (Exception e) {

        }
    }

    void homeWatcherInit() {
        mHomeWatcher = new HomeWatcher(this);
        final Context context = this;
        mHomeWatcher.setOnHomePressedListener(new HomeWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                //만약 정상적으로 종료가 되지 않고 홉버튼등으로 종료가 되는경우 강제 리부팅을 시도한다.
                Toast.makeText(context, "Abnormal situation.. Reboot", Toast.LENGTH_LONG).show();
                Log.e("homeWatcher", "onHomePressed");
                rebootSystem();
            }

            @Override
            public void onHomeLongPressed() {
                Toast.makeText(context, "Abnormal situation.. Reboot", Toast.LENGTH_LONG).show();
                Log.e("homeWatcher", "onHomeLongPressed");
                rebootSystem();
            }
        });
        mHomeWatcher.startWatch();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(TAG, "onConfigurationChanged: " + newConfig);
        super.onConfigurationChanged(newConfig);
    }

    void mainAppCall() {
        String restartReason = getIntent().getStringExtra("RestartReason");

        try {
//            Intent myIntent = new Intent(this, Class.forName(callActivity));
//            myIntent.putExtra("RestartReason", restartReason);
//            startActivity(myIntent);

            startActivity(new Intent(this, Class.forName(callActivity))
                    .putExtra("Restartreason", restartReason)
                    .setAction(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Log.e("MainAct", callActivity + " Not Found!!!");

            Toast.makeText(this, callActivity + " Not Found!!!", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickExitApp(View v) {
        restoreNavBarStateAndFinish();
    }

    public void onCertTest(View v) {
        try {
            Intent myIntent = new Intent(this, Class.forName(ACTIVITY_CERT_TEST_UI));
            startActivity(myIntent);
        } catch (Exception e) {
        }
    }

    public void onClickStartCharger(View v) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mainAppCall();
                    }
                }, 500);

            }
        });
    }

    public void onRotatePortrait(View v) {
        Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
        Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, 1);
    }

    public void onRotateLandscape(View v) {
        Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
        Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, 0);
    }

    public void restoreNavBarStateAndFinish() {
        // 숨겨진 네비게이션 바의 아이콘을 다시 살리기 위하여 한번 키보드를 나타내게 한다.
        final EditText editTextDummy = (EditText) findViewById(R.id.editTextDummy);
        new Handler().post(new Runnable() { // new Handler and Runnable
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editTextDummy, InputMethodManager.SHOW_FORCED);
            }
        });

        mHomeWatcher.stopWatch();

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(editTextDummy.getWindowToken(), 0);
                        finish();
                    }
                }, 500);

            }
        });
    }

    public void rebootSystem() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.reboot("force");
        } catch (Exception e) {
        }
    }

    public void onClickRebootSystem(View v) {
        rebootSystem();
    }

    // 마우스 오른쪽 버턴을 누를때 종료.
    @Override
    public void onBackPressed() {
        restoreNavBarStateAndFinish();
    }

    @Override
    protected void onDestroy() {
        LogWrapper.d(TAG, "onDestroy");

        super.onDestroy();
        //android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0); // 프로세서를 죽인다.
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

//        btStartApp.setVisibility(View.VISIBLE);
        btRebootSystem.setVisibility(View.VISIBLE);
        btExitApp.setVisibility(View.VISIBLE);
//        btCertTest.setVisibility(View.VISIBLE);
        //btRotatePortrait.setVisibility(View.VISIBLE);
        //btRotateLandscape.setVisibility(View.VISIBLE);
        tvWaitLoadingMsg.setVisibility(View.INVISIBLE);
    }

    public void hideNavBar() {
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

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
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility(flags);
                    }
                }
            });
        }
    }
}
