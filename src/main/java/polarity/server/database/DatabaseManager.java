package polarity.server.database;

import polarity.shared.tools.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    protected static String DATABASE_NAME = "polarity";
    protected static String SQL_CREATE_DATABASE = "CREATE DATABASE "+DATABASE_NAME;

    protected static String DB_IP = "";
    protected static String DB_PORT = "";
    protected static String DB_USER = "";
    protected static String DB_PASSWORD = "";

    /**
     * Builds a connection string for connecting to the database.
     * @param dbip IP for the database to connect to.
     * @param dbport Port for the database to connect to.
     * @param database Name of database to connect to. Can be left empty if not attempting to query a table within the database.
     * @return String used to connect to the SQL Server.
     */
    private static String buildConnectionString(String dbip, String dbport, String database){
        return "jdbc:mysql://" +
                dbip +
                ":" +
                dbport +
                "/" +
                database;
    }
    public static boolean connect(String dbip, String dbport, String dbuser, String dbpassword){
        // Set the database variables for continued use throughout the program.
        DB_IP = dbip;
        DB_PORT = dbport;
        DB_USER = dbuser;
        DB_PASSWORD = dbpassword;

        // Attempt an initial connection without a database, so we can check if the database exists.
        Connection conn = null;
        try {
            String connString = buildConnectionString(dbip, dbport, "");
            conn = DriverManager.getConnection(connString, dbuser, dbpassword);

            // If the connection succeeds, we know that the SQL connection is good, and the user/password was successful.
            Util.log("Successfully connected to SQL.");
        } catch (SQLException e) {
            // If something went wrong, there is a problem with the SQL connection, username, or password.
            Util.log("Failed to connect to SQL database. Check SQL IP, Port, Username, and Password in the server properties.");
            e.printStackTrace();
            return false;
        } finally {
            // Try closing connection.
            try{
                if (conn != null) conn.close();
            } catch (SQLException ignored) { }
        }

        // Now that we know the connection is fine, we test if the database exists in the SQL server.
        try {
            String connString = buildConnectionString(dbip, dbport, DATABASE_NAME);
            conn = DriverManager.getConnection(connString, dbuser, dbpassword);

            // Successfully found the database within the server. Now we move on to checking the tables that exist.
            Util.log(String.format("Found the database \"%s\" in the SQL server.", DATABASE_NAME));
        } catch (SQLException e) {
            // If the connection failed, it's because the database doesn't exist. So now we have to create it.
            Util.log(String.format("Could not find database \"%s\" in the SQL server. Creating it now.", DATABASE_NAME));
            createDatabase();
        } finally {
            // Try closing the connection.
            try{
                if (conn != null) conn.close();
            } catch (SQLException ignored) { }
        }

        // We can now assume that the database exists, so we will proceed to check all the tables and ensure they exist.
        checkTables();

        return true;
    }

    protected static void checkTables(){
        //
    }

    /**
     * Creates the database used for the game via JDBC SQL connection.
     */
    protected static void createDatabase(){
        Connection conn = null;
        Statement stmt = null;
        try {
            // Connect without specifying a database, since we're going to be creating a new one.
            String connString = buildConnectionString(DB_IP, DB_PORT, "");
            conn = DriverManager.getConnection(connString, DB_USER, DB_PASSWORD);
            stmt = conn.createStatement();
            stmt.executeUpdate(SQL_CREATE_DATABASE);

            // If the execute fails, we wont get past this line.
            Util.log(String.format("Successfully created database \"%s\" in MySQL Server.", DATABASE_NAME));
        } catch (SQLException e) {
            Util.log(String.format("Failed to create database \"%s\" in MySQL Server. Check user permissions.", DATABASE_NAME));
            e.printStackTrace();
        } finally {
            // Try closing statement.
            try {
                if(stmt != null) stmt.close();
            } catch (SQLException ignored) { }
            // Try closing connection.
            try{
                if (conn != null) conn.close();
            } catch (SQLException ignored) { }
        }
    }
}
