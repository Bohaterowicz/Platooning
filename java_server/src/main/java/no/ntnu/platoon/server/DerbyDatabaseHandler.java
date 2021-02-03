package no.ntnu.platoon.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
                    "startTime DATE NOT NULL," +
                    "numRegistered int NOT NULL," +
                    "PRIMARY KEY(fromLocation, towardsLocation, startTime))";

            Statement statement = dataseConnection.createStatement();
            statement.execute(sqlQueryCreateUsers);
            statement.execute(sqlQueryCreatePlatoonTable);
            statement.close();
        }catch(SQLException e){
            //TABLE creation failed......... WHY????????????
            System.out.print(e.getMessage());
        }
    }

    public boolean ValidateUserCredentials(String username, String pwdHash){
        return true;
    }
}
