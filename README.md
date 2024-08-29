# MIFARE Ultralight C Examples

## Description

This app reads and writes data to NXP's MIFARE Ultralight C tag. It is tested with a fabric new Ultralight C type.

**Please do not use a MIFARE Ultralight or Ultralight EV1 tag with this app** - these have a similar command set but are different 
in Authentication, page locking, counter and other details. Using these tag may brick your tag.

## Data Sheet

The Mifare Ultralight C datasheet is a good source for all questions regarding programming this tag:

MIFARE Ultralight C: Get the datasheet here MF0ICU2: https://www.nxp.com/docs/en/data-sheet/MF0ICU2.pdf

The datasheet is available in the docs folder of this repository, but it is always better to get one from the origin source.

## Usage

There are 5 icons in the "Bottom Navigation Bar":

1) Home: gives an overview about the app and shows the license terms of material used for the app.
2) Read: tries to read the complete content of the tag and display the data in a colored dump.
3) Write Counter: increases the 16-bit one way counter by "1".
4) Write Data: writes up to 16 characters to 4 subsequent pages of the user memory. Another option is to write a current timestamp to the tag.
5) Write Configuration: Select the page a memory protection is active. Selecting page 48 disables any memory protection. Select the mode of memory protection: write access only or read and write access. Select if you want to clear the user memory. The last option is to leave or change the current password (change to the Default or Custom key).

## Screenshots of the app:

### Home Fragment:

![Home screen of the app](screenshots/small/app_home_01.png)


###

Icons: https://www.freeiconspng.com/images/nfc-icon

Nfc Simple PNG Transparent Background: https://www.freeiconspng.com/img/20581

<a href="https://www.freeiconspng.com/img/20581">Nfc Png Simple</a>

https://www.asiarfid.com/how-to-choose-rfid-mifare-chip.html

## Technical details

Minimum SDK is 21 (Android 5)

## Counter on Mifare Ultralight-C:
```plaintext
7.5.11 Counter
The MF0ICU2 features a 16-bit one-way counter, located at the first two bytes of page 
29h. The default counter value is 0000h.

The first1 valid WRITE or COMPATIBILITY WRITE to address 29h can be performed
with any value in the range between 0001h and FFFFh and corresponds to the initial
counter value. Every consecutive WRITE command, which represents the increment, can
contain values between 0001h and 000Fh. Upon such WRITE command and following
mandatory RF reset, the value written to the address 29h is added to the counter content.
After the initial write, only the lower nibble of the first data byte is used for the increment
value (0h-Fh) and the remaining part of the data is ignored. Once the counter value
reaches FFFFh and an increment is performed via a valid WRITE command, the
MF0ICU2 will reply a NAK. If the sum of counter value and increment is higher than
FFFFh, MF0ICU2 will reply a NAK and will not increment the counter.
An increment by zero (0000h) is always possible, but does not have any impact to the
counter value.
It is recommended to protect the access to the counter functionality by authentication.
```
