package polarity.server.input;

import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import polarity.shared.input.Bind;
import polarity.shared.input.InputHandler;
import polarity.shared.main.GameApplication;

/**
 * ClientInputHandler - Handles all polarity.shared.input from users and organizes them based on conditions.
 * @author SinisteRing
 */
public class ServerInputHandler extends InputHandler implements ActionListener, AnalogListener{
    // Initialization
    public ServerInputHandler(GameApplication app){
        super(app);
    }
    public void setupInputs(){
        for(Bind bind : Bind.values()){
            app.getInputManager().addMapping(bind.mapping, bind.trigger);
            app.getInputManager().addListener(this, bind.mapping);
        }
    }
    
    // Action handlers
    public void onAction(String bind, boolean down, float tpf){
        if(screen == null){
            return;
        }
        screen.onAction(app.getInputManager().getCursorPosition(), bind, down, tpf);
    }
    public void onAnalog(String name, float value, float tpf){
        if(screen == null){
            return;
        }
        screen.onCursorMove(app.getInputManager().getCursorPosition());
    }
}
