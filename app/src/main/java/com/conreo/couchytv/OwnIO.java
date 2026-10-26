package com.conreo.couchytv;

import java.io.*;

/** Stream reading compatible with Android 8 and later. */
public final class OwnIO {
    public static byte[] read(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[32768];
        int count;
        while ((count = input.read(data)) != -1) out.write(data, 0, count);
        return out.toByteArray();
    }
}
