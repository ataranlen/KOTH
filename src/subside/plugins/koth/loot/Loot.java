package subside.plugins.koth.loot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import lombok.Getter;
import subside.plugins.koth.areas.Koth;
import subside.plugins.koth.captureentities.Capper;
import subside.plugins.koth.modules.Lang;
import subside.plugins.koth.utils.JSONSerializable;
import subside.plugins.koth.utils.MessageBuilder;
import subside.plugins.koth.utils.Utils;

/**
 * @author Thomas "SubSide" van den Bulk
 *
 */
public class Loot implements JSONSerializable<Loot> {

    private @Getter Inventory inventory;
    private @Getter String name;
    private @Getter List<String> commands;
    private @Getter Boolean useRandom;
    
    private @Getter LootHandler lootHandler;
    
    public Loot(LootHandler lootHandler){
        this.lootHandler = lootHandler;
        commands = new ArrayList<>();
        useRandom = false;
        inventory = Bukkit.createInventory(null, 54, "Loot chest!");
    }
    
    public Loot(LootHandler lootHandler, String name){
        this(lootHandler);
        setName(name);
    }
    
    public Loot(LootHandler lootHandler, String name, List<String> commands, Boolean random){
        this(lootHandler, name);
        this.commands = commands;
        this.useRandom = random;
    }
    
    public void setName(String title){
        this.name = title;
        Inventory newInv = Bukkit.createInventory(null, 54, createTitle(name));
        if(this.inventory != null){
            for(int i = 0; i < this.inventory.getContents().length; i++){
                newInv.setItem(i, this.inventory.getContents()[i]);
            }
        }
        
        this.inventory = newInv;
    }
    
    public void setRandom(Boolean random) {
    	this.useRandom = random;
    }

    /** Get the title by the loot name
     * 
     * @param name      The name of the loot
     * @return          The marked-up title
     */
    public static String createTitle(String name){
        name = name == null ? "" : name;
        String title = new MessageBuilder(Lang.COMMAND_LOOT_CHEST_TITLE).loot(name).build()[0];
        if (title.length() > 32) title = title.substring(0, 32);
        return title;
    }
    
    public void triggerCommands(Koth koth, Capper<?> capper){
        if(!lootHandler.getPlugin().getConfigHandler().getLoot().isCmdEnabled()){
            return;
        }
        
        if(capper == null){
            return;
        }
        Random rand = new Random();
        if (this.useRandom) {
        	int lootAmount = lootHandler.getPlugin().getKothHandler().getRunningKoth().getLootAmount();
        	List<String> usableCommands = new ArrayList<>();
            for (String command : commands) {
                if (command != null) {
                	usableCommands.add(command);
                }
            }
            if (usableCommands.size() < 1) return;
            for (int x = 0; x < lootAmount; x++) {
                if (usableCommands.size() < 1) {
                    break;
                }
                String command = commands.get(rand.nextInt(commands.size()));
                usableCommands.remove(command);
                sendCommand(command, koth, capper);
            }
        } else {
	        for(String command : commands){
	            sendCommand(command, koth, capper);
	        }
        }
    }
    
    private void sendCommand(String command, Koth koth, Capper<?> capper) {
    	List<Player> players = new ArrayList<>(capper.getAvailablePlayers(koth));
        if(command.contains("%player%")){
            for(Player player : players){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("%player%", player.getName()));
            }
        } else if(command.contains("%faction%")){
        	players.retainAll(capper.getAllOnlinePlayers());
            for(Player player : players){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("%player%", player.getName()));
            }
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
  
    public Loot load(JSONObject obj) {
        this.name = (String)obj.get("name");
        this.setName(this.name);
        
        JSONObject lootItems = (JSONObject)obj.get("items");
        for(Object key : lootItems.keySet()){
            try {
                this.inventory.setItem(Integer.parseInt((String)key), Utils.itemFrom64((String)lootItems.get(key)));
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        
        if(obj.containsKey("commands")){
            JSONArray commands = (JSONArray)obj.get("commands");
            Iterator<?> it = commands.iterator();
            while(it.hasNext()){
                try {
                    this.commands.add((String)it.next());
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        
        if (obj.containsKey("useRandom")) {
        	this.useRandom = (Boolean)obj.get("useRandom");
        }
        
        
        return this;
    }

    @SuppressWarnings("unchecked")
    public JSONObject save(){
        JSONObject obj = new JSONObject();
        obj.put("name", this.name); // name
        obj.put("useRandom", this.useRandom);
        
        if(inventory.getSize() > 0){
            JSONObject lootItems = new JSONObject();
            for (int x = 0; x < 54; x++) {
                ItemStack item = inventory.getItem(x);
                if (item != null) {
                    lootItems.put(x, Utils.itemTo64(item));
                }
            }
            obj.put("items", lootItems); // items
        }
        
        if(commands.size() > 0){
            JSONArray commandz = new JSONArray();
            commandz.addAll(commands);
            
            obj.put("commands", commandz); // commands
        }
        
        return obj;
    }
}
