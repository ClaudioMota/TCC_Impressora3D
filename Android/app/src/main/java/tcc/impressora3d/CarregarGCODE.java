package tcc.impressora3d;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;


/**
 * Created by nanio on 11/11/16.
 */

public class CarregarGCODE extends Activity {

    public static final int CANCELADO = 0;
    public static final int OK = 1;
    public static final int ERRO = 0;


    ArrayAdapter<String> arrayAdapter;
    ArrayList<File> pilha = new ArrayList<File>();
    File diretorio;

    ListView lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            setResult(ERRO, null);
            finish();
        }

        diretorio = Environment.getExternalStorageDirectory();

        setContentView(R.layout.activity_diretorios);

        lista = (ListView)findViewById(R.id.lista);

        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectFile(position);
            }
        });

        resetListaDiretorio();
    }

    void resetListaDiretorio()
    {
        String nomes[] = diretorio.list();
        String nomesFinais[] = new String[nomes.length+1];

        nomesFinais[0] = "..";
        for(int i = 0; i < nomes.length; i++)
        {
            nomesFinais[i+1] = nomes[i];
        }

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, nomesFinais);
        lista.setAdapter(arrayAdapter);
    }

    void selectFile(int posicao)
    {
        if(posicao == 0)
        {
            if(!pilha.isEmpty()) diretorio = pilha.remove(pilha.size()-1);
        }else
        {
            File escolha = diretorio.listFiles()[posicao-1];
            if(escolha.exists())
            {
                if(escolha.isDirectory())
                {
                    pilha.add(diretorio);
                    diretorio = escolha;
                }else if(escolha.isFile())
                {
                    Intent dadosResultantes = new Intent();
                    dadosResultantes.putExtra("path", escolha.getAbsolutePath());
                    setResult(OK, dadosResultantes);
                    finish();
                }
            }
        }
        resetListaDiretorio();
    }
}
