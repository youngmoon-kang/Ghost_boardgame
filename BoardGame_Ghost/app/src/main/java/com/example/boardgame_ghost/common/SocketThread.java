package com.example.boardgame_ghost.common;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketThread extends Thread{
    String host = GameInfo.ip;
    String data;

    public SocketThread(String data)
    {
        this.data = data;
    }

    @Override
    public void run()
    {
        try{
            int port = GameInfo.port;
            Socket socket = new Socket(host, port);
            ObjectOutputStream outstream = new ObjectOutputStream(socket.getOutputStream());
            outstream.writeObject(data);
            outstream.flush();

            ObjectInputStream instream = new ObjectInputStream(socket.getInputStream());
            String response = (String) instream.readObject();


        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
