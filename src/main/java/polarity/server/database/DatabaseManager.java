package polarity.server.database;

import com.sun.istack.internal.Nullable;
import polarity.shared.tools.Util;

import java.sql.*;

public class DatabaseManager {
    // Database Name & Creation String
    protected static String DATABASE_NAME = "polarity";
    protected static String SQL_CREATE_DATABASE = "CREATE DATABASE "+DATABASE_NAME;
    protected static String SQL_JDBC = "jdbc:mysql://";
    protected static String COLLATION = "utf8_general_ci";

    // Table Names
    public static String TABLE_PLAYERS = "players";
    public static String TABLE_ITEMS = "items";

    // Table Creation Strings
    protected static final String CREATE_TABLE_PLAYERS = "CREATE TABLE " + TABLE_PLAYERS +
            "(id int NOT NULL AUTO_INCREMENT," +
            "name text NOT NULL," +
            "PRIMARY KEY(id)) COLLATE " + COLLATION;
    protected static final String CREATE_TABLE_ITEMS = "CREATE TABLE " + TABLE_ITEMS +
            "(id int NOT NULL AUTO_INCREMENT," +
            "name text NOT NULL," +
            "basetype int NOT NULL DEFAULT 0," + // Base type of the item
            "itemclass int NOT NULL DEFAULT 0," + // Class of the item
            "data1 float NOT NULL DEFALUT 0," +
            "data2 float NOT NULL DEFAULT 0," +
            "data3 float NOT NULL DEFAULT 0," +
            "data4 float NOT NULL DEFAULT 0," +
            "data5 float NOT NULL DEFAULT 0," +
            "data6 float NOT NULL DEFAULT 0," +
            "PRIMARY KEY(id)) COLLATE " + COLLATION;

    // Database Connection Variables
    protected static String DB_IP = "";
    protected static String DB_PORT = "";
    protected static String DB_USER = "";
    protected static String DB_PASSWORD = "";

    /**
     * Checks all the tables that exist in the database to ensure they are properly created.
     */
    protected static void checkTables(){
        checkCreateTable(TABLE_PLAYERS, CREATE_TABLE_PLAYERS);
        checkCreateTable(TABLE_ITEMS, CREATE_TABLE_ITEMS);
    }

    /**
     * Builds a connection string for connecting to the database.
     * @param dbip IP for the database to connect to.
     * @param dbport Port for the database to connect to.
     * @param database Name of database to connect to. Can be left empty if not attempting to query a table within the database.
     * @return String used to connect to the SQL Server.
     */
    private static String buildConnectionString(String dbip, String dbport, String database){
        return SQL_JDBC +
                dbip +
                ":" +
                dbport +
                "/" +
                database;
    }

