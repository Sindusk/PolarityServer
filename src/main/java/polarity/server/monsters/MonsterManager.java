package polarity.server.monsters;

import com.jme3.network.HostedConnection;
import polarity.server.world.ServerWorld;
import polarity.shared.character.Monster;
import polarity.shared.monsters.MonsterMediator;

import java.util.HashMap;

public class MonsterManager extends MonsterMediator {
    public MonsterManager(){
        super(); // Call MonsterMediator constructor.
    }

    // TODO: Prevent copy with PlayerManager.
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
    public int findEmptyMonsterID(){
        return findEmptyID(monsterID, 9999);
    }

    public void sendMonsterData(HostedConnection conn){
        for(Monster monster : monsters){
            conn.send(monster.getData());
        }
    }

    public void serverUpdate(ServerWorld world, float tpf){
        for(Monster monster : monsters){
            monster.serverUpdate(world, tpf);
        }
    }
}
