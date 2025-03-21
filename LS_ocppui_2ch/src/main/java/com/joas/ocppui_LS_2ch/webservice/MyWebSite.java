/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_LS_2ch.webservice;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yanzhenjie.andserver.error.NotFoundException;
import com.yanzhenjie.andserver.framework.body.StreamBody;
import com.yanzhenjie.andserver.framework.website.BasicWebsite;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.util.Assert;
import com.yanzhenjie.andserver.util.DigestUtils;
import com.yanzhenjie.andserver.util.MediaType;
import com.yanzhenjie.andserver.util.Patterns;
import com.yanzhenjie.andserver.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Zhenjie Yan on 2018/9/7.
 */
public class MyWebSite extends BasicWebsite implements Patterns {

    private com.yanzhenjie.andserver.framework.website.AssetsWebsite.AssetsReader mReader = null;
    private String mRootPath = null;;
    private Map<String, String> mPatternMap = null;;
    private PackageInfo mPackageInfo = null;;

    private boolean isScanned = false;

    /**
     * Create a website object.
     *
     * @param rootPath website root directory.
     */
    public MyWebSite(@NonNull Context context, @NonNull String rootPath) {
        this(context, rootPath, DEFAULT_INDEX);
    }

    /**
     * Create a website object.
     *
     * @param rootPath website root directory.
     * @param indexFileName the default file name for each directory, e.g. index.html.
     */
    public MyWebSite(@NonNull Context context, @NonNull String rootPath, @NonNull String indexFileName) {
        super(indexFileName);
        Assert.isTrue(!StringUtils.isEmpty(rootPath), "The rootPath cannot be empty.");
        Assert.isTrue(!StringUtils.isEmpty(indexFileName), "The indexFileName cannot be empty.");

        if (!rootPath.matches(PATH)) {
            String message = String.format("The format of [%s] is wrong, it should be like [/root/project].", rootPath);
            throw new IllegalArgumentException(message);
        }

        this.mReader = new com.yanzhenjie.andserver.framework.website.AssetsWebsite.AssetsReader(context.getAssets());
        this.mRootPath = trimStartSlash(rootPath);
        this.mPatternMap = new HashMap<>();

        PackageManager packageManager = context.getPackageManager();
        try {
            mPackageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (Exception ignored) {
            throw new RuntimeException(ignored);
        }
    }

    @Override
    public boolean intercept(@NonNull HttpRequest request) {
        tryScanFile();

        String httpPath = request.getPath();
        return mPatternMap.containsKey(httpPath);
    }

    /**
     * Try to scan the file, no longer scan if it has already been scanned.
     */
    private void tryScanFile() {
        if (!isScanned) {
            synchronized (com.yanzhenjie.andserver.framework.website.AssetsWebsite.class) {
                if (!isScanned) {
                    List<String> fileList = mReader.scanFile(mRootPath);
                    for (String filePath : fileList) {
                        String httpPath = filePath.substring(mRootPath.length(), filePath.length());
                        httpPath = addStartSlash(httpPath);
                        mPatternMap.put(httpPath, filePath);

                        String indexFileName = getIndexFileName();
                        if (filePath.endsWith(indexFileName)) {
                            httpPath = filePath.substring(0, filePath.indexOf(indexFileName) - 1);
                            httpPath = addStartSlash(httpPath);
                            mPatternMap.put(httpPath, filePath);
                            mPatternMap.put("/", filePath); // 기존 버그 코그 수정
                        }
                    }
                    isScanned = true;
                }
            }
        }
    }

    @Override
    public String getETag(@NonNull HttpRequest request) throws IOException {
        String httpPath = request.getPath();
        String filePath = mPatternMap.get(httpPath);
        final InputStream stream = mReader.getInputStream(filePath);
        if (stream != null) {
            return DigestUtils.md5DigestAsHex(stream);
        }
        throw new NotFoundException(httpPath);
    }

    @Override
    public long getLastModified(@NonNull HttpRequest request) throws IOException {
        String filePath = mPatternMap.get(request.getPath());
        return mReader.isFile(filePath) ? mPackageInfo.lastUpdateTime : -1;
    }

    @NonNull
    @Override
    public ResponseBody getBody(@NonNull HttpRequest request) throws IOException {
        String httpPath = request.getPath();
        String filePath = mPatternMap.get(httpPath);
        final InputStream stream = mReader.getInputStream(filePath);
        if (stream == null) {
            throw new NotFoundException(httpPath);
        }
        final MediaType mediaType = MediaType.getFileMediaType(filePath);
        return new StreamBody(stream, stream.available(), mediaType);
    }

    public static class AssetsReader {

        /**
         * {@link AssetManager}.
         */
        private AssetManager mAssetManager;

        /**
         * Create {@link com.yanzhenjie.andserver.framework.website.AssetsWebsite.AssetsReader}.
         *
         * @param manager {@link AssetManager}.
         */
        public AssetsReader(@NonNull AssetManager manager) {
            this.mAssetManager = manager;
        }

        /**
         * Get stream file.
         *
         * @param filePath assets in the absolute path.
         *
         * @return {@link InputStream} or null.
         */
        @Nullable
        public InputStream getInputStream(@NonNull String filePath) {
            try {
                return mAssetManager.open(filePath);
            } catch (Throwable ignored) {
                return null;
            }
        }

        /**
         * Specify whether the destination is a file.
         *
         * @param fileName assets in the absolute path.
         *
         * @return true, other wise is false.
         */
        public boolean isFile(@NonNull String fileName) {
            return getInputStream(fileName) != null;
        }

        /**
         * Scanning subFolders and files under the specified path.
         *
         * @param path the specified path.
         *
         * @return String[] Array of strings, one for each asset. May be null.
         */
        @NonNull
        public List<String> list(@NonNull String path) {
            List<String> fileList = new ArrayList<>();
            try {
                String[] files = mAssetManager.list(path);
                Collections.addAll(fileList, files);
            } catch (Throwable ignored) {
            }
            return fileList;
        }

        /**
         * Scan all files in the inPath.
         *
         * @param path path in the path.
         *
         * @return under inPath absolute path.
         */
        @NonNull
        public List<String> scanFile(@NonNull String path) {
            Assert.isTrue(!StringUtils.isEmpty(path), "The path cannot be empty.");

            List<String> pathList = new ArrayList<>();
            if (isFile(path)) {
                pathList.add(path);
            } else {
                List<String> files = list(path);
                for (String file : files) {
                    String realPath = path + File.separator + file;
                    if (isFile(realPath)) {
                        pathList.add(realPath);
                    } else {
                        List<String> childList = scanFile(realPath);
                        if (childList.size() > 0) pathList.addAll(childList);
                    }
                }
            }
            return pathList;
        }
    }
}