package com.example;

import java.util.ArrayList;

public class Memory {
    private static ArrayList<byte[]> byteArray = new ArrayList<byte[]>();

    public ArrayList<byte[]> getByteArray() {
        return byteArray;
    }

    public void setByteArray(ArrayList<byte[]> arrayLst) {
        byteArray = arrayLst;
    }

    public int getByteArrayLength() {
        return byteArray.size();
    }

    public void addMemory(int mem) throws OutOfMemoryError {
        for(int i = 0; i < mem; i++) {
            byteArray.add(new byte[1024*1024]);
        }
    }

    public void removeMemory(int mem) throws IndexOutOfBoundsException {
        for(int i = 0; i < mem; i++) {
            byteArray.remove(byteArray.size()-1);
        }
        System.gc();
    }

    public void releaseMemory() {
        byteArray = new ArrayList<byte[]>();
        System.gc();
    }
}
