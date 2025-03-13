package com.joas.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SftpUploader extends AsyncTask<String, Void, Boolean> {
    private static final String TAG = "SftpUploader";

    SimpleFTPUploadListener simpleFTPUploadListener;

    public SftpUploader(SimpleFTPUploadListener listener) {
        simpleFTPUploadListener = listener;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String sftpUrl = params[0];
        String localFilePath = params[1];
        ChannelSftp channel = null;
        Session session = null;

        try {
            JSch jsch = new JSch();

            String host = "";
            int port = 22;
            String username = "";
            String password = "";
            String path = "";

            String regex = "^sftp://([^:]+):([^/@]+)@([^/]+)(/.*)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(sftpUrl);
            if (matcher.find()) {
                username = matcher.group(1);
                password = matcher.group(2);
                host = matcher.group(3);
                path = matcher.group(4);
            }
//            //todo 테스트 후 삭제
//            username = "cptest";
//            password = "zmffkv4!@3";
//            host = "112.106.138.100";
//            path = "/appsys/sw/ev_sftp/";


            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            Properties props = new Properties();
            props.put("StrictHostKeyChecking", "no");
            session.setConfig(props);

            session.connect();

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            channel.put(localFilePath, path);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "SFTP upload failed", e);
            return false;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            simpleFTPUploadListener.onSimpleFTPUploadFinished();

            LogWrapper.v(TAG, "SFTP upload successful");

        } else {
            String err = "SFTP upload failed";
            simpleFTPUploadListener.onSimpleFTPUploadFileError(err);

            LogWrapper.v(TAG, err);
        }
    }
}
