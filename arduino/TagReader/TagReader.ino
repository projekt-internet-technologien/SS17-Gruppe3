#include <Adafruit_PN532.h>

// If using the breakout or shield with I2C, define just the pins connected
// to the IRQ and reset lines.  Use the values below (2, 3) for the shield!
#define PN532_IRQ   (2)
#define PN532_RESET (3)  // Not connected by default on the NFC Shield

// Shield with an I2C connection
Adafruit_PN532 nfc(PN532_IRQ, PN532_RESET);

void setup(void) {
  // Sets the data rate
  Serial.begin(115200);
  // Begin the NFC-Reader/Writer
  nfc.begin();
  nfc.SAMConfig();
}


void loop(void) {
 
  uint8_t success;
  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer to store the returned UID
  uint8_t uidLength;                        // Length of the UID (4 or 7 bytes depending on ISO14443A card type)
    
  // Wait for an ISO14443A type cards (Mifare, etc.).  When one is found
  // 'uid' will be populated with the UID, and uidLength will indicate
  // if the uid is 4 bytes (Mifare Classic) or 7 bytes (Mifare Ultralight)
  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength);
  
  if (success) {
    if (uidLength == 4) {
      //Seems to be an RFID-Card, Trying to authenticate
      uint8_t keya[6] = { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF };
	  
	    // Start with block 1 since sector 0
      success = nfc.mifareclassic_AuthenticateBlock(uid, uidLength, 4, 1, keya);
	  
      if (success) {
        // Authentication success
        uint8_t data[16];
		
        // If you want to write something to block 4 to test with, uncomment the following line
        //memcpy(data, (const uint8_t[]){ 'G', 'r', 'u', 'p', 'p', 'e', '2', 0, 0, 0, 0, 0, 0, 0, 0, 0 }, sizeof data);
        //success = nfc.mifareclassic_WriteDataBlock (4, data);

        // Try to read the contents of block 4
        success = nfc.mifareclassic_ReadDataBlock(4, data);
		
        if (success) {
          // Data seems to have been read ... spit it out
          Serial.print((char*)data);
          Serial.print("#");
        }
      }
    }
    else if (uidLength == 7) {
      // Seems to be an NFC-Tag
      uint8_t page7[0];
      uint8_t page8[0];
      uint8_t page9[0];
      
      nfc.ntag2xx_ReadPage(7, page7);
      nfc.ntag2xx_ReadPage(8, page8);
      nfc.ntag2xx_ReadPage(9, page9);
     
      Serial.print((char)page7[2]);
      Serial.print((char)page7[3]);
      Serial.print((char)page8[0]);
      Serial.print((char)page8[1]);
      Serial.print((char)page8[2]);
      Serial.print((char)page8[3]);
      Serial.print((char)page9[0]);
      Serial.print("#");
    }
    
    // Wait a bit before reading the card again
    delay(3000);
  }
}

