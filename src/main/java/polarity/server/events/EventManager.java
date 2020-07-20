package polarity.server.events;

import com.jme3.network.Server;
import polarity.shared.world.GameWorld;

import java.util.ArrayList;

/**
 *
 * @author SinisteRing
 */
public class EventManager {
    protected ArrayList<EventChain> chains;
    
    public EventManager(){
        chains = new ArrayList<>();
    }
    
    public void serverUpdate(Server server, GameWorld world, float tpf){
        int i = 0;
        while(i < chains.size()){
            if(!chains.get(i).isFinished()){
                chains.get(i).execute(server, world, tpf);
            }else{
                chains.remove(i);
            }
            i++;
        }
    }
    
    public void addEventChain(EventChain chain){
        chains.add(chain);
    }
}
