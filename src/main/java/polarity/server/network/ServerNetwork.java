package polarity.server.network;

import com.jme3.math.Vector2f;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Node;
import polarity.server.database.DatabaseManager;
import polarity.server.events.EventChain;
import polarity.server.main.GameServer;
import polarity.shared.character.CharacterManager;
import polarity.shared.character.Player;
import polarity.shared.character.data.MonsterData;
import polarity.shared.character.data.PlayerData;
import polarity.shared.equipment.Equipment;
import polarity.shared.events.ProjectileEvent;
import polarity.shared.files.properties.PlayerProperties;
import polarity.shared.files.properties.vars.PlayerVar;
import polarity.shared.items.Inventory;
import polarity.shared.items.creation.ItemFactory;
import polarity.shared.netdata.*;
import polarity.shared.netdata.requests.ChunkRequest;
import polarity.shared.netdata.testing.MonsterCreateData;
import polarity.shared.netdata.updates.MatrixUpdate;
import polarity.shared.network.GameNetwork;
import polarity.shared.network.NetData;
import polarity.shared.spellforge.SpellMatrix;
import polarity.shared.tools.DevCheats;
import polarity.shared.tools.Util;

import java.io.IOException;
import java.sql.*;
import java.util.concurrent.Callable;

/**
 * 
 * @author Sindusk
 */
public class ServerNetwork extends GameNetwork {
    // SQL Statements
    protected static final String SQL_LOAD_PLAYER = "SELECT * FROM " + DatabaseManager.TABLE_PLAYERS + " WHERE name = ?";
    protected static final String SQL_ADD_PLAYER = "INSERT INTO " + DatabaseManager.TABLE_PLAYERS +
            "(name)" +
            " values " +
            "(?)";

    // Important variables:
    private ServerListener listener = new ServerListener();
    protected final GameServer app;
    protected CharacterManager charManager;
    protected Server server;
    
    public ServerNetwork(GameServer app){
        this.app = app;
        this.charManager = app.getCharManager();
        try {
            server = Network.createServer(6143);
            registerSerials();
            server.addConnectionListener(listener);
            server.start();
        }catch (IOException ex){
            Util.log(ex);
        }
    }
    
    public Server getServer(){
        return server;
    }
    
    private void registerClass(Class c){
        Serializer.registerClass(c);
        server.addMessageListener(listener, c);
    }
    private void registerSerials(){
        for(NetData d : NetData.values()){
            registerClass(d.c);
        }
    }
    
    public void stop(){
        server.close();
    }
    
    public void send(Message m){
        if(server != null){
            server.broadcast(m);
        }
    }
    
    private class ServerListener implements MessageListener<HostedConnection>, ConnectionListener{
        public void connectionAdded(Server server, HostedConnection conn) {
            // Nothing needed here.
        }
        public void connectionRemoved(Server server, HostedConnection conn) {
            int id = charManager.removePlayer(conn);
            if(id == -1){
                return;
            }
            Player p = charManager.getPlayer(id);
            server.broadcast(new DisconnectData(id));
            conn.close("Disconnected");
            Util.log("[Connection Removed] Player "+id+" ("+p.getName()+") has disconnected.");
        }
        
        // HANDSHAKING PROCESS BEGINS
        
