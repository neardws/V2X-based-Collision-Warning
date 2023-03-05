package com.linmu.collision_warning_system.utils;

import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IpUtil {
    public static String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface
                    .getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAddress = netI
                        .getInetAddresses(); enumIpAddress.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddress.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        String hostIp = inetAddress.getHostAddress();
                        Log.d("GetIp", "手机IP地址get the IpAddress--> " + hostIp);
                        return hostIp;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Log.e("GetIp", "没有获取到IP地址");
        return null;
    }
}
