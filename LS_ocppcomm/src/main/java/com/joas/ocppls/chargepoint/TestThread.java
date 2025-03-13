package com.joas.ocppls.chargepoint;

import com.joas.ocppls.test.OCPPStackTest;
import com.joas.ocppls.test.TransceiverTest;
import com.joas.ocppls.test.TransportTest;

/**
 * Created by user on 2017-05-31.
 */

public class TestThread extends Thread {
    OCPPStackTest testStack;
    public TestThread() {

    }

    @Override
    public void run() {
        testOcppStack();
    }

    public void testTransport() {
        TransportTest test = new TransportTest();
        test.testTransportWebsocket();
    }

    public void testTransceiver() {
        TransceiverTest test = new TransceiverTest();
        test.testTransceiverWAMP();
    }

    public void testOcppStack() {
        testStack = new OCPPStackTest();
        testStack.testOCPPStack();
    }

    public void testOcppStackAuthTest() {
        testStack.testAuth();
    }

}
