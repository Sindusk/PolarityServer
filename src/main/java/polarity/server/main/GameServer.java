package polarity.server.main;

import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import polarity.server.database.DatabaseManager;
import polarity.server.events.EventManager;
import polarity.server.files.ServerProperties;
import polarity.server.input.ServerInputHandler;
import polarity.server.monsters.MonsterManager;
import polarity.server.network.ServerNetwork;
import polarity.server.players.PlayerManager;
import polarity.server.world.ServerWorld;
import polarity.shared.ai.AIManager;
import polarity.shared.hud.advanced.FPSCounter;
import polarity.shared.main.GameApplication;
import polarity.shared.netdata.ServerStatusData;
import polarity.shared.tools.Sys;
import polarity.shared.tools.Util;

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
    // Constants:
    private static final String SERVER_PROPERTIES_FILENAME  = "server.properties";

    // Instance variables:
    protected static GameServer Instance = null;
    protected PlayerManager playerManager = new PlayerManager();
    protected MonsterManager monsterManager = new MonsterManager();
    protected ServerInputHandler inputHandler;
    protected ServerNetwork serverNetwork;
    protected ServerProperties properties = new ServerProperties(SERVER_PROPERTIES_FILENAME);
    protected ServerStatusData status = new ServerStatusData();
    
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
    public ServerProperties getProperties(){
        return properties;
    }
    
    public static void main(String[] args){
        Instance = new GameServer();
        Instance.start(JmeContext.Type.Headless);
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
        settings.setTitle("Polarity Server");
        this.setSettings(settings);
        super.start();
    }
    
    @Override
    public void initialize(){
        super.initialize();

        // Initialize properties
        properties.load();
        properties.loadSettings(status);

        // Read database properties and initialize database connection.
        String dbip = properties.getVar("dbip");
        String dbport = properties.getVar("dbport");
        String dbuser = properties.getVar("dbuser");
        String dbpassword = properties.getVar("dbpassword");
        Util.log(String.format("Using database at %s with username %s and password with length %d.", dbip, dbuser, dbpassword.length()));
        boolean connected = DatabaseManager.connect(dbip, dbport, dbuser, dbpassword);
        if (!connected){
            Util.log("Failed to connect to a valid MySQL database. Stopping program now.");
            Instance.stop();
            return;
        }

        // Initialize Player Manager.
        
        // Initialize input handler
        Util.log("[GameServer] <initialize> Creating InputHandler...", 1);
        inputHandler = new ServerInputHandler(this);
        
        // Start server network
        Util.log("[GameServer] <initialize> Starting Network...", 1);
        serverNetwork = new ServerNetwork(this, playerManager, monsterManager);
        Sys.setNetwork(serverNetwork);
        
        // Custom Initialize
        world = new ServerWorld(50);
        Sys.setWorld(world);
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
                aiManager.serverUpdate(getWorld(), monsterManager.getMonsters(), tpf);
                return null;
            }
        });
        enqueue(new Callable<Void>(){
            public Void call() throws Exception{
                playerManager.serverUpdate((ServerWorld)getWorld(), tpf);
                return null;
            }
        });
        enqueue(new Callable<Void>(){
            public Void call() throws Exception{
                monsterManager.serverUpdate((ServerWorld)getWorld(), tpf);
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
