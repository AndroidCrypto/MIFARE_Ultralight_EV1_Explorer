# Commands for MIFARE Ultralight tags

## Get Version (Ultralight EV1 only)
Reads the version data of a NFC tag, returns an 8 byte long sequence:

Example response: length: 8 data: 0004030101000b03

Command: 60h

```plaintext
Table 15. GET_VERSION response for MF0UL11 and MF0UL21

Byte no. Description            MF0UL11/ MF0ULH11  MF0UL21/ MF0ULH21  Interpretation  
0        fixed header           00h                00h
1        vendor ID              04h                04h                NXP Semiconductors
2        product type           03h                03h                MIFARE Ultralight
3        product subtype        01h/02h            01h/02h            17 pF / 50pF
4        major product version  01h                01h                EV1           
5        minor product version  00h                00h                V0
6        storage size           0Bh                0Eh                see following explanation
7        protocol type          03h                03h                ISO/IEC 14443-3 compliant

Storage size 08h = 80 byte = 48 bytes free user memory, total of 20 pages
             0Eh = 164 byte = 128 bytes free user memory, total of 32 pages
             
Counter: 3 independent 24-bit true one-way counters  
Protection: 32-bit password protection to prevent unintended memory operations          
```

```plaintext
Technical data of a MIFARE Ultralight C
Complete memory: 192 bytes = total of 48 pages
Free user memory: 144 bytes = 36 pages
Counter: 16-bit one-way counter
Protection: 3DES Authentication
```

## 3DES Authentication on MIFARE Ultralight C only

The tag I'm describing uses an authentication scheme based on mutual Triple DES Encryption (3DES) that runs in 3 steps.

Note on the 3DES key: usually we are using three different DES keys (each 8 bytes long) that were used for a 
"Encryption - Decryption - Encryption" sequence. But in this case we are using two different keys only, and the key 
for the second/last encryption is the same as the one for the first encryption.

a) step 1: The card reader asks the tag to provide the value encRndB, meaning the encrypted random value "B" that is 
           8 bytes long (e.g. dd081cb2b80569aeh).

           The card reader decrypts the received encRndB with the 3DES key and gets RndB (e.g. 8e8a7490662a0362h).

           The card reader rotates the RndB by one position (byte) to the left (e.g. 8a7490662a03628eh) with the name rndBrot.

b) step 2: The card reader generates a random value RndA that is 8 bytes long (e.g. 9255dadbd82be1d1h).

           The card reader combines RndA and rndBrot to a 16 bytes long value (e.g. 9255dadbd82be1d18a7490662a03628eh) 
           with the name rndARndBrot

           The card reader encrypts rndARndBrot with the 3DES key and gets encRndARndBrot (e.g. 988e118f7c206a8bdf1b4f229836c675h).

           The card reader sends encRndARndBrot to the tag.

c) step 3: The card reader receives an 8 bytes long value (e.g. 671cf4086725d445h) with the name encRndArot.

           The card reader decrypts encRndArot with the 3DES key and gets rndArot (e.g. 55dadbd82be1d192h)

           The card  compares rndArot (e.g. 55dadbd82be1d192h) with the left rotatet rndA (see step 2, 9255dadbd82be1d1h -> 55dadbd82be1d192h).

           If the (self generated) rndArot is equals to the received rndArot the authentication was successful.

For details see datasheet pages 12 ff:

**Programming of 3DES key to memory**
The 16 bytes of the 3DES key are programmed to memory pages from 2Ch to 2Fh. The keys are stored in memory as shown in Table 10. 
The key itself can be written during personalization or at any later stage using the WRITE (see Section 9.3) or 
COMPATIBILITY WRITE (see Section 9.4) command. For both commands, Byte 0 is always sent first.

On example of Key1 = 0001020304050607h and Key2 = 08090A0B0C0D0E0Fh, the command sequence needed for key programming with 
WRITE command is:

• A2 2C 07 06 05 04 CRC
• A2 2D 03 02 01 00 CRC
• A2 2E 0F 0E 0D 0C CRC
• A2 2F 0B 0A 09 08 CRC

In other words, that means that Key1 and Key2 are saved on a mirrored basis.

Command: A2h

**Configuration for memory access via 3DES Authentication**

The behavior of the memory access rights depending on the authentication is configured with two configuration bytes, 
AUTH0 and AUTH1, located in pages 2Ah and 2Bh. Both configuration bytes are located in Byte 0 of the respective pages (see also Table 5).
• AUTH0 defines the page address from which the authentication is required. Valid address values for byte AUTH0 are from 03h to 30h.
• Setting AUTH0 to 30h effectively disables memory protection.
• AUTH1 determines if write access is restricted or both read and write access are restricted, see Table 12

Table 12. AUTH1 bit description:
- Bit 0 in Byte 0: 0 = read and write access restricted
                   1 = write access restricted, read access allowed without authentication    
Example: 00 00 00 00 = read and write access restricted
         01 00 00 00 = write access restricted, read access allowed without authentication

## Read Page
Read the content of a page (4 bytes)

Example response: length: 4 data: 00000000

Note: if the page is read restricted you need to authenticate first before reading any content

Command: 30h

## Write Page
Writes the content of a page (4 bytes)

Example response: length: 4 data: 00000000

Note: if the page is write restricted you need to authenticate first before reading any content

Command: A2h

## Lock Bits
The bits for locking a page (means the page is set to "read only") are located in 2 pages: the lock bytes 0 and 1 
are located in page 2h, set lock bytes 2 and 3 are located in page 28h. Each bit of the lock bytes represent a 
page or page range, and if you write a "1" bit at the position the corresponding page get locked.

The bits can be set to "1" just one time, after that the corresponding page is irrevocably locked. For that 
reason I don't offer a solution for setting these bits. If you want protect the pages against any write access kindly 
use the authentication method.

## One Time Programming Bits (OTP)
The page 03h is reserved for OTP usage - you can write a "1" to each bit of the 32 bits (in 4 bytes) and during the next 
usage these values are readable. As this process is irrevocably I do not offer any method to use the OTP area.

## Counter
The MIFARE Ultralight C tag features a 16-bit one-way counter, located at the **first two bytes of page 29h**. The default 
counter value is 0000h.

The first1 valid WRITE or COMPATIBILITY WRITE to address 29h can be performed with any value in the range between 0001h 
and FFFFh and corresponds to the initial counter value. Every consecutive WRITE command, which represents the increment, 
can contain values between 0001h and 000Fh. Upon such WRITE command and following mandatory RF reset, the value written 
to the address 29h is added to the counter content. 

After the initial write, only the lower nibble of the first data byte is used for the increment value (0h-Fh) and the 
remaining part of the data is ignored. Once the counter value reaches FFFFh and an increment is performed via a valid 
WRITE command, the MF0ICU2 will reply a NAK. If the sum of counter value and increment is higher than FFFFh, the tag 
will reply a NAK and will not increment the counter. 

An increment by zero (0000h) is always possible, but does not have any impact to the counter value. **It is recommended to 
protect the access to the counter functionality by authentication.**

If you read out page 41d use the first two bytes only; the value is LSB encoded. 

## Service methods

## convertPassword

When changing the password we provide a new, 16 bytes long password. For a later usage authentication you need to convrert 
the password by using this method. It requires a 16 bytes long password that gets converted to a 16 bytes long password for 
authentication.


