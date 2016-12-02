#include <SoftwareSerial.h>

SoftwareSerial serial(2,3); //(Arduino RX/ ESP TX), (Arduino TX/ ESP RX)

#define CODE_CONECTAR '1'
#define CODE_REDE_STATUS '2'
#define CODE_IP '3'
#define CODE_CHECK '4'
#define CODE_RESET '0'
#define DELAY 10

struct ServidorWifi
{
  bool comecou = false;
  long TIMEOUT = 6000;
  
  String IP = "";
  int controlador = -1;
  unsigned long comunicacao = 0;
  
  ServidorWifi()
  {
  }

  void begin()
  {
    delay(500);
    serial.begin(115200);
    consumir();
    if(sendWithOk("AT\r\n") == 2 || !comecou)
    {
      sendWithOk("AT+CIOBAUD=38400\r\n");
    }
    comecou = true; 
    serial.begin(38400);
    getIPFromModule();
    delay(100);
    if(isIPValido())
      configurarRede();
    else
      sendWithOk("AT+CWQAP\r\n");
  }

  void reset()
  {
    sendWithOk("AT+CWQAP\r\n");
    //sendWithOk("AT+RST\r\n");
    serial.begin(115200);
    sendWithOk("AT+CWQAP\r\n");
    //sendWithOk("AT+RST\r\n");
    IP = "";
    sendWithOk("AT+CIOBAUD=38400\r\n");
    configurarRede();
    serial.begin(38400); 
  }

  void conectarRede(String ssid, String senha)
  {
    if(!comecou) begin();
    TIMEOUT = 12000;
    sendWithOk("AT+CWJAP=\""+ssid+"\",\""+senha+"\"\r\n");
    getIPFromModule();
    TIMEOUT = 6000;
    configurarRede();
  }

  void configurarRede()
  {
    sendWithOk("AT+CWMODE=1\r\n");
    sendWithOk("AT+CIPMUX=1\r\n");
    sendWithOk("AT+CIPSERVER=1,13130\r\n");
    sendWithOk("AT+CIPCLOSE=1\r\n");
    sendWithOk("AT+CIPSTART=1,\"UDP\",\"0.0.0.0\",13000,13000,2\r\n");  
  }

  int sendWithOk(String msg)
  {
     serial.print(msg);
     return consumir();
  }

  void getIPFromModule()
  {
    serial.print("AT+CIFSR\r\n");
    long t = millis();
    IP = "";
    while(!serial.available() && (millis() - t) < TIMEOUT){delay(DELAY);}
    if(serial.available())
    {
      delay(50);
      if(serial.find("+CIFSR:STAIP,\""))
      {
        
        char c;
        while((millis() - t) < TIMEOUT)
        {
          c = get();
          if(c != '\"')
          {
            IP += c; 
          }else
          {
            break;  
          } 
        }
      }else
      {
        consumir();
      }
    }
  }

  int consumir()
  {
     int estado = 0;
     unsigned long t;
     char c;
     //String messageBack = "";
     bool ok = false;
     t = millis();
     while(!serial.available() && (millis() - t) < TIMEOUT){delay(DELAY);}
     delay(DELAY);
     while((millis() - t) < TIMEOUT)// && estado != 3)
     {
       c = get();
       if(c == -1) break;
       //messageBack += c;

       switch(estado)
       {
         case 0:
           if(c == 'O') estado = 1;
           break;
         case 1:
           if(c == 'K'){ estado = 2; ok = true;}
           else estado = 0;
           break;
         case 3: if(c == '\n') {estado = 4;} break;
         default:
           if(c == '\r') estado = 3;
           if(c == 'O') estado = 1;
           break;
       }
       if(estado == 4) break;
     }
     
     //if(estado != 4) messageBack += "!!!TIMEOUT!!!";
     //messageBack += '\0';
     //Serial.println("Consumiu=\n" + messageBack + "\n===");
     
     if(c == -1)
     {
       if(ok) return 2;
       return 1;
     }else if(estado == 4)
     {
       if(ok) return 2;
       return 1; 
     }
     else return 0;  
  }
 
  char get()
  {
    unsigned long t = millis();
    while(!serial.available() && (millis()-t) < TIMEOUT){delay(DELAY);}
    return serial.read();
  }

