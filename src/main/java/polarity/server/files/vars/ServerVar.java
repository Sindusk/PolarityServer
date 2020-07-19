package polarity.server.files.vars;

/**
 *
 * @author SinisteRing
 */
public enum ServerVar {
    ServerPlayerData("serverPlayerData", "true"),
    MaxPlayers("maxPlayers", "10"),
    Password("password", ""),
    DatabaseIp("dbip", "127.0.0.1"),
    DatabasePort("dbport", "3306"),
    DatabaseUser("dbuser", "user"),
    DatabasePassword("dbpassword", "password");
    
    protected String var;
    protected String value;
    ServerVar(String var, String value){
        this.var = var;
        this.value = value;
    }
    public String getVar(){
        return var;
    }
    public String getValue(){
        return value;
    }
}
