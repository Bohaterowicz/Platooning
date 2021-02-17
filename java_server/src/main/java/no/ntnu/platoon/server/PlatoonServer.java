package no.ntnu.platoon.server;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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
            JSONObject temp = new JSONObject();
            createNewPlatoonRoute(null, temp);
            getAllPlatoonRoutes(null, temp);
            threadExecutor = Executors.newCachedThreadPool();
            threadExecutor.execute(this::connectionThread);
            threadExecutor.execute(this::readMessagesThread);
        } catch(Exception e){
            System.out.print(e.getMessage());
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
            System.out.print(e.getMessage());
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
                    //TODO: HANDLE THIS???
                }
                else{
                    newClient.setUsername(initMessage.getString(Message.USERNAME));
                    synchronized (logged_clients){
                        logged_clients.put(newClient.getUsername(), newClient);
                        System.out.print("SUCCESSFUL LOGIN!");
                    }
                    newClient.sendMessage(Message.createConfirmLoginMessage(newClient.getUsername()));
                }
            }
            else{
                //TODO: Wrong user credentials
                newClient.sendMessage(Message.createDenyLoginMessage());
            }
        }
    }

    private void readMessagesThread(){
        TimerTask readMessages = new TimerTask() {
            @Override
            public void run() {
                synchronized (logged_clients){
                    for(Map.Entry<String, ClientService> entry : logged_clients.entrySet()){
                        ClientService currentClient = entry.getValue();
                        try{
                            JSONObject currentMessage = currentClient.readMessages();
                            if(currentMessage != null){
                                switch (currentMessage.get(Message.MESSAGE_ID).toString()){
                                    case "new_platoon_route":
                                        createNewPlatoonRoute(currentClient, currentMessage);
                                        break;
                                    case Message.GET_PLATOON_ROUTES:
                                        getAllPlatoonRoutes(currentClient, currentMessage);
                                        break;
                                }
                            }
                        }catch(IOException e){
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(readMessages, 50L, 50L);
    }

    private void getAllPlatoonRoutes(ClientService currentClient, JSONObject msg) {
        String from = "Oslo";//msg.getString(Message.FROM_LOCATION);
        String towards = "Berlin";// msg.getString(Message.TOWARDS_LOCATION);
        String date = "17-02-2021";//msg.getString(Message.START_DATE);


        ArrayList<JSONObject> platoonRoutes = db.getPlatoonRoutes(from, towards, date);
        //currentClient.sendMessage();
    }

    private void createNewPlatoonRoute(ClientService currentClient, JSONObject msg) {
        String from = "Oslo";
        String towards = "Berlin";
        String driver = "Henry";
        String date = "17-02-2021";
        String time = "10:30:00";
        db.AddPlatoonRoute(from, towards, driver, date, time);
    }

    public static void main(String[] args) { new PlatoonServer(); System.out.print("Running..."); }
}
