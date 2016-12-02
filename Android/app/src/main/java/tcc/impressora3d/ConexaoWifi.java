package tcc.impressora3d;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by nanio on 11/11/16.
 */

public class ConexaoWifi {

    InetAddress ipImpressora = null;
    Socket socket = null;

    BufferedWriter out;
    BufferedReader in;

    static ConexaoWifi conectar()
    {
        try{
            ConexaoWifi ret = new ConexaoWifi();
            String messageStr="CEP2";
            int server_port = 13000;
            DatagramSocket s = new DatagramSocket();
            InetAddress local = InetAddress.getByName("255.255.255.255");
            int msg_length=messageStr.length();
            byte[] message = messageStr.getBytes();
            DatagramPacket p = new DatagramPacket(message, msg_length,local,server_port);
            s.send(p);

            byte[] messbck = new byte[1500];
            DatagramPacket r = new DatagramPacket(messbck, messbck.length);

            s.setSoTimeout(1500);
            s.receive(r);

            ret.ipImpressora = InetAddress.getByName(new String(messbck).trim());
            ret.socket = new Socket(ret.ipImpressora, 13130);

            ret.out = new BufferedWriter(new OutputStreamWriter(ret.socket.getOutputStream()));
            ret.in = new BufferedReader(new InputStreamReader(ret.socket.getInputStream()));

            ret.send("CEP5");

            return ret;
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    boolean isAtivo()
    {
        return (socket != null && socket.isConnected());
    }

    String receive()
    {
        try
        {
            if(!in.ready()) return null;
            String ret = "";
            while(in.ready())
            {
                ret += (char)in.read();
            }
            return ret;
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    boolean isPending()
    {
        try
        {
            return in.ready();
        }catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    void send(String s)
    {
        try
        {
            out.write(s, 0, s.length());
            out.flush();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}