package no.ntnu.platoon.server;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlatoonServer {
    private ExecutorService threadExecutor;
    private DerbyDatabaseHandler db;
    private boolean running = false;

    private static final int CONNECTION_PORT = 4567;

    private ConcurrentHashMap<String, ClientService> logged_clients = new ConcurrentHashMap<>();

    private PlatoonServer() {
        running = true;
        try{
            db = new DerbyDatabaseHandler();
            threadExecutor = Executors.newCachedThreadPool();
            threadExecutor.execute(this::connectionThread);
        } catch(Exception e){

        }
    }

    private void connectionThread(){
        try {
            ServerSocket connectListener = new ServerSocket(CONNECTION_PORT);
            while(running){
                System.out.print("Awaiting connection...");
                Socket connectedSocket = connectListener.accept();
                try{
                    connectClient(connectedSocket);
                } catch(IOException e){
                    System.out.print("Error");
                }
            }
        } catch(IOException e){

        }
    }

    private void connectClient(Socket socket) throws IOException {
        final ClientService newClient = new ClientService(socket);

        JSONObject initMessage = newClient.initialConnectionMessage();
        if(initMessage == null){
            return;
        }

        if(initMessage.get(Message.MESSAGE_ID).toString().equals(Message.LOGIN)){
            //TODO: Login Token?
            if(db.ValidateUserCredentials(initMessage.get(Message.USERNAME).toString(), initMessage.get(Message.PWD_HASH).toString())){
                if(logged_clients.containsKey(initMessage.getString(Message.USERNAME))){
                    //already logged in
                }
                else{
                    newClient.setUsername(initMessage.getString(Message.USERNAME));
                    synchronized (logged_clients){
                        logged_clients.put(newClient.getUsername(), newClient);
                    }
                }
            }
            else{
                //TODO: Wrong user credentials
            }
        }
    }

    public static void main(String[] args) { new PlatoonServer(); System.out.print("Running..."); }
}