        // Starts with version check from ConnectData
        /** Recieved when a player first pings the server, checking if a slot is open.
         * <p>
         * This method determines if the player has the correct version & if there's an open
         * slot in the server before telling the client it's a successful connection.
         * If the checks pass, this will send an ID back to the player for connection.
         * @param d Data from the message.
         */
        private void ConnectMessage(final HostedConnection source, final ConnectData d){
            Util.log("[ServerNetwork] <ConnectMessage> Recieving new connection...", 1);
            if(d.getVersion().equals(app.getVersion())){    // Ensures application versions match
                app.enqueue(new Callable<Void>(){
                    public Void call() throws Exception{
                        int id = charManager.findEmptyPlayerID();    // Find an empty slot for the player, if one exists
                        if(id != -1){ // If an empty slot exists
                            if(app.getProperties().getVar("serverPlayerData").equals("true")){ // If using server-based data.
                                PlayerData pd = null; // Will be loaded with information if the player exists.
                                Connection conn = DatabaseManager.createConnection();
                                if (conn != null){
                                    PreparedStatement stmt = null;
                                    try{
                                        stmt = conn.prepareStatement(SQL_LOAD_PLAYER);
                                        stmt.setString(1, d.getName());
                                        ResultSet rs = stmt.executeQuery();
                                        if (rs.next()){
                                            stmt.close();
                                            conn.close();
                                            Util.log(String.format("Found database entry for player %s.", d.getName()));
                                            pd = new PlayerData(id, rs.getString("name"), new Vector2f(0, 0), new Equipment());
                                        }else{
                                            stmt.close();
                                            conn.close();
                                            Util.log(String.format("No database entry found for player %s. Creating new one now...", d.getName()));
                                            int playerId = createPlayerDatabaseEntry(d.getName());
                                            Util.log(String.format("Added player %s with id %d to the table %s.", d.getName(), playerId, DatabaseManager.TABLE_PLAYERS));
                                        }
                                    }catch (SQLException ignored) { }
                                }else{
                                    Util.log("Failed to connect to database to receive player data.");
                                }
                                if (pd == null){ // If we couldn't find the player in the database, make a new default PlayerData instance.
                                    pd = new PlayerData(id, d.getName(), new Vector2f(0, 0), new Equipment());
                                }
                                Inventory inv = new Inventory();
                                // Adds some randomly generated items to the inventory for testing
                                for(int i = 0; i < 40; i++){
                                    inv.add(ItemFactory.randomItem(inv, (int) Util.scaledRandFloat(1, 100)));
                                }
                                pd.setInventory(inv);
                                source.send(new PlayerConnectionData(id, pd));
                            }else{
                                source.send(new PlayerIDData(id));
                            }
                        }else{
                            source.close("Server is full.");
                        }
                        return null;
                    }
                });
            }else{  // Decline connection if the client version does not match
                Util.log("[Connect Message] ERROR: Client has incorrect version. A player was denied connection.");
                source.close("Invalid Version. [Client: "+d.getVersion()+"] [Server: "+app.getVersion()+"]");
            }
        }