  void read(int n, void* vetor)
  {
    int i;
    char* vetorChar = (char*)vetor;
    for(i = 0; i < n; i++)
    {
      vetorChar[i] = get();
    }
  }

  void read(int* a)
  {
    read(sizeof(int), a);
  }

  bool isIPValido()
  {
    int i = 0;
    int p = 0;
    int n = 0;
    if(!IP.length()) return false;
    while(IP[i])
    {
      if(IP[i] == '.')
      { p++;
        if(n > 3 || !n) return false;
        n = 0;
      }else if(IP[i] >= '0' && IP[i] <= '9')
      {
        n++;  
      }else
      {
        return false;  
      }
      i++;  
    }
    if(n == 1 && IP[i-1] == '0') return false;
    return (n && (p == 3));  
  }

  void requestCommands()
  {
    if(controlador == -1 || (millis() - comunicacao > 10000)) return;
    String comando = "S9";
    String cipSend = "AT+CIPSEND=";
          cipSend += controlador;
          cipSend += ",";
          cipSend += comando.length();
          cipSend += "\r\n";
    sendWithOk(cipSend);
    sendWithOk(comando);
  }

  void receberDados()
  {
    if(serial.available())
    {
      if(serial.find("+IPD,"))
      {
        //Serial.println("Recebeu conexao");
        
        delay(DELAY);
        int id = get() - 48;
        //Serial.println(connectionId);
   
        
        if(id != 1)
        {
          controlador = id;
          comunicacao = millis();
          
          get();
          
          long tamanhoEntrada = 0;
          long i;
          char c = get();
          
          while(c != ':')
          {
            tamanhoEntrada = tamanhoEntrada*10 + c-48;
            c = get();
          }
          
          String recebido = "";

          while(tamanhoEntrada)
          {
            c = get();
            recebido += c;
            tamanhoEntrada--;
          }

          recebido += '\0';

          Serial.println(recebido);
          /*
          String resposta = "OK";
          String cipSend = "AT+CIPSEND=";
          cipSend += id;
          cipSend += ",";
          cipSend += resposta.length();
          cipSend += "\r\n";
   
          sendWithOk(cipSend);
          sendWithOk(resposta);*/
        }else
        {
          String cipSend = "AT+CIPSEND=";
          cipSend += id;
          cipSend += ",";
          cipSend += IP.length();
          cipSend += "\r\n";
          sendWithOk(cipSend);
          sendWithOk(IP);
        }
        
      }else
      {
        consumir();  
      }
    }  
  }
};

ServidorWifi servidor;

void processarSerial()
{
  int estado = 0;
  char c;
  //Serial.println("Atualizando Serial");
  if(Serial.available() && Serial.peek() == 'C')
  {
    delay(DELAY);
    while(Serial.available())
    {
      c = Serial.read();
      Serial.peek();
      switch(estado)
      {
        case 0:
          if(c == 'C'){ estado = 1;}
          break;
        case 1:
          if(c == 'E'){ estado = 2;}
          else{ estado = 0;}
          break;
        case 2:
          if(c == 'P'){ estado = 3;}
          else{ estado = 0;}
          break;
        default:
          estado = 0;
          break;
      }
      if(estado == 3)
      {
        c = Serial.read();
        if(c == CODE_CONECTAR)
        {
          int i;
          String nome = Serial.readStringUntil('=');
          String senha = Serial.readStringUntil('=');
  
          servidor.conectarRede(nome, senha);
        }else if(c == CODE_REDE_STATUS)
        {
          Serial.println(servidor.isIPValido());
        }else if(c == CODE_IP)
        {
          Serial.println(servidor.IP);
        }else if(c == CODE_CHECK)
        {
          if(servidor.sendWithOk("AT\r\n") == 2) Serial.println("Parece estar OK");
          else Serial.println("Nao estah funcionando corretamente");
        }else if(c == CODE_RESET)
        {
          servidor.reset();  
        }
      }  
    }
  }
}

void setup()
{
  Serial.begin(9600);
  servidor.begin();
  pinMode(8, OUTPUT);
}

long mil = 0;

void loop()
{
  processarSerial();
  servidor.receberDados();
  if(millis()-mil > 100)
  {
    mil = millis();
    servidor.requestCommands();
  }
  digitalWrite(8, servidor.isIPValido());
}




