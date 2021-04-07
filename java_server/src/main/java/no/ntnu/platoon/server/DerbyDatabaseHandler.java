package no.ntnu.platoon.server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

public class DerbyDatabaseHandler {

    private Connection dataseConnection;

    DerbyDatabaseHandler() throws DerbyDatabaseException {
        String databaseURL = "jdbc:derby:./platoondb";
        try{
            dataseConnection = DriverManager.getConnection(databaseURL);

        }catch(SQLException e){
            if("Database './platoondb' not found.".equals(e.getMessage())){
                try{
                    dataseConnection = DriverManager.getConnection(databaseURL + ";create=true");
                    initializeDatabase();
                }catch(SQLException createE){
                    //TODO: create database failed, logging, error handling
                    throw new DerbyDatabaseException(createE.getMessage());
                }
            }
            else{
                //Could not connect to database...
                //TODO: Error handling
                throw new DerbyDatabaseException(e.getMessage());
            }
        }
    }

    private void initializeDatabase(){
        try{
            final String sqlQueryCreateUsers =
                    "CREATE TABLE users (" +
                            "userName varchar(255) NOT NULL," +
                            "password varchar(255) NOT NULL," +
                            //"email varchar(255) NOT NULL UNIQUE," + //MAYBE DONT INCLUDE EMAIL
                            "PRIMARY KEY(userName))";

            final String sqlQueryCreatePlatoonTable =
                    "CREATE TABLE platoons (" +
                            "fromLocation varchar(255) NOT NULL," +
                            "towardsLocation varchar(255) NOT NULL," +
                            "driver varchar(255) NOT NULL," +
                            "startDate DATE NOT NULL," +
                            "time TIME NOT NULL," +
                            "numRegistered int NOT NULL," +
                            "PRIMARY KEY(fromLocation, towardsLocation, driver, startDate))";

            final String sqlQueryCreateUserRoutes =
                    "CREATE TABLE userRoutes (" +
                            "userName varchar(255) NOT NULL" +
                            "fromLocation varchar(255) NOT NULL," +
                            "towardsLocation varchar(255) NOT NULL," +
                            "driver varchar(255) NOT NULL," +
                            "startDate DATE NOT NULL," +
                            "PRIMARY KEY(userName, fromLocation, towardsLocation, driver, startDate)" +
                            "FOREIGN KEY (fromLocation, towardsLocation, driver, startDate) REFERENCES platoons(fromLocation, towardsLocation, driver, startDate))";

            final String sqlQueryAddUser =
                    "INSERT INTO users (userName, password) " +
                            "VALUES(?, ?)";

            final String sqlAddPlatoon =
                    "INSERT INTO platoons (fromLocation, towardsLocation, driver, startDate, time, numRegistered) " +
                            "VALUES(?, ?, ?, ?, ?, ?)";

            Statement statement = dataseConnection.createStatement();
            statement.execute(sqlQueryCreateUsers);
            statement.execute(sqlQueryCreatePlatoonTable);
            statement.execute(sqlQueryCreateUserRoutes);
            statement.close();

            PreparedStatement newStatement = dataseConnection.prepareStatement(sqlQueryAddUser);
            newStatement.setString(1, "jhon");
            newStatement.setString(2, "admin123");
            newStatement.executeUpdate();
            newStatement.close();

            PreparedStatement newStatement2 = dataseConnection.prepareStatement(sqlAddPlatoon);
            newStatement2.setString(1, "Oslo");
            newStatement2.setString(2, "Munchen");
            newStatement2.setString(3, "Olav");
            java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
            newStatement2.setDate(4, date);
            Time time = new Time(System.currentTimeMillis());
            newStatement2.setTime(5, time);
            newStatement2.setInt(6, 2);
            newStatement2.executeUpdate();
            newStatement2.close();
        }catch(SQLException e){
            //TABLE creation failed......... WHY????????????
            System.out.print(e.getMessage());
        }
    }

    public boolean ValidateUserCredentials(String username, String pwdHash) {
        if (usernameExists(username)) {
            return passwordMatch(username, pwdHash);
        }
        return false;
    }

    public boolean RegisterNewUser(String username, String pwd) {
        if(!usernameExists(username)){
            try{
                final String sqlQuery = "INSERT INTO users (userName, password) " +
                        "VALUES (?, ?)";

                PreparedStatement statement = dataseConnection.prepareStatement(sqlQuery);
                statement.setString(1, username);
                statement.setString(2, pwd);

                statement.executeUpdate();
                statement.close();
                return true;
            } catch(SQLException e){
                System.out.println(e.getMessage());
            }
        }
        return false;
    }

    public boolean usernameExists(String usrname){
        try{
            final String sqlQuery = "SELECT userName FROM users WHERE userName=?";
            PreparedStatement statement = dataseConnection.prepareStatement(sqlQuery);
            statement.setString(1, usrname);
            ResultSet result = statement.executeQuery();
            if(result.next()){
                statement.close();
                return true;
            }
            statement.close();
        } catch(SQLException e){
            System.out.print(e.getMessage());
        }
        return false;
    }

    public boolean passwordMatch(String usrname, String pass){
        try{
            final String sqlQuery = "SELECT userName FROM users WHERE (userName=? AND password=?)";
            PreparedStatement statement = dataseConnection.prepareStatement(sqlQuery);
            statement.setString(1, usrname);
            statement.setString(2, pass);
            ResultSet result = statement.executeQuery();
            if(result.next()){
                statement.close();
                return true;
            }
            statement.close();
        } catch(SQLException e){
            System.out.print(e.getMessage());
        }
        return false;
    }