        /**
         * Creates a database entry for the given player name. Returns the generated key.
         * @param name
         * @return
         */
        private int createPlayerDatabaseEntry(String name){
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
        
        // SSPD off - The player sends back his player data for the server to add, then send to all other players
        /**
         * Message is recieved when a player is authenticated for joining the server.
         * <p>
         * This contains all the data for the player themselves, such as character stats, location, etc.
         * @param d The data of the message.
         */
        private void PlayerMessage(final HostedConnection source, final PlayerData d){
            Util.log("[ServerNetwork] <PlayerMessage> Player "+d.getID()+" (v"+app.getVersion()+") connected successfully.");
            app.enqueue(new Callable<Void>(){
                public Void call() throws Exception{
                    Player player = charManager.addPlayer(app.getWorld(), d);
                    player.setConnection(source);
                    player.initializeMatrixArray(new Node());
                    DevCheats.initPlayerMatrix(source, d.getID(), player.getMatrix(0));
                    server.broadcast(Filters.notEqualTo(source), d);
                    app.getWorld().sendData(source);
                    charManager.sendData(source);
                    return null;
                }
            });
        }
        
        // HANDSHAKING PROCESS ENDS
        // SPELL MATRIX
        
        private void MatrixUpdateMessage(final HostedConnection source, final MatrixUpdate d){
            app.enqueue(new Callable<Void>(){
                public Void call() throws Exception{
                    source.send(d);
                    charManager.updateMatrix(d);
                    return null;
                }
            });
        }
        
        // END SPELL MATRIX
        // ACTION
        
        private void ActionMessage(final HostedConnection source, final ActionData d){
            app.enqueue(new Callable<Void>(){
                public Void call() throws Exception{
                    Player owner = charManager.getPlayer(d.getID());
                    SpellMatrix matrix = owner.getMatrix(d.getSlot());
                    EventChain events = new EventChain(d.getStart(), d.getTarget());
                    events.addEvents(matrix.calculateEvents(source, d));
                    app.getEventManager().addEventChain(events);
                    return null;
                }
            });
        }
        
        // END ACTION
        // WORLD
        
        private void ChunkRequestMessage(final HostedConnection source, final ChunkRequest d){
            app.enqueue(new Callable<Void>(){
                public Void call() throws Exception{
                    source.send(app.getWorld().requestChunk(d));
                    return null;
                }
            });
        }
        
        // END WORLD
        // BEGIN CHAT
        
        private void ChatMessage(final HostedConnection source, final ChatMessage d){
            server.broadcast(Filters.notEqualTo(source), d);
            Util.log(charManager.getOwner(d.getOwner()).getName()+": "+d.getMessage());
        }
        
        // END CHAT
        
        /** Recieved when a player moves.
         * <p>
         * This method is mainly used to broadcast the movement
         * @param d Data from the message.
         */
        private void MoveMessage(final HostedConnection source, final MoveData d){
            app.enqueue(new Callable<Void>(){
                public Void call() throws Exception{
                    charManager.updatePlayerLocation(d);
                    server.broadcast(Filters.notEqualTo(source), d);
                    return null;
                }
            });
        }
        /**
         * Ping data is empty, simply used as a timing device.
         * @param d Data from the message.
         */
        private void PingMessage(HostedConnection source, PingData d){
            source.send(d);
        }
        /**
         * Message recieved when a new Projectile is spawned.
         * <p>
         * Holds all data required to reproduce the projectile.
         * @param d The data of the message.
         */
        private void ProjectileMessage(final ProjectileData d){
            app.enqueue(new Callable<Void>(){
                public Void call() throws Exception{
                    app.getWorld().addProjectile(new ProjectileEvent(charManager, d));
                    server.broadcast(d);
                    return null;
                }
            });
        }
        private void SoundMessage(SoundData d){
            server.broadcast(d);
        }
        
        // TESTING
        
        private void MobCreateMessage(final MonsterCreateData d){
            app.enqueue(new Callable<Void>(){
                public Void call() throws Exception{
                    MonsterData data = new MonsterData(charManager.findEmptyMonsterID(), "A Mob", d.getLocation());
                    charManager.addMonster(app.getWorld(), data);
                    server.broadcast(data);
                    return null;
                }
            });
        }
        
        // END TESTING
        
        public void messageReceived(HostedConnection source, Message m) {
            // Handshaking
            if(m instanceof ConnectData){   // Version Checking
                ConnectMessage(source, (ConnectData) m);
            }else if(m instanceof PlayerData){
                PlayerMessage(source, (PlayerData) m);
            // Spell Matrix
            }else if(m instanceof MatrixUpdate){
                MatrixUpdateMessage(source, (MatrixUpdate) m);
            // Actions
            }else if(m instanceof ActionData){
                ActionMessage(source, (ActionData) m);
            // World
            }else if(m instanceof ChatMessage){
                ChatMessage(source, (ChatMessage) m);
            }else if(m instanceof ChunkRequest){
                ChunkRequestMessage(source, (ChunkRequest) m);
            }else if(m instanceof MoveData){
                MoveMessage(source, (MoveData) m);
            }else if(m instanceof PingData){
                PingMessage(source, (PingData) m);
            }else if (m instanceof ProjectileData){
                ProjectileMessage((ProjectileData) m);
            }else if(m instanceof SoundData){
                SoundMessage((SoundData) m);
            // Testing
            }else if(m instanceof MonsterCreateData){
                MobCreateMessage((MonsterCreateData) m);
            }
        }
    }
}
