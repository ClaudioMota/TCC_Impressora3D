package tcc.impressora3d;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static tcc.impressora3d.R.id.textView;

/**
 * Created by nanio on 11/11/16.
 */

//Baseado em: http://www.allaboutcircuits.com/projects/communicate-with-your-arduino-through-android/

public class DefinirRede extends Activity {

    public static final int CANCELADO = 0;
    public static final int OK = 1;
    public static final int ERRO = 2;
    public static final int DESCONECTADO = 3;

    public static final String PERMISSAO_USB = "tcc.impressora3d.USB_PERMISSION";

    UsbManager manager;
    UsbDevice dispositivo;
    UsbDeviceConnection conexao;
    UsbSerialDevice portaSerial = null;

    EditText edssid;
    EditText edsenha;

    UsbSerialInterface.UsbReadCallback onReceive = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                //tvAppend(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR1)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PERMISSAO_USB)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    conexao = manager.openDevice(dispositivo);
                    portaSerial = UsbSerialDevice.createUsbSerialDevice(dispositivo, conexao);
                    if (portaSerial != null) {
                        if (portaSerial.open()) {
                            portaSerial.setBaudRate(9600);
                            portaSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            portaSerial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            portaSerial.setParity(UsbSerialInterface.PARITY_NONE);
                            portaSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            portaSerial.read(onReceive);

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
            }
        }
        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1)
        {
            setResult(ERRO, new Intent());
            finish();
        }else
        {
            manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            portaSerial = null;
            dispositivo = null;
            conexao = null;

            if(manager.getDeviceList().isEmpty())
            {
                setResult(DESCONECTADO, null);
                finish();
            }else
            {
                for(Map.Entry iterador : manager.getDeviceList().entrySet())
                {
                    dispositivo = (UsbDevice) iterador.getValue();
                    if(dispositivo.getVendorId() == 0x2341)
                    {
                        PendingIntent permissao = PendingIntent.getBroadcast(this, 0, new Intent(PERMISSAO_USB), 0);
                        manager.requestPermission(dispositivo, permissao);
                    }
                }
            }

            setContentView(R.layout.activity_definir_rede);

            edssid = (EditText) findViewById(R.id.ssid);
            edsenha = (EditText) findViewById(R.id.senha);
            Button blogin = (Button) findViewById(R.id.login);

            IntentFilter filter = new IntentFilter();
            filter.addAction(PERMISSAO_USB);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            registerReceiver(broadcastReceiver, filter);

            blogin.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            conectar();
                        }
                    }
            );
        }
    }

    void conectar()
    {
        if(portaSerial != null)
        {
            portaSerial.write(("CEP1" + edssid.getText().toString() + "=" + edsenha.getText().toString() + "=").getBytes());
        }
        Intent dadosResultantes = new Intent();
        setResult(OK, dadosResultantes);
        finish();
    }

}
