package com.joas.hw.fwUpdater;

public interface FWUpdaterListener {
    void onFWUpdateComplete();
    void onFWUpdateStart();
    void onFWUpdateFailed();

}
