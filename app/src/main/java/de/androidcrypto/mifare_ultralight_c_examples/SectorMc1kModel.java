package de.androidcrypto.mifare_ultralight_c_examples;

import java.nio.charset.StandardCharsets;

public class SectorMc1kModel {

    /**
     * this class is for usage with Mifare Classic 1K tags
     */

    private int sectorNumber;
    private boolean isSector0;
    private boolean isReadableSector;
    private byte[] sectorData = new byte[64]; // sector 0 first 16 bytes a re UID & manufacture info, last 16 bytes is access block
    private byte[] uidData; // contains the UID & manufacture info, only if isSector0 = true
    private byte[] blockData; // contains 2 (if sector 0) or 3 blocks of 16 bytes of data
    private byte[] accessBlock = new byte[16]; // complete block, sections see below. key A and B are nulled as they can't read out
    private byte[] keyA = new byte[6]; // access key A
    private byte[] accessBits = new byte[3]; // 3 access bytes for access to the data elements
    private byte[] unused = new byte[1]; // unused byte, can be used for data
    private byte[] keyB = new byte[6]; // access key B

    public SectorMc1kModel(int sectorNumber, boolean isSector0, boolean isReadableSector, byte[] sectorData, byte[] uidData, byte[] blockData, byte[] accessBlock, byte[] keyA, byte[] accessBits, byte[] unused, byte[] keyB) {
        this.sectorNumber = sectorNumber;
        this.isSector0 = isSector0;
        this.isReadableSector = isReadableSector;
        this.sectorData = sectorData;
        this.uidData = uidData;
        this.blockData = blockData;
        this.accessBlock = accessBlock;
        this.keyA = keyA;
        this.accessBits = accessBits;
        this.unused = unused;
        this.keyB = keyB;
    }

    public int getSectorNumber() {
        return sectorNumber;
    }

    public void setSectorNumber(int sectorNumber) {
        this.sectorNumber = sectorNumber;
    }

    public boolean isSector0() {
        return isSector0;
    }

    public void setSector0(boolean sector0) {
        isSector0 = sector0;
    }

    public boolean isReadableSector() {
        return isReadableSector;
    }

    public void setReadableSector(boolean readableSector) {
        isReadableSector = readableSector;
    }

    public byte[] getSectorData() {
        return sectorData;
    }

    public void setSectorData(byte[] sectorData) {
        this.sectorData = sectorData;
    }

    public byte[] getUidData() {
        return uidData;
    }

    public void setUidData(byte[] uidData) {
        this.uidData = uidData;
    }

    public byte[] getBlockData() {
        return blockData;
    }

    public void setBlockData(byte[] blockData) {
        this.blockData = blockData;
    }

    public byte[] getAccessBlock() {
        return accessBlock;
    }

    public void setAccessBlock(byte[] accessBlock) {
        this.accessBlock = accessBlock;
    }

    public byte[] getKeyA() {
        return keyA;
    }

    public void setKeyA(byte[] keyA) {
        this.keyA = keyA;
    }

    public byte[] getAccessBits() {
        return accessBits;
    }

    public void setAccessBits(byte[] accessBits) {
        this.accessBits = accessBits;
    }

    public byte[] getUnused() {
        return unused;
    }

    public void setUnused(byte[] unused) {
        this.unused = unused;
    }

    public byte[] getKeyB() {
        return keyB;
    }

    public void setKeyB(byte[] keyB) {
        this.keyB = keyB;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("MifareClassic sector: ").append(sectorNumber).append("\n");
        sb.append("isSector0: ").append(isSector0).append("\n");
        sb.append("isReadableSector: ").append(isReadableSector).append("\n");
        if (sectorData != null) {
            sb.append("sectorData length: ").append(sectorData.length).append(" data: ").append(bytesToHexNpe(sectorData)).append("\n");
        } else {
            sb.append("sectorData is NULL").append("\n");
        }
        if (uidData != null) {
            sb.append("uidData length: ").append(uidData.length).append(" data: ").append(bytesToHexNpe(uidData)).append("\n");
        } else {
            sb.append("uidData is NULL").append("\n");
        }
        if (blockData != null) {
            sb.append("blockData length: ").append(blockData.length).append(" data: ").append(bytesToHexNpe(blockData)).append("\n");
            sb.append("blockData UTF-8: " + new String(blockData, StandardCharsets.UTF_8)).append("\n");
        } else {
            sb.append("blockData is NULL").append("\n");
        }
        if (accessBlock != null) {
            sb.append("accessBlock length: ").append(accessBlock.length).append(" data: ").append(bytesToHexNpe(accessBlock)).append("\n");
        } else {
            sb.append("accessBlock is NULL").append("\n");
        }
        if (keyA != null) {
            sb.append("keyA length: ").append(keyA.length).append(" data: ").append(bytesToHexNpe(keyA)).append("\n");
        } else {
            sb.append("keyA is NULL").append("\n");
        }
        if (accessBits != null) {
            sb.append("accessBits: ").append(accessBits.length).append(" data: ").append(bytesToHexNpe(accessBits)).append("\n");
        } else {
            sb.append("accessBits is NULL").append("\n");
        }
        if (unused != null) {
            sb.append("unused: ").append(unused.length).append(" data: ").append(bytesToHexNpe(unused)).append("\n");
        } else {
            sb.append("unused is NULL").append("\n");
        }
        if (keyB != null) {
            sb.append("keyB length: ").append(keyB.length).append(" data: ").append(bytesToHexNpe(keyB));
        } else {
            sb.append("keyB is NULL");
        }
        return sb.toString();
    }


    /**
     * converts a byte array to a hex encoded string
     * This method is Null Pointer Exception (NPE) safe
     *
     * @param bytes
     * @return hex encoded string
     */
    private static String bytesToHexNpe(byte[] bytes) {
        if (bytes != null) {
            StringBuffer result = new StringBuffer();
            for (byte b : bytes)
                result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            return result.toString();
        } else {
            return "";
        }
    }
}
