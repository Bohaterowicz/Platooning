package no.ntnu.platoon.server;

import org.json.JSONObject;

public class Message {
    public static final String MESSAGE_ID = "message_id";
    public static final String LOGIN = "login";
    public static final String USERNAME = "username";
    public static final String PWD_HASH = "pwd";
    public static final String GET_PLATOON_ROUTES = "get_platoon_routes";
    public static final String FROM_LOCATION = "from_location";
    public static final String TOWARDS_LOCATION = "towards_location";
    public static final String START_DATE = "start_date";
    public static final String START_TIME = "start_time";
    public static final String DRIVER = "driver";
    public static final String REGISTERED_COUNT = "registered_count";

    public static String createConfirmLoginMessage(String user){
        JSONObject tmpObj = new JSONObject();
        tmpObj.put(MESSAGE_ID, "login_OK");
        tmpObj.put(USERNAME, user);

        return tmpObj.toString();
    }

    public static String createDenyLoginMessage() {
        JSONObject tmpObj = new JSONObject();
        tmpObj.put(MESSAGE_ID, "login_DENY");

        return tmpObj.toString();
    }
}
