package com.luoxq.http.proxy;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.*;

public class Common {

    public static void setDefaultProperties(String key, String value) {
        System.setProperty(key, System.getProperty(key, value));
    }


    public static void concurrent(Runnable... cs) {
        ExecutorService exe = Executors.newFixedThreadPool(cs.length);
        Future[] futures = new Future[cs.length];
        for (int i = 0; i < cs.length; i++) {
            futures[i] = exe.submit(cs[i]);
        }
        for (Future f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeAll(Closeable... cs) {
        for (Closeable c : cs) {
            try {
                if (c != null) c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024 * 4];
        int read = 0;
        while ((read = in.read(buf)) >= 0) {
            out.write(buf, 0, read);
            out.flush();
        }
    }


    public static String readLine(InputStream in) throws IOException {
        StringBuilder b = new StringBuilder(50);

        int c = 0;
        while ((c = in.read()) >= 0) {
            b.append((char) c);
            if (c == '\n') {
                break;
            }
        }
        return b.toString();
    }
}