    public ArrayList<JSONObject> getPlatoonRoutes(String from, String towards, String strDate) {
        ArrayList<JSONObject> resultList = new ArrayList<>();
        try{
            final String sqlQuery = "SELECT * FROM platoons WHERE (fromLocation=? AND towardsLocation=? AND startDate=?)";
            PreparedStatement statement = dataseConnection.prepareStatement(sqlQuery);
            statement.setString(1, from);
            statement.setString(2, towards);
            try{
                SimpleDateFormat formatter=new SimpleDateFormat("dd-MM-yyyy");
                Date dDate = formatter.parse(strDate);
                java.sql.Date date = new java.sql.Date(dDate.getTime());
                statement.setDate(3, date);
                ResultSet result = statement.executeQuery();
                while(result.next()){
                    JSONObject resultObj = new JSONObject();
                    resultObj.put(Message.FROM_LOCATION, result.getString(1));
                    resultObj.put(Message.TOWARDS_LOCATION, result.getString(2));
                    resultObj.put(Message.DRIVER, result.getString(3));
                    Date tempDate = new SimpleDateFormat("yyyy-MM-dd").parse(result.getString(4));
                    resultObj.put(Message.START_DATE, formatter.format(tempDate));
                    resultObj.put(Message.START_TIME, result.getString(5));
                    resultObj.put(Message.REGISTERED_COUNT, result.getString(6));
                    resultList.add(resultObj);
                }
            }catch(ParseException e){
                System.out.print(e.getMessage());
                //TODO: ERROR HANDLING
            }
        } catch(SQLException e){
            System.out.print(e.getMessage());
        }
        return resultList;
    }

    public boolean AddPlatoonRoute(String from, String towards, String driver, String strDate, String strTime) {
        boolean Result = false;
        try{
            final String sqlAddPlatoon =
                    "INSERT INTO platoons (fromLocation, towardsLocation, driver, startDate, time, numRegistered) " +
                            "VALUES(?, ?, ?, ?, ?, ?)";

            PreparedStatement Statement = dataseConnection.prepareStatement(sqlAddPlatoon);
            Statement.setString(1, from);
            Statement.setString(2, towards);
            Statement.setString(3, driver);
            SimpleDateFormat formatter=new SimpleDateFormat("dd-MM-yyyy");
            Date dDate = formatter.parse(strDate);
            java.sql.Date date = new java.sql.Date(dDate.getTime());
            Statement.setDate(4, date);
            Time time = Time.valueOf(strTime);
            Statement.setTime(5, time);
            Statement.setInt(6, 0);
            Statement.executeUpdate();
            Statement.close();
            Result = true;
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return Result;
    }

    public JSONObject GetPlatoonRoute(String from, String towards, String leader, String date) {
        JSONObject RouteResult = new JSONObject();
        try{
            final String sqlQuery =
                    "SELECT * FROM platoons WHERE (fromLocation=? AND towardsLocation=? AND startDate=? AND driver=?)";
            PreparedStatement Statement = dataseConnection.prepareStatement(sqlQuery);
            Statement.setString(1, from);
            Statement.setString(2, towards);
            Statement.setString(3, date);
            Statement.setString(4, leader);
            ResultSet result = Statement.executeQuery();
            Statement.close();
            while(result.next()){
                RouteResult.put(Message.FROM_LOCATION, result.getString(1));
                RouteResult.put(Message.TOWARDS_LOCATION, result.getString(2));
                RouteResult.put(Message.DRIVER, result.getString(3));
                RouteResult.put(Message.START_DATE, result.getString(4));
                RouteResult.put(Message.START_TIME, result.getString(5));
                RouteResult.put(Message.REGISTERED_COUNT, result.getInt(6));
            }
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return RouteResult;
    }

    public void JoinPlatoonRoute(String username, String from, String towards, String leader, String date) {
        try{
            final String sqlQuery = "UPDATE platoons SET numRegistered+=1 WHERE (fromLocation=? AND towardsLocation=? AND startDate=? AND driver=?)";
            final String sqlAddUserRoute = "INSERT INTO userRoutes (userName, fromLocation, towardsLocation, driver, startDate)" +
                    "VALUES(?, ?, ?, ?, ?)";
            PreparedStatement Statement = dataseConnection.prepareStatement(sqlQuery);
            Statement.setString(1, from);
            Statement.setString(2, towards);
            Statement.setString(3, date);
            Statement.setString(4, leader);
            Statement.executeUpdate();
            Statement.close();
            PreparedStatement Statement2 = dataseConnection.prepareStatement(sqlAddUserRoute);
            Statement2.setString(1, username);
            Statement2.setString(2, from);
            Statement2.setString(3, towards);
            Statement2.setString(4, leader);
            Statement2.setString(5, date);
            Statement2.executeUpdate();
            Statement2.close();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<JSONObject> GetUserRoutes(String username) {
        try{
            final String sqlQuery = "SELECT * FROM userRoutes WHERE(userName=?)";
            PreparedStatement Statement = dataseConnection.prepareStatement(sqlQuery);
            Statement.setString(1, username);
            ResultSet Result = Statement.executeQuery();
            ArrayList<JSONObject> Routes = new ArrayList<>();
            while (Result.next()){
                JSONObject Route = GetPlatoonRoute(Result.getString(1), Result.getString(2), Result.getString(3), Result.getString(4));
                Routes.add(Route);
            }
            return Routes;
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return null;
    }
}
