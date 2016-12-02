package tcc.impressora3d;

import android.widget.Toast;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by nanio on 14/11/16.
 */

public class GCODE {

    byte [] dados;
    int tamanho;
    int posicao = 0;

    void prepararLeitura()
    {
        posicao = 0;
    }

    String proximoComando()
    {
        String retorno = "";
        if(posicao >= tamanho) return null;
        while(dados[posicao] == ';' || dados[posicao] == '\r' || dados[posicao] == '\n')
        {
            while(posicao < tamanho && dados[posicao] != '\n')
            { posicao++;
            }
            posicao++;
            if(posicao >= tamanho) return null;
        }
        while(posicao < tamanho && dados[posicao] != '\n' && dados[posicao] != ';')
        {
            retorno += (char)dados[posicao];
            posicao++;
        }
        if(dados[posicao] == ';')
        {
            while(posicao < tamanho && dados[posicao] != '\n')
            { posicao++;
            }
        }
        if(posicao < tamanho) posicao++;
        return retorno.trim();
    }

    float progresso()
    {
        return (float)posicao/tamanho;
    }

    static GCODE carregar(String caminho)
    {
        try {
            FileInputStream fileStream = new FileInputStream(caminho);
            FileChannel canal = fileStream.getChannel();
            MappedByteBuffer buffer = canal.map(FileChannel.MapMode.READ_ONLY, 0, canal.size());

            byte informacoes[] = new byte[(int) canal.size()];

            buffer.get(informacoes, 0, (int) canal.size());
            GCODE gcode = new GCODE();
            gcode.dados = informacoes;
            gcode.tamanho = (int) canal.size();
            gcode.prepararLeitura();
            return gcode;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