    /**
     * Simple shorthand method to close a connection without requiring a try/catch.
     * @param conn Connection to close.
     */
    private static void closeConnection(Connection conn){
        try {
            conn.close();
        } catch (SQLException e) {
            Util.log("Failed to close connection.");
            e.printStackTrace();
        }
    }
    /**
     * Attempts to create a connection to the SQL server and the given database.
     * If it fails, it will return a null connection. Null checking must be done.
     * @param database Database to connect to. Can be blank if no database is being connected to.
     * @return Connection instance to the SQL Server and given database.
     */
    @Nullable
    private static Connection createConnection(String database){
        Connection conn;
        try {
            String connString = buildConnectionString(DB_IP, DB_PORT, database);
            conn = DriverManager.getConnection(connString, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            Util.log(e.getMessage()); // We don't want a full stack trace here. Just the reason why it failed.
            return null;
        }
        return conn; // Most likely succeeded.
    }

    /**
     * Creates a connection using the standard database.
     * @return Connection instance to the SQL Server using the standard database.
     */
    @Nullable
    public static Connection createConnection(){
        return createConnection(DATABASE_NAME);
    }

    /**
     * Execute an update query with no parameters.
     * @param query Update query to execute.
     */
    public static void executeUpdate(String query){
        Connection conn = null;
        Statement stmt = null;
        try {
            String connString = buildConnectionString(DB_IP, DB_PORT, DATABASE_NAME);
            conn = DriverManager.getConnection(connString, DB_USER, DB_PASSWORD);
            stmt = conn.createStatement();
            stmt.execute(query);
        } catch (SQLException e) {
            Util.log(e.getMessage());
        } finally {
            // Try closing the statement.
            try{
                if (stmt != null) stmt.close();
            } catch (SQLException ignored) { }
            // Try closing the connection.
            try{
                if (conn != null) conn.close();
            } catch (SQLException ignored) { }
        }
    }

    /**
     * Executes a scalar query and returns the generated key. Cannot take arguments.
     * @param query Query to execute.
     * @return Key generated from the query.
     */
    public static int executeScalarQuery(String query){
        Connection conn = null;
        PreparedStatement ps = null;
        int id = -1;
        try {
            String connString = buildConnectionString(DB_IP, DB_PORT, DATABASE_NAME);
            conn = DriverManager.getConnection(connString, DB_USER, DB_PASSWORD);
            ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            id = Integer.parseInt(rs.getString(1)); // Key comes out as a string, must be parsed to integer.
        } catch (SQLException e) {
            Util.log(e.getMessage());
        } finally {
            // Try to close the statement.
            try {
                if (ps != null) ps.close();
            } catch (SQLException ignored) { }
            // Try to close the connection.
            try {
                if (conn != null) conn.close();
            } catch (SQLException ignored) { }
        }
        return id;
    }

    /**
     * Checks if the given table exists in the MySQL Database.
     * @param table Name of the table to check for.
     * @return True if it exists, false otherwise.
     */
    private static boolean checkTableExists(String table){
        Connection conn = createConnection(DATABASE_NAME);
        if (conn != null) {
            try {
                DatabaseMetaData dbm = conn.getMetaData();
                ResultSet tables = dbm.getTables(null, null, table, null);
                return tables.next(); // True if table exists, false if table doesn't exist.
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }else{
            Util.log(String.format("Connection to check if table \"%s\" exists failed.", table));
        }
        return false;
    }

    /**
     * Does an initialization and connection preparation for the MySQL Database.
     * @param dbip IP to connect to the MySQL Server with.
     * @param dbport Port to connect to the MySQL Server with.
     * @param dbuser Username to connect to the MySQL Server with.
     * @param dbpassword Password to connect to the MySQL Server with.
     * @return True if the connection to the MySQL Server was successful. False otherwise.
     */
    public static boolean connect(String dbip, String dbport, String dbuser, String dbpassword){
        // Set the database variables for continued use throughout the program.
        DB_IP = dbip;
        DB_PORT = dbport;
        DB_USER = dbuser;
        DB_PASSWORD = dbpassword;

        // Attempt an initial connection without a database, so we can check if the database exists.
        //Connection conn = null;

        Connection conn = createConnection("");
        if (conn != null){
            // If the connection succeeds, we know that the SQL connection is good, and the user/password was successful.
            closeConnection(conn);
            Util.log("Successfully connected to SQL.");
        }else{
            // If something went wrong, there is a problem with the SQL connection, username, or password.
            Util.log("Failed to connect to SQL database. Check SQL IP, Port, Username, and Password in the server properties.");
            return false;
        }

        conn = createConnection(DATABASE_NAME);
        if (conn != null){
            // Successfully found the database within the server. Now we move on to checking the tables that exist.
            closeConnection(conn);
            Util.log(String.format("Found the database \"%s\" in the SQL server.", DATABASE_NAME));
        }else{
            // If the connection failed, it's because the database doesn't exist. So now we have to create it.
            Util.log(String.format("Could not find database \"%s\" in the SQL server. Creating it now.", DATABASE_NAME));
            createDatabase();
        }

        // We can now assume that the database exists, so we will proceed to check all the tables and ensure they exist.
        checkTables();

        return true;
    }

    /**
     * Checks if a table exists. If it does not, it will create the table.
     * @param table Name of the table to check for.
     * @param createTableQuery SQL query to execute to create the table.
     */
    protected static void checkCreateTable(String table, String createTableQuery){
        if(checkTableExists(table)){
            Util.log(String.format("Table \"%s\" already exists.", table));
        }else{
            Util.log(String.format("Table \"%s\" does not exist. Creating it now.", table));
            executeUpdate(createTableQuery);
        }
    }

    /**
     * Creates the database used for the game via JDBC SQL connection.
     */
    protected static void createDatabase(){
        Util.log("Executing query to add database...");
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
