package rr.simple;

import java.util.HashMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

class Dumper {

    public static void dumpMap(String filePath, HashMap<String, Integer> map) {
        try {
            File file = new File(filePath);
            FileOutputStream fos = new FileOutputStream(file);

            map.forEach((k, v) -> {
                try {
                    dumpString(fos, k);
                    dumpInt(fos, v);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (FileNotFoundException e) {
            //TODO: handle exception
            e.printStackTrace();
        }
    }

    public static void dumpInt(FileOutputStream fos, int n) throws IOException {
        fos.write(bigEndian(n));
    }

    public static void dumpString(FileOutputStream fos, String s) throws IOException {
        dumpInt(fos, s.length());
        fos.write(s.getBytes());
    }

    public static byte[] bigEndian(int n) {
         ByteBuffer bb = ByteBuffer.allocate(4);
         bb.order(ByteOrder.BIG_ENDIAN);
         bb.putInt(n);
         return bb.array();
    }
}
