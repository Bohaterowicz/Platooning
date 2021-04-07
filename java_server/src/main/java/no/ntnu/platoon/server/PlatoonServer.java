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
            db.AddPlatoonRoute("Oslo", "Munich", "Olav", "17-02-2021", "10:30:00");
            db.AddPlatoonRoute("Oslo", "Munich", "Jhon", "17-02-2021", "12:30:00");
            db.AddPlatoonRoute("Trondheim", "Berlin", "Per", "15-11-2021", "12:30:00");
            ArrayList<JSONObject> platoonRoutes = db.getPlatoonRoutes("Oslo", "Munich", "17-02-2021");
            JSONObject result = new JSONObject();
            result.put(Message.MESSAGE_ID, "platoon_routes_result");
            result.put("routes", platoonRoutes);
            String PlatoonRoutesResult = result.toString();
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
        System.out.println("New Connection...");
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
        else if(initMessage.get(Message.MESSAGE_ID).toString().equals(Message.REGISTER)){
            if(db.RegisterNewUser(initMessage.getString(Message.USERNAME), initMessage.getString(Message.PWD_HASH))){
                newClient.setUsername(initMessage.getString(Message.USERNAME));
                synchronized (logged_clients){
                    logged_clients.put(newClient.getUsername(), newClient);
                    System.out.println("user registered and logged in!");
                }
                newClient.sendMessage(Message.createConfirmRegisterMessage(newClient.getUsername()));
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
                                    case "join_platoon_route":
                                        JoinPlatoonRoute(currentClient, currentMessage);
                                        break;
                                    case "get_user_routes":
                                        GetUserRoutes(currentClient, currentMessage);
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
        JSONObject result = new JSONObject();
        result.put(Message.MESSAGE_ID, "platoon_routes_result");
        result.put("routes", platoonRoutes);
        String PlatoonRoutesResult = result.toString();
        try{
            currentClient.sendMessage(PlatoonRoutesResult);
        }catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createNewPlatoonRoute(ClientService currentClient, JSONObject msg) {
        String from = msg.getString(Message.FROM_LOCATION);
        String towards = msg.getString(Message.TOWARDS_LOCATION);
        String driver = msg.getString(Message.DRIVER);
        String date = "17-02-2021"; //msg.getString(Message.START_DATE);
        String time = "10:30:00"; //msg.getString(Message.START_TIME);
        boolean AddResult = db.AddPlatoonRoute(from, towards, driver, date, time);
        JSONObject JAddResult = new JSONObject();
        JAddResult.put(Message.MESSAGE_ID, "new_platoon_route_result");
        JAddResult.put("result", AddResult ? "1":"0");
        try{
            currentClient.sendMessage(JAddResult.toString());
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    private void JoinPlatoonRoute(ClientService currentClient, JSONObject msg){
        String leader = msg.getString(Message.DRIVER);
        String date = msg.getString(Message.START_DATE);
        String from = msg.getString(Message.FROM_LOCATION);
        String towards = msg.getString(Message.TOWARDS_LOCATION);
        boolean Result = false;
        JSONObject Route = db.GetPlatoonRoute(from, towards, leader, date);
        if(Route.getInt(Message.REGISTERED_COUNT) < 5){
            db.JoinPlatoonRoute(currentClient.getUsername(), from, towards, leader, date);
            Result = true;
        }
        else{
            //Max number of driver in this route
        }
        JSONObject JAddResult = new JSONObject();
        JAddResult.put(Message.MESSAGE_ID, "join_platoon_route_result");
        JAddResult.put("result", Result ? "1":"0");
        try{
            currentClient.sendMessage(JAddResult.toString());
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    private void GetUserRoutes(ClientService currentClient, JSONObject msg){
        String username = msg.getString(Message.USERNAME);
        ArrayList<JSONObject> UserRoutes = db.GetUserRoutes(username);
        JSONObject JSONRoutes = new JSONObject(UserRoutes);
        try{
            currentClient.sendMessage(JSONRoutes.toString());
        }
        catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) { new PlatoonServer(); System.out.print("Running... (ver. 0.2)"); }
}
