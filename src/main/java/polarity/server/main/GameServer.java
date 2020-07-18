package polarity.server.main;

import ai.AIManager;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import events.EventManager;
import hud.advanced.FPSCounter;
import input.ServerInputHandler;
import main.GameApplication;
import polarity.server.network.ServerNetwork;
import tools.Sys;
import tools.Util;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
Copyright (c) 2003-2011 jMonkeyEngine
All rights reserved.
 
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
 
Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 
Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
 
Neither the name of 'jMonkeyEngine' nor the names of its contributors 
may be used to endorse or promote products derived from this software 
without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * Game Server
 * @author SinisteRing
 */
public class GameServer extends GameApplication {
    protected ServerInputHandler inputHandler;
    protected ServerNetwork serverNetwork;
    
    protected AIManager aiManager;
    protected EventManager eventManager;

    protected FPSCounter fpsCounter;
    
    // Getters for Nodes:
    public Node getGUI(){
        return gui;
    }
    public Node getRoot(){
        return root;
    }
    public EventManager getEventManager(){
        return eventManager;
    }
    
    public static void main(String[] args){
        main.GameServer app = new main.GameServer();
        app.start(JmeContext.Type.Headless);
    }
    
    @Override
    public void start(){
        Logger.getLogger("com.jme3").setLevel(Level.WARNING);
        settings = new AppSettings(true);
        settings.setSamples(0);
        settings.setVSync(false);
        settings.setFrameRate(64); // Server tickrate
        //settings.setRenderer(AppSettings.LWJGL_OPENGL1);
        settings.setResolution(600, 400);
        settings.setTitle("Reach Server");
        this.setSettings(settings);
        super.start();
    }
    
    @Override
    public void initialize(){
        super.initialize();
        
        // Initialize input handler
        Util.log("[GameServer] <initialize> Creating InputHandler...", 1);
        inputHandler = new ServerInputHandler(this);
        
        // Start server network
        Util.log("[GameServer] <initialize> Starting Network...", 1);
        serverNetwork = new ServerNetwork(this);
        Sys.setNetwork(serverNetwork);
        
        // Custom Initialize
        world.generateStart();
        aiManager = new AIManager();
        eventManager = new EventManager();
        
        fpsCounter = new FPSCounter(gui, new Vector2f(30, Sys.height-30), 30);
    }

    @Override
    public void update() {
        super.update(); // makes sure to execute AppTasks
        if(speed == 0 || paused){   // If the client is paused, do not update.
            return;
        }
        final float tpf = timer.getTimePerFrame() * speed;
        if(timer.getTimePerFrame() >= 1){
            Util.log("Server is chugging: "+timer.getTimePerFrame());
        }
        
        // Update States:
        stateManager.update(tpf);
        
        // Custom updates
        fpsCounter.update(tpf);
        
        enqueue(new Callable<Void>(){
            public Void call() throws Exception{
                aiManager.serverUpdate(getWorld(), charManager.getMonsters(), tpf);
                return null;
            }
        });
        enqueue(new Callable<Void>(){
            public Void call() throws Exception{
                charManager.serverUpdate(getWorld(), tpf);
                return null;
            }
        });
        enqueue(new Callable<Void>(){
            public Void call() throws Exception{
                eventManager.serverUpdate(serverNetwork.getServer(), getWorld(), tpf);
                return null;
            }
        });
        enqueue(new Callable<Void>(){
            public Void call() throws Exception{
                world.serverUpdate(tpf);
                return null;
            }
        });

        // Update logical and geometric states:
        updateNodeStates(tpf);
        
        // Render display:
        renderDisplay(tpf);
    }
    
    @Override
    public void destroy(){
        serverNetwork.stop();
        super.destroy();
    }
}
