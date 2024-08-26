# MIFARE Ultralight C Examples

## Note on this app

At the moment this app is just a stub with no running code for a MIFARE Ultralight NFC tag. I will add the code
for these tags soon.

### Description

This app reads and writes data to NXP's MIFARE Ultralight C tag. It is tested with the Ultralight C type so
I cannot guarantee that it works on the other type also.

**Please do not use a MIFARE Ultralight EV1 tag with this app** - these have a similar command set but are different 
in Authentication, page locking, counter and other details.

The Mifare Ultralight C datasheet is a good source for all questions regarding programming:

MIFARE Ultralight C: Get the datasheet here MF0ICU2: https://www.nxp.com/docs/en/data-sheet/MF0ICU2.pdf

The datasheet is available in the docs folder of this repository, but it is always better to get one from the origin source.

## Usage

There are 5 icons in the "Bottom Navigation Bar":

1) Home:
2) Read:
3) Red Value:
4) Write:
5) Write Value:



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
