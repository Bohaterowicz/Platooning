package no.ntnu.platoon.server;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientService {
    private String username;
    private BufferedReader buffReader;
    private BufferedWriter buffWriter;

    private final Socket socket;


    ClientService(Socket s) throws IOException{
        socket = s;
        buffReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        buffWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

    }

    public void setUsername(String usr){
        username = usr;
    }

    public String getUsername(){
        return username;
    }

    public JSONObject initialConnectionMessage(){
        try{
            String jsonString = buffReader.readLine();
            System.out.println(jsonString);

            if(jsonString == null){
                return null;
            }

            JSONObject jsonMessage = new JSONObject(jsonString);
            if(jsonMessage.get(Message.MESSAGE_ID).toString().equals(Message.LOGIN) || jsonMessage.get(Message.MESSAGE_ID).toString().equals(Message.REGISTER)){
                return jsonMessage;
            }
            else{
                //Wrong initial message
                return null;
            }

        } catch (IOException e){
            return null;
        }
    }

    public void sendMessage(String msg) throws IOException {
        buffWriter.write(msg);
        buffWriter.newLine();
        buffWriter.flush();
    }

    public JSONObject readMessages() throws IOException {
        //TODO: This only reads one message from buffered reader, if more messages are queded, they will wait. Probably should iterate over all queued messages...
        if(buffReader.ready()){
            String temp = buffReader.readLine();
            return new JSONObject(temp);
        }
        return null;
    }
}
