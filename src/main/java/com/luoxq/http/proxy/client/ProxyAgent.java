package com.luoxq.http.proxy.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import static com.luoxq.http.proxy.Common.*;
import static com.luoxq.http.proxy.Common.transfer;

public class ProxyAgent extends Thread {

    static String server;
    static int port;

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.err.println("Usage: ProxyAgent Server ServerPort LocalPort");
            System.exit(0);
        }

        server = args[0];
        port = Integer.valueOf(args[1]);

        setDefaultProperties("javax.net.ssl.keyStore", "keystore");
        setDefaultProperties("javax.net.ssl.keyStorePassword", "changeit");
        setDefaultProperties("javax.net.ssl.trustStore", "keystore");
        setDefaultProperties("javax.net.ssl.trustStorePassword", "changeit");


        ServerSocket ss;
        Integer localPort = Integer.valueOf(args[2]);

        if (args.length > 3 && "secure".equalsIgnoreCase(args[3])) {
            ss = SSLServerSocketFactory.getDefault()
                    .createServerSocket(localPort);
        } else {
            ss = ServerSocketFactory.getDefault().createServerSocket(localPort);
        }
        ss.setReceiveBufferSize(1024 * 1024);
        while (true) {
            Socket s = ss.accept();
            new ProxyAgent(s).start();
        }
    }

    Socket cSock;
    Socket sSock;
    InputStream cin;
    OutputStream cout;

    public ProxyAgent(Socket s) {
        this.cSock = s;
    }

    public void run() {

        try {
            cin = new BufferedInputStream(cSock.getInputStream());
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

    private void doConnect() throws UnknownHostException, IOException {
        sSock = SSLSocketFactory.getDefault().createSocket(server, port);
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
