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

    public JSONObject initialConnectionMessage(){
        try{
            String jsonString = buffReader.readLine();
            System.out.print(jsonString);

            if(jsonString == null){
                return null;
            }

            JSONObject jsonMessage = new JSONObject(jsonString);
            if(jsonMessage.get(Message.MESSAGE_ID).toString().equals(Message.LOGIN)){
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

    public void setUsername(String usr){
        username = usr;
    }

    public String getUsername(){
        return username;
    }
}
