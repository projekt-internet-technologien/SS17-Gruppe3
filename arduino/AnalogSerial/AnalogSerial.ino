// These constants won't change.  They're used to give names
// to the pins used:
const int analogInPin = A0;

int sensorValue = 0;

void setup() {
  // initialize serial communications at 9600 bps:
  Serial.begin(9600);
}

void loop() {
  // read the analog in value:
  sensorValue = analogRead(analogInPin);

  Serial.print(sensorValue);
  Serial.print("#");

  // wait 2 milliseconds before the next loop
  // for the analog-to-digital converter to settle
  // after the last reading:
  delay(20);
}
