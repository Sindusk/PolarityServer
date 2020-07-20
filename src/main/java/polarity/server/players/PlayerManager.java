package polarity.server.players;

import com.jme3.network.HostedConnection;
import polarity.server.database.DatabaseManager;
import polarity.server.world.ServerWorld;
import polarity.shared.character.Monster;
import polarity.shared.character.Player;
import polarity.shared.players.PlayerMediator;
import polarity.shared.tools.Util;

import java.sql.*;
import java.util.HashMap;

public class PlayerManager extends PlayerMediator {
    // SQL Statements
    public static final String SQL_LOAD_PLAYER = "SELECT * FROM " + DatabaseManager.TABLE_PLAYERS + " WHERE name = ?";
    protected static final String SQL_ADD_PLAYER = "INSERT INTO " + DatabaseManager.TABLE_PLAYERS +
            "(name)" +
            " values " +
            "(?)";

    public PlayerManager(){
        super(); // Call PlayerMediator constructor.
    }

    // TODO: Prevent copy with MonsterManager.
    // Finds an empty ID slot for the given idList.
    public int findEmptyID(HashMap<Integer,Integer> idList, int limit){
        int i = 0;
        while(i < limit || limit == -1){
            if(!idList.containsKey(i)){
                return i;
            }
            i++;
        }
        return -1;
    }
    // TODO: Should be limited by server max players
    public int findEmptyPlayerID(){
        return findEmptyID(playerID, 9999);
    }

    /**
     * Creates a database entry for the given player name. Returns the generated key.
     * @param name
     * @return
     */
    public static int createPlayerDatabaseEntry(String name){
        Connection conn = DatabaseManager.createConnection();
        PreparedStatement ps = null;
        int id = -1;
        try {
            ps = conn.prepareStatement(SQL_ADD_PLAYER, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name); // Set the player name.
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            id = Integer.parseInt(rs.getString(1));
        } catch (SQLException e) {
            Util.log(String.format("Failed to insert player with name %s into database: %s", name, e.getMessage()));
        } finally {
            // Try closing the prepared statement.
            try {
                if (ps != null) ps.close();
            } catch (SQLException ignored) { }
            // Try closing the connection.
            try {
                if (conn != null) conn.close();
            } catch (SQLException ignored) { }
        }
        return id;
    }

    /**
     * Sends all player data to the passed in connection.
     * @param conn The connection of the player that will recieve the data.
     */
    public void sendPlayerData(HostedConnection conn){
        for(Player player : players){
            if(player.isConnected() && player.getConnection() != conn){
                conn.send(player.getData());
            }
        }
    }

    /**
     * Update loop for players on the server.
     * @param world
     * @param tpf
     */
    public void serverUpdate(ServerWorld world, float tpf){
        for(Player player : players){
            player.serverUpdate(tpf);
        }
    }
}
