package com.sunstone.model;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;

public class HeaderDb {

    public static void fillHeaderDB(ArrayList<HeaderConfigData> headerData){

        HeaderConfigData securityEnabled = new HeaderConfigData("Security enabled", "Indicates an encrypted packet.", false, 3);
        headerData.add(securityEnabled);

        HeaderConfigData framePending = new HeaderConfigData("Frame pending", "The next packet will extend the current one.", false, 4);
        headerData.add(framePending);

        HeaderConfigData ackRequest = new HeaderConfigData("ACK request", "The recipient shall send an ACK in the same time slot.", false, 5);
        headerData.add(ackRequest);

        HeaderConfigData bytesToBoundry6 = new HeaderConfigData("Bytes to boundary", "If security is enabled, these bits indicate the number of random filled bytes at\n" +
                "the end of the packet to meet the security block boundary (15 bytes maximum).", false, 6);
        headerData.add(bytesToBoundry6);

        HeaderConfigData bytesToBoundry7 = new HeaderConfigData("Bytes to boundary", "If security is enabled, these bits indicate the number of random filled bytes at\n" +
                "the end of the packet to meet the security block boundary (15 bytes maximum).", false, 7);
        headerData.add(bytesToBoundry7);

        HeaderConfigData bytesToBoundry8 = new HeaderConfigData("Bytes to boundary", "If security is enabled, these bits indicate the number of random filled bytes at\n" +
                "the end of the packet to meet the security block boundary (15 bytes maximum).", false, 8);
        headerData.add(bytesToBoundry8);

        HeaderConfigData bytesToBoundry9 = new HeaderConfigData("Bytes to boundary", "If security is enabled, these bits indicate the number of random filled bytes at\n" +
                "the end of the packet to meet the security block boundary (15 bytes maximum).", false, 9);
        headerData.add(bytesToBoundry9);

        HeaderConfigData reserved10 = new HeaderConfigData("Reserved", "Should be set.", false, 10);
        headerData.add(reserved10);

        HeaderConfigData routedPacket = new HeaderConfigData("Routed packet", "If the packet was forwarded (see routing request), this bit should be set.", false, 11);
        headerData.add(routedPacket);

        HeaderConfigData frameVersion12 = new HeaderConfigData("Frame version", "Should be set to indicate LoLaN frame.", false, 12);
        headerData.add(frameVersion12);

        HeaderConfigData frameVersion13 = new HeaderConfigData("Frame version", "Should be set to indicate LoLaN frame.", false, 13);
        headerData.add(frameVersion13);

        HeaderConfigData reserved14 = new HeaderConfigData("Reserved", "Should be set.", false, 14);
        headerData.add(reserved14);

        HeaderConfigData routingRequest = new HeaderConfigData("Routing request", "If this bit is set, the receiver device should forward this packet if it is addressed\n" +
                "to an other device.", false, 15);
        headerData.add(routingRequest);
    }

    public static void setDbValues(String header, ArrayList<HeaderConfigData> headerData){
        char[] headerCharArray = header.toCharArray();

        for (int i=0; i<headerCharArray.length; i++){
            if (headerCharArray[i] == "1".charAt(0)){
                headerData.get(i).setChecked(true);
            } else {
                headerData.get(i).setChecked(false);
            }
        }

    }

}
