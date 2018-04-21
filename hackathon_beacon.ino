#include <SPI.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"

//Common definitions
#define FACTORYRESET_ENABLE true
#define VERBOSE_MODE true

//Pin definitions
#define BLUEFRUIT_SPI_CS 8
#define BLUEFRUIT_SPI_IRQ 7
#define BLUEFRUIT_SPI_RST 4

//Beacon definitions
#define BEACON_NAME "TheGrassIsGreen"
#define BEACON_MANUFACTURER_ID "0xFF00"
#define BEACON_UUID "01-12-23-34-45-56-67-78-89-9A-AB-BC-CD-DE-EF-F0"
#define BEACON_MAJOR "0x0000"
#define BEACON_MINOR "0x0000"
#define BEACON_RSSI_1M "-54"

//Hardware SPI
Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

void setup(void) {
  Serial.begin(115200);
  Serial.print(F("Creating the grass is green beacon... "));
  Serial.flush();

  if (!ble.begin(VERBOSE_MODE)) {
    Serial.println(F("Couldn't find Bluefruit, make sure it's in CoMmanD mode & check wiring?"));
    return;
  }
  Serial.println(F("Done!"));
  if (FACTORYRESET_ENABLE) {
    Serial.println(F("Performing a factory reset: "));
    if (!ble.factoryReset()){
      Serial.println(F("Couldn't factory reset"));
      return;
    }
  }

  //Disable the echo command
  ble.echo(false);

  Serial.println(F("Requesting device info:"));
  ble.info();

  Serial.println(F("Setting beacon configuration details: "));
  ble.print("AT+GAPDEVNAME=");
  ble.println(BEACON_NAME);

  if(!ble.waitForOK()) {
    Serial.println(F("We didn't get the rename OK!"));
    return;
  }

  ble.print("AT+BLEBEACON=");
  ble.print(BEACON_MANUFACTURER_ID); ble.print(',');
  ble.print(BEACON_UUID); ble.print(',');
  ble.print(BEACON_MAJOR); ble.print(',');
  ble.print(BEACON_MINOR); ble.print(',');
  ble.println(BEACON_RSSI_1M);

  // check response status
  if (! ble.waitForOK() ) {
    Serial.println(F("We didn't get the OK!"));
    return;
  }

  Serial.println(F("Started the beacon!"));
}

void loop(void) {}
