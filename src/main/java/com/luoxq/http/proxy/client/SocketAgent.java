package com.luoxq.http.proxy.client;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static com.luoxq.http.proxy.Common.*;

/**
 * A port forwarding program. Listen on a local port and forward all the content in the socket in to a specific server
 * socket address and return the data from server to the client.
 */
public class SocketAgent extends Thread {

    static String server;
    static int port;
    Socket cSock;
    Socket sSock;
    InputStream cin;
    OutputStream cout;

    public SocketAgent(Socket s) {
        this.cSock = s;
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 3) {
            System.err.println("Usage: SocketAgent Server ServerPort LocalPort");
            System.exit(0);
        }

        server = args[0];
        port = Integer.valueOf(args[1]);


        ServerSocket ss;
        Integer localPort = Integer.valueOf(args[2]);
        ss = ServerSocketFactory.getDefault().createServerSocket(localPort);
        ss.setReceiveBufferSize(1024 * 1024);
        while (true) {
            Socket s = ss.accept();
            new SocketAgent(s).start();
        }
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
        sSock = new Socket(server, port);
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
