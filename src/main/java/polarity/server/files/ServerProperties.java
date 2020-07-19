package polarity.server.files;

import polarity.server.files.vars.ServerVar;
import polarity.shared.files.PropertiesFileManager;
import polarity.shared.netdata.ServerStatusData;

/**
 *
 * @author SinisteRing
 */
public class ServerProperties extends PropertiesFileManager {
    public ServerProperties(String saveFilename){
        super(saveFilename);
        for(ServerVar v : ServerVar.values()){
            vars.put(v.getVar(), v.getValue());
        }
    }
    public void loadSettings(ServerStatusData status){
        status.setServerPlayerData(Boolean.parseBoolean(getVar(ServerVar.ServerPlayerData.getVar())));
        status.setMaxPlayers(Integer.parseInt(getVar(ServerVar.MaxPlayers.getVar())));
        status.setHasPassword(!getVar(ServerVar.Password.getVar()).equals(""));
    }
}
