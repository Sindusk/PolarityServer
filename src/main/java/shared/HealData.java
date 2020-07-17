package shared;

//import character.types.CharType;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Sindusk
 */
@Serializable
public class HealData extends AbstractMessage {
    protected int id;
    //protected CharType type;
    protected float value;
    public HealData(){}
    public HealData(int id, /*CharType type,*/ float value){
        this.id = id;
        //this.type = type;
        this.value = value;
        setReliable(false);
    }
    public int getID(){
        return id;
    }
    //public CharType getType(){
        //return type;
    //}
    public float getValue(){
        return value;
    }
}
