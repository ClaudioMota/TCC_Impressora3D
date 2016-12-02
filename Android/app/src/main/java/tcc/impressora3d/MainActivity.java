package tcc.impressora3d;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    static MainActivity singleton = null;

    public static final int ACT_CARREGAR_GCODE = 1;
    public static final int ACT_DEFINIR_REDE = 2;

    TextView texto;
    Button bcarregar;
    Button bdefinir;
    Button bconectar;
    Button bprint;
    ProgressBar progress;
    TextView tProgresso;


    public static ConexaoWifi conexao = null;
    public static GCODE gcode = null;

    DataUpdateReceiver receiver = null;

    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ControladorImpressora.ATUALIZAR)) {
                atualizarProgresso();
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        singleton = this;

        setContentView(R.layout.activity_main);

        progress = (ProgressBar)findViewById(R.id.progresso);
        tProgresso = (TextView)findViewById(R.id.tProgresso);

        progress.setVisibility(View.INVISIBLE);
        tProgresso.setVisibility(View.INVISIBLE);

        bcarregar = (Button) findViewById(R.id.carregar);
        bdefinir = (Button) findViewById(R.id.definirRede);
        bconectar = (Button) findViewById(R.id.conectar);
        bprint = (Button) findViewById(R.id.imprimir);

        receiver = new DataUpdateReceiver();

        IntentFilter intentFilter = new IntentFilter(ControladorImpressora.ATUALIZAR);
        registerReceiver(receiver, intentFilter);

        bconectar.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        conexao = ConexaoWifi.conectar();
                        if(conexao != null) Toast.makeText(getApplicationContext(), "Conectado à impressora 3D", Toast.LENGTH_LONG).show();
                        else Toast.makeText(getApplicationContext(), "Não foi possível estabelecer a conexão", Toast.LENGTH_LONG).show();
                    }
                }
        );

        bdefinir.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, DefinirRede.class);
                        startActivityForResult(intent, ACT_DEFINIR_REDE);
                    }
                }
        );

        bcarregar.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, CarregarGCODE.class);
                        startActivityForResult(intent, ACT_CARREGAR_GCODE);
                    }
                }
        );

        bprint.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ControladorImpressora.print = !ControladorImpressora.print;

                        if(conexao != null && gcode != null && !ControladorImpressora.RUNNING)
                        {
                            gcode.prepararLeitura();
                            Intent it = new Intent(MainActivity.this, ControladorImpressora.class);
                            startService(it);
                        }

                        atualizarProgresso();
                    }
                }
        );
    }

    void atualizarProgresso()
    {
        float valor = 0.0f;
        if(gcode != null) valor = gcode.posicao*100.0f/gcode.tamanho;
        int visibilidade = ControladorImpressora.print ? View.VISIBLE : View.INVISIBLE;
        progress.setVisibility(visibilidade);
        tProgresso.setVisibility(visibilidade);

        progress.setProgress((int) valor);
        tProgresso.setText(new Float(((float)(int)(valor*100))/100.0f).toString() + "%");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACT_CARREGAR_GCODE:
                if (resultCode == CarregarGCODE.ERRO) {
                    Toast.makeText(getApplicationContext(), "Cartão SD não conectado", Toast.LENGTH_LONG);
                } else if (resultCode == CarregarGCODE.OK) {
                    gcode = GCODE.carregar(data.getStringExtra("path"));
                    if(gcode != null) Toast.makeText(getApplicationContext(), "Arquivo carregado", Toast.LENGTH_LONG).show();
                    else  Toast.makeText(getApplicationContext(), "Não foi possível abrir o arquivo", Toast.LENGTH_LONG).show();
                }
                break;
            case ACT_DEFINIR_REDE:
                if (resultCode == DefinirRede.ERRO) {
                    Toast.makeText(getApplicationContext(), "Este Android não suporta comunicação USB", Toast.LENGTH_LONG).show();
                }else if(resultCode == DefinirRede.DESCONECTADO)
                {
                    Toast.makeText(getApplicationContext(), "Necessita conectar o USB à impressora 3D", Toast.LENGTH_LONG).show();
                }
                break;
        }

    }
}
