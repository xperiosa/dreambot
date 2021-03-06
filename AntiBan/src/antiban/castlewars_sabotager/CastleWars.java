/*
 * Copyright (c) 2021, 7ctx <https://github.com/7ctx/> 
 * Email: <7ctx@mail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package antiban.castlewars_sabotager;

import java.util.concurrent.atomic.AtomicInteger;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Map;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.wrappers.interactive.Entity;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.WidgetChild;

public abstract class CastleWars extends AbstractScript 
{
    private final AtomicInteger gameCount = new AtomicInteger();
    
    /**
     * Game count getter
     * 
     * @return gameCount
     */
    public int getGameCount() 
    {
        return gameCount.get();
    }
    
    /**
     * Is in lobby boolean
     *
     * @return in lobby
     */
    public boolean inLobby() 
    {
        return inArea(Locations.castlewarsLobby.getArea());
    }

    /**
     * Is in waiting room boolean
     *
     * @return in waiting room
     */
    public boolean inWaitingRoom() 
    {
        return inArea(Locations.saradominWaitingRoom.getArea());
    }

    /**
     * Is in respawn room boolean
     *
     * @return in respawn room
     */
    public boolean inRespawnRoom() 
    {
        WidgetChild saradominRespawnRoomWidget = Widgets.getWidgetChild(58, 24);
        return inArea(Locations.saradominRespawnRoom.getArea()) || saradominRespawnRoomWidget != null && saradominRespawnRoomWidget.getText().contains("minutes to leave the respawn room.");
    }

    /**
     * Is on first floor
     *
     * @return on first floor
     */
    public boolean onFirstFloor() 
    {
        return inArea(Locations.saradominFirstFloor.getArea());
    }

    /**
     * Is in supply area boolean
     *
     * @return in supply area
     */
    public boolean inSupplyArea() 
    {
        return inArea(Locations.saradominSupply.getArea());
    }

    /**
     * Game started boolean
     *
     * @return gameStarted
     */
    public boolean gameStarted() 
    {
        WidgetChild saradominGameTimeWidget = Widgets.getWidgetChild(58, 25);
        return saradominGameTimeWidget != null;
    }

    /**
     * onGameStart
     */
    public void onGameStart() 
    {
        log("CastleWars game has been started");
    }

    /**
     * onGameEnd
     */
    public void onGameEnd() 
    {
        log("CastleWars game has ended");
        gameCount.incrementAndGet();
    }

    /**
     * Is in area boolean
     */
    private boolean inArea(Area area) 
    {
        return area.contains(getLocalPlayer().getTile());
    }

    /**
     * Enter saradomin waiting room
     */
    public void enterWaitingRoom() 
    {
        log("State = ENTER_WAIT_ROOM");
        GameObject saradominPortal = GameObjects.closest(gameObject -> gameObject != null && gameObject.getName().equals("Saradomin Portal") && gameObject.hasAction("Enter"));
        
        if (saradominPortal == null) 
        {
            return;
        }
        
        saradominPortal.interact();
        sleepUntil(() -> inWaitingRoom() || !inLobby(), Calculations.random(300, 600));
    }

    /**
     * Wait until next game
     */
    public void waitUntilGame() 
    {
        log("State = WAIT");
        Camera.rotateToEntity(getLocalPlayer()); // Stay logged in
        sleepUntil(() -> inRespawnRoom(), 120000); // 2 minutes?
        if (inRespawnRoom()) 
        {
            this.onGameStart();
        }
    }

    /**
     * Leave respawn room
     */
    public void leaveRespawnRoom() 
    {
        log("State = LEAVE_RESPAWN");
        GameObject energyBarrier = GameObjects.closest(gameObject -> gameObject != null
                && gameObject.getName().equals("Energy Barrier")
                && gameObject.hasAction("Pass")
                && gameObject.getX() == 2422
                && gameObject.getY() == 3076);
        
        if (energyBarrier == null) 
        {
            return;
        }
        
        Mouse.click(energyBarrier);
        energyBarrier.interactForceRight("Pass");
        sleepUntil(() -> onFirstFloor(), 5000);
    }

    /**
     * Leave first floor
     */
    public void leaveFirstFloor() 
    {
        log("State = LEAVE_FIRST_FLOOR");
        GameObject Ladder = GameObjects.closest(gameObject -> gameObject != null
                && gameObject.getName().equals("Ladder")
                && gameObject.hasAction("Climb-down"));
        
        if (Ladder == null) 
        {
            return;
        }
        
        Ladder.interact();
        sleepUntil(() -> !onFirstFloor(), 3000);
    }

    /**
     * Grab explosive potions
     */
    public void grabExplosives() 
    {
        log("State = GRAB_EXPLOSIVES");
        GameObject explosiveTable = GameObjects.closest(gameObject -> gameObject != null
                && gameObject.getX() == 2426
                && gameObject.getY() == 3075);

        // Walk to saradomin supply
        if (!inSupplyArea()) 
        {
            Walking.walk(Locations.saradominSupply.getArea().getCenter());
            if (Inventory.contains(Item -> Item.getName().equals("Barricade"))) 
            {
                Inventory.get("Barricade").interact();
            }
            return;
        }
        
        if (explosiveTable == null) 
        {
            return;
        }
        
        while (Inventory.count("Explosive potion") < 3 && inSupplyArea()) 
        {
            explosiveTable.interact();
        }
    }
    
    /**
     * Grab tinderbox
     */
    public void grabTinderbox() 
    {
        log("State = GRAB_TINDERBOX");
        GroundItem tinderbox = GroundItems.closest("Tinderbox");
        if (tinderbox != null) 
        {
            tinderbox.interactForceRight("Take");
        }
    }

    /**
     * Grab barricades
     */
    public void grabBarricades() 
    {
        log("State = GRAB_BARRICADES");
        GameObject barricadeTable = GameObjects.closest(gameObject -> gameObject != null
                && gameObject.getX() == 2429
                && gameObject.getY() == 3073);

        // Walk to saradomin supply
        if (!inSupplyArea()) 
        {
            Walking.walk(Locations.saradominSupply.getArea().getCenter());
            return;
        }
        
        if (barricadeTable == null) 
        {
            return;
        }
        
        // Grab
        while (Inventory.count("Barricade") < 22 && inSupplyArea()) 
        {
            barricadeTable.interactForceRight("Take-5");
        }
    }

    /**
     * Setup barricades
     */
    public void setupBarricades() 
    {
        log("State = SETUP_BARRICADES");

        //Walking.walk(myTile.getRandomizedTile(7));
        //Tile dst = getLocalPlayer().getTile().getArea(50).getRandomTile();
        
        Tile dst = Locations.barricadeSetupArea.getArea().getRandomTile();
        if (Map.canReach(dst)) 
        {
            Walking.walk(dst);
            sleep(Calculations.random(815, 900));
        }
        
        /*if (!Locations.barricadeSetupArea.getArea().contains(getLocalPlayer())) 
        {
            Walking.walk(Locations.barricadeSetupArea.getArea().getCenter());
        }
        
        LocalPath path = new LocalPath();
        ArrayList<Tile> tiles = new ArrayList<>();
        tiles.add(new Tile(2402, 3072, 0).getRandomizedTile());
        tiles.add(new Tile(2401, 3072, 0).getRandomizedTile());
        tiles.add(new Tile(2400, 3072, 0).getRandomizedTile());
        path.addAll(tiles);
        path.setDirection(PathDirection.FORWARD);
        path.walk();*/
        
        //Walking.walk(path.next().getTile());
        if (Inventory.contains(Item -> Item.getName().equals("Barricade"))) 
        {
            Inventory.get("Barricade").interact();
        }
    }

    /**
     * Use explosive potions on barricade nearby
     */
    public void useExplosive() 
    {
        NPC barricade = NPCs.closest(n -> n.getName().equals("Barricade")
                && n.distance(getLocalPlayer()) <= 1 && n.getLocalX() == getLocalPlayer().getLocalX() && n.getLocalY() != getLocalPlayer().getLocalY()
                || n.getName().equals("Barricade") && n.getLocalY() == getLocalPlayer().getLocalY() && n.getLocalX() != getLocalPlayer().getLocalX());
        if (barricade == null) 
        {
            return;
        }
        Inventory.get(Item -> Item.getName().equals("Explosive potion")).useOn(barricade);
        sleep(Calculations.random(700, 900));
    }
    
    /**
     * Use tinderbox on barricade nearby
     */
    public void useTinderbox() 
    {
        NPC barricade = NPCs.closest(n -> n.getName().equals("Barricade")
                && n.distance(getLocalPlayer()) <= 1 && n.getLocalX() == getLocalPlayer().getLocalX() && n.getLocalY() != getLocalPlayer().getLocalY()
                || n.getName().equals("Barricade") && n.getLocalY() == getLocalPlayer().getLocalY() && n.getLocalX() != getLocalPlayer().getLocalX());
        if (barricade == null) 
        {
            return;
        }
        Inventory.get(item -> item.getName().equals("Tinderbox")).useOn(barricade);
        sleep(Calculations.random(700, 900));
    }
    
    /**
     * Use a explosive potion on entity
     * 
     * @param entity 
     */
    public void useExplosiveOn(Entity entity) 
    {
        Item explosivePotion = Inventory.get(Item -> Item.getName().equals("Explosive potion"));
        if (explosivePotion == null) 
        {
            return;
        }
        explosivePotion.interact();
        Mouse.click(entity);
    }
}
