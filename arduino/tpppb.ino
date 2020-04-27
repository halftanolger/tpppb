
#include <HX711.h>
#include <SoftwareSerial.h>

HX711 scale;
SoftwareSerial bluetooth(4,5); //(TX,RX)

void setup() 
{  
  
  scale.begin(9,8); //(DOUT,SCK)   
  scale.tare();
  scale.set_scale(32500);
  
  bluetooth.begin(9600);
  bluetooth.listen();

  pinMode(10,OUTPUT);
  pinMode(11,OUTPUT);
  digitalWrite(10,HIGH);
  digitalWrite(11,LOW);
  
}

void loop() 
{

  float f = scale.get_units(3);

  if (f < 0)
    f = 0.0;

  if (f > 4.9 && f < 6.0)
    digitalWrite(11,HIGH);
  else
    digitalWrite(11,LOW);
 
  String data = "[" + String(f,1) + " kg]";
  bluetooth.print(data);
    
}
