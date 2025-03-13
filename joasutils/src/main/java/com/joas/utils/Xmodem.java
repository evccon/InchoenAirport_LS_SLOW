package com.joas.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Xmodem {
    public static final String TAG = "Xmodem";

    private static final byte SOH = 0x01; // Start of Header
    private static final byte STX = 0x02; // Start of Text
    private static final byte EOT = 0x04; // End of Transmission
    private static final byte ACK = 0x06; // Acknowledge
    private static final byte NAK = 0x15; // Negative Acknowledge
    private static final byte CAN = 0x18; // Cancel
    private static final int POLYNOMIAL = 0x1021;

    private InputStream inputStream;
    private OutputStream outputStream;

    private SerialPort mSerialPort;

    public Xmodem(String serialDev) {
        try {
            mSerialPort = new SerialPort(new File(serialDev), 115200, 0);

            if (mSerialPort == null) {
                LogWrapper.v(TAG, "Xmodem SerialPort Open Fail!");
                return;
            }

            inputStream = mSerialPort.getInputStream();
            outputStream = mSerialPort.getOutputStream();

        } catch (IOException e) {
            LogWrapper.v(TAG, "Xmodem SerialPort Fail :" + e);
        }

    }

    public boolean send(byte[] data) {
        try {
            outputStream.write(0x43);
            outputStream.flush();

            // Wait for the receiver's response
            byte[] responseBuffer = new byte[20];
            if (inputStream.read(responseBuffer) == -1) {
                return false;
            }
            int response = responseBuffer[0];
            if (response == 0x43) {
                byte packetNumber = 1;
                int offset = 0;
                while (offset < data.length) {
                    // Send a packet with a header and data
                    ByteBuffer packetBuffer = ByteBuffer.allocate(1030);
                    packetBuffer.put(STX);
                    packetBuffer.put(packetNumber);
                    packetBuffer.put((byte) ~packetNumber);
                    byte[] packetData = Arrays.copyOfRange(data, offset, offset + 1024);
                    packetBuffer.put(packetData);

                    int padding = 1024 - packetData.length;
                    for (int i = 0; i < padding; i++) {
                        packetBuffer.put((byte) 0x1A); // Padding
                    }
                    packetBuffer.put((byte[]) calculate(packetData));

                    outputStream.write(packetBuffer.array()); // CRC data
                    outputStream.flush();


                    responseBuffer = new byte[20];
                    if (inputStream.read(responseBuffer) == -1) {
                        return false;
                    }
                    response = responseBuffer[0];
                    if (response == ACK) {
                        // ACK received, move on to the next packet
                        packetNumber++;
                        offset += 1024;
                        continue;
                    } else if (response == NAK) {
                        packetNumber++;
                        offset += 1024;
                        continue;
                    } else {
                        // Unexpected response received, abort transmission
                        return false;
                    }
                }
                // Send an EOT to signal the end of transmission
                outputStream.write(EOT);
                int eotResponse = inputStream.read();
                if (eotResponse != ACK) {
                    // Failed to receive ACK after EOT, transmission failed
                    return false;
                }
                return true;
            }

        } catch (IOException e) {
            // IO error occurred, transmission failed
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public static byte[] calculate(byte[] data) {
        int crc = 0;

        for (byte b : data) {
            crc = crc ^ ((b & 0xFF) << 8);

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ POLYNOMIAL;
                } else {
                    crc <<= 1;
                }
            }
        }

        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((crc >> 8) & 0xFF);
        bytes[1] = (byte) (crc & 0xFF);
        return bytes;
    }

    public int receive(byte[] buffer) {
        // Implement Xmodem receive logic here
        return 0;
    }

    public void stopSerialPort() {
        if (mSerialPort != null) mSerialPort.close();
    }


}
