package tcc.impressora3d;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Created by nanio on 14/11/16.
 */

public class ControladorImpressora extends IntentService {

    public static boolean RUNNING = false;
    public static boolean print = false;
    public static String ATUALIZAR = "ATUALIZAR_PROGRESSO";

    ControladorImpressora()
    {
        super("Impressao3D");
    }

    @Override
    protected void onHandleIntent(Intent workIntent)
    {
        RUNNING = true;
        while(print) {
            ConexaoWifi conexao = MainActivity.conexao;
            if (conexao != null && conexao.isAtivo()) {
                GCODE gcode = MainActivity.gcode;
                if(gcode != null)
                {
                        try
                        {
                            String rcv = null;
                            if(conexao.isPending()) {
                                rcv = conexao.receive();
                                if(rcv.charAt(0) == 'S')
                                {
                                    int n = 1;//rcv.charAt(1)-48;
                                    String comandos = "";
                                    for(int i = 0; i < n; i++) {
                                        String comando = gcode.proximoComando();
                                        if (comando != null) {
                                            comandos += comando + "\n";
                                        } else {
                                            print = false;
                                            break;
                                        }
                                    }
                                    conexao.send(comandos);
                                    //MainActivity.singleton.atualizarProgresso(gcode.progresso()*100.0f);
                                    sendBroadcast(new Intent(ATUALIZAR));
                                }
                            }

                            Thread.sleep(50);
                        }catch(Exception e)
                        {
                            e.printStackTrace();
                            break;
                        }

                }else
                {
                    break;
                }
            }else
            {
                break;
            }
        }
        RUNNING = false;
    }
}
