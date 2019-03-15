package com.luoxq.http.proxy.client;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import static com.luoxq.http.proxy.Common.*;

public class ProxyAgent extends Thread {

    static String server;
    static int port;
    static SSLSocketFactory sslSocketFactory;
    Socket cSock;
    Socket sSock;
    InputStream cin;
    OutputStream cout;

    public ProxyAgent(Socket s) {
        this.cSock = s;
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.err.println("Usage: ProxyAgent Server ServerPort LocalPort");
            System.exit(0);
        }

        server = args[0];
        port = Integer.valueOf(args[1]);

        sslSocketFactory = getSslSocketFactory();

        ServerSocket ss;
        Integer localPort = Integer.valueOf(args[2]);
        ss = ServerSocketFactory.getDefault().createServerSocket(localPort);
        ss.setReceiveBufferSize(1024 * 1024);
        while (true) {
            Socket s = ss.accept();
            new ProxyAgent(s).start();
        }
    }

    static private SSLSocketFactory getSslSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ssl_ctx = SSLContext.getInstance("TLS");
        TrustManager[] certs = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String t) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String t) {
            }
        }};
        ssl_ctx.init(null, certs, new SecureRandom());

        return ssl_ctx.getSocketFactory();
    }

    public void run() {
        try {
            cin = cSock.getInputStream();
            cout = cSock.getOutputStream();
            doConnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                cSock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doConnect() throws Exception {
        sSock = sslSocketFactory.createSocket(server, port);
        sSock.setReceiveBufferSize(1024 * 1024);
        InputStream sin = sSock.getInputStream();
        OutputStream sout = sSock.getOutputStream();
        doTransfer(sin, sout);
    }

    private void doTransfer(InputStream sin, OutputStream sout) {
        concurrent(
                () -> {
                    try {
                        transfer(cin, sout);
                    } catch (IOException e) {
                        close();
                    }
                },
                () -> {
                    try {
                        transfer(sin, cout);
                    } catch (IOException e) {
                        close();
                    }
                }

        );
    }

    void close() {
        closeAll(cSock, sSock);
    }

}
