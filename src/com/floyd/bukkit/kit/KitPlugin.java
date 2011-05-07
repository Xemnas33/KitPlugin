package com.floyd.bukkit.kit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.iConomy.*;
import com.iConomy.system.Holdings;

import com.nijikokun.bukkit.Permissions.Permissions;

/**
* Kit plugin for Bukkit
*
* @author FloydATC
*/
public class KitPlugin extends JavaPlugin {
    String path = "plugins/KitPlugin";

    private CopyOnWriteArrayList<KitObject> kits = new CopyOnWriteArrayList<KitObject>();
    
    public static Permissions Permissions = null;
    public static iConomy iConomy = null;
  public static final Logger logger = Logger.getLogger("Minecraft.KitPlugin");

//    public KitPlugin(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
//        super(pluginLoader, instance, desc, folder, plugin, cLoader);
//        // TODO: Place any custom initialization code here
//
//        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
//    }

    @SuppressWarnings("static-access")
	public void onDisable() {
        // TODO: Place any custom disable code here
        kits.clear();
        if (this.iConomy != null) {
                this.iConomy = null;
                System.out.println("[MyPlugin] un-hooked from iConomy.");
        }

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
      
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
      PluginDescriptionFile pdfFile = this.getDescription();
      logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

	@SuppressWarnings("static-access")
	public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

      loadKits();
      setupPermissions();
      if (this.iConomy == null) {
          Plugin iConomy = this.getServer().getPluginManager().getPlugin("iConomy");

          if (iConomy != null) {
              if (iConomy.isEnabled() && iConomy.getClass().getName().equals("com.iConomy.iConomy")) {
                  this.iConomy = (iConomy)iConomy;
                  System.out.println("[Kit] hooked into iConomy.");
              }
          }
      }
      
        // Register our events
        @SuppressWarnings("unused")
		PluginManager pm = getServer().getPluginManager();

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }


    @SuppressWarnings("static-access")
	public void setupPermissions() {
      Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

      if(this.Permissions == null) {
          if(test != null) {
            this.Permissions = (Permissions)test;
            logger.info( "[Kit] Permission system detected. Good." );
          } else {
            logger.info( "[Kit] Permission system not enabled. Disabling plugin." );
            this.getServer().getPluginManager().disablePlugin(this);
          }
      }
    }

    @SuppressWarnings({ "static-access", "unused" })
	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
      String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        String pname = "(Console)";
        if (sender instanceof Player) {
          player = (Player)sender;
          pname = player.getName();
        }
             
        //System.out.println("[Kit] intercepted command " + split[0]);
        if (cmdname.equalsIgnoreCase("kit")) {
          if (player == null || Permissions.Security.permission(player, "kit") || Permissions.Security.permission(player, "kit.kit")) {
              if (args.length == 0) {
                logger.info("[Kit] " + pname + " requested a list of kits ");
                  // Show a list of kits
                  String kitnames = "";
              for ( KitObject kit : kits ) {
                //KitObject kit = new KitObject(kitdata);
                if (player == null || Permissions.Security.permission(player, "kit." + kit.Name().toLowerCase())) {
                  if (!kitnames.matches("")) {
                    kitnames = kitnames.concat(", ");
                  }
                  kitnames = kitnames.concat(kit.Name());
                  if (iConomy != null && kit.Cost() > 0) {
                    kitnames = kitnames.concat(" ("+kit.Cost()+")");
                  }
                }
              }
              if (kitnames.equals("")) {
                kitnames = "You don't have access to any kits";
              }
              respond(player, "[Kit] " + kitnames);
                  return true;
              }
              if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("reload") && (player == null || Permissions.Security.permission(player, "kit.reload"))) {
                  kits.clear();
                  respond(player, "[Kit] Reloading configuration");
                  loadKits();
                  return true;
                }
                logger.info("[Kit] " + pname + " requested kit " + args[0]);
                // Load history for this player
                HashMap<String, Long> history = loadHistory(player);
  
                String[] players;
                if (args.length == 1) {
                  players = new String[1];
                  players[0] = pname;
                } else {
                  players = new String[args.length-1];
                  for (Integer n=1; n<args.length; n++) {
                    players[n-1] = args[n];
                  }
                }
                
                // Look through the available kits
                  boolean found = false;
              for ( KitObject kit : kits ) {
                //KitObject kit = new KitObject(kitdata);
                
                if (args[0].equalsIgnoreCase(kit.Name())) {
                  
                  // Check permissions
                logger.finest("[Kit] Found kit '"+kit.Name().toLowerCase()+"', checking access");
                  if (player == null) {
                    logger.finest("[Kit] Console access granted");                
                  } else {
                    if (Permissions.Security.permission(player, "kit." + kit.Name().toLowerCase())) {
                      logger.finest("[Kit] Access granted");
                    } else {
                      respond(player, "§4[Kit] Access denied");
                      logger.info("[Kit] Access to kit '"+kit.Name().toLowerCase()+"' denied for "+pname);
                      return true;
                    }
                  }
                          found = true;
  
                          // Check cooldown timer
                          Long now = (new Date()).getTime() / 1000;  // Unixtime
                          Long seconds = 0L;
                          Long last = history.get(kit.Name());
                          if (last == null) { last = 0L; }
                          //System.out.println("Last got this kit at "+last);
                          if (kit.Cooldown() != 0) {
                            //System.out.println("Cooldown is "+kit.Cooldown());
                            seconds = last + kit.Cooldown() - now;
                            //System.out.println("Must wait "+seconds+" seconds");
                            if (Permissions.Security.permission(player, "kit.proxy") && seconds > 0) {
                              logger.info("[Kit] Ignoring cooldown for " + pname + " (" + seconds + " seconds) because of 'kit.proxy' permission");
                              seconds = 0L;  // Moderators bypass the cooldown check
                            }
                          }
                          if (seconds > 0) {
                      respond(player, "§4[Kit] Please try again in about " + timeUntil(seconds) + ".");
                            logger.info("[Kit] Refused kit for " + pname + ": " + kit.Name() + " (need cooldown)");
                            return true;
                  }

                          // iConomy stuff goes here
                          if (kit.Cost() > 0 && player != null && iConomy != null) {
                            if (Permissions.Security.permission(player, "kit.proxy")) {
                              logger.info("[Kit] Ignoring cost of "+kit.Cost()+" for " + pname + " (" + seconds + " seconds) because of 'kit.proxy' permission");
                            } else {
                            	Holdings balance = iConomy.getAccount(player.getName()).getHoldings();                            if (balance.hasEnough(kit.Cost())) {
                                balance.subtract(kit.Cost());
                                player.sendMessage("[Kit] "+iConomy.format(kit.Cost())+" deducted");
                                logger.info("[Kit] Deducted "+iConomy.format(kit.Cost())+" from "+pname);
                              } else {
                                player.sendMessage("§4[Kit] You can't afford that");
                                logger.info("[Kit] "+pname+" can't afford the kit '"+kit.Name()+"'");
                                return true;
                              }
                            }
                          }
                          
                          // Dispense items -- loop through components
                          logger.info("[Kit] Giving a kit to " + pname + ": " + kit.Name());
                          for (String item : kit.Components().keySet()) {
                            Integer count = kit.Components().get(item);
                            String[] elements = item.split(":");
                            Integer id = kit.ComponentId(item);
                            Byte data = kit.ComponentData(item);
                            Short dura = kit.ComponentDurability(item);
                            logger.finest("[Kit] item="+item+" count="+count+" dura="+dura+" data="+data);
                    ItemStack itemstack = new ItemStack(id, count, dura, data);
                    for (String playername : players) {
                      Player p = getServer().getPlayer(playername);
                      if (p != null) {
                        if (player == null || p.equals(player) || Permissions.Security.permission(player, "kit.proxy")) {
                          PlayerInventory inventory = p.getInventory(); 
                          inventory.addItem(itemstack);
                        }
                      }
                    }
                          }

                    // Update history for this player
                          history.put(kit.Name(), now);
                        saveHistory(player, history);
                  for (String playername : players) {
                    Player p = getServer().getPlayer(playername);
                    if (p == null) {
                      respond(player, "§4[Kit] "+playername+" is not online");
                    } else {
                      if (p.equals(player)) {
                        respond(player, "[Kit] Enjoy your kit!");
                      } else {
                      respond(player, "[Kit] "+p.getName()+" has received a kit");
                      respond(p, "[Kit] "+pname+" gave you a kit!");
                      }
                    }
                  }
                  
                          return true; // Mission accomplished
                }
              }
              if (found == false) {
                respond(player, "§4[Kit] Please type '/kit' for a list of valid kits.");
            logger.info("[Kit] "+player.getName()+" requested unknown kit '"+args[0]+"'");
              }
                  return true;
              }
          } else {
            respond(player, "§4[Kit] Permission denied");
            return true;
          }
        }
        return false;
    }
    
    private void respond(Player player, String message) {
      if (player == null) {
        System.out.println(message);
      } else {
        player.sendMessage(message);
      }
    }
    
    private HashMap<String, Long> loadHistory(Player player) {
      HashMap<String, Long> history = new HashMap<String, Long>();
      if (player != null) {
        String fname = path + "/kits-" + player.getName().toLowerCase() + ".txt";
        try {
            BufferedReader input =  new BufferedReader(new FileReader(fname));
          String line = null;
          while (( line = input.readLine()) != null) {
            // File consists of key=value pairs, put them in the hash 
            String[] parts = line.split("=", 2);
            history.put(parts[0], Long.valueOf(parts[1]));
          }
          input.close();
        }
        catch (FileNotFoundException e) {
          // Don't print anything, this just confuses people
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
      return history;
    }

    private void saveHistory(Player player, HashMap<String, Long> history) {
      if (player != null) {
        String fname = path + "/kits-" + player.getName().toLowerCase() + ".txt";
         BufferedWriter output;
           try {
             output = new BufferedWriter(new FileWriter(fname));
             for ( String key : history.keySet() ) {
               output.write( key + "=" + history.get(key) + "\n" );
             }
             output.close();
           }
           catch (Exception e) {
          e.printStackTrace();
           }
      }
    }
    
    private String timeUntil(Long sec) {
      Integer buf;
      if (sec < 60*2) {
        buf = Math.round(sec);
        return buf + " second" + (buf==1?"":"s"); 
      }
      if (sec < 3600*2) { 
        buf = Math.round(sec/60);
        return buf + " minute" + (buf==1?"":"s"); 
      }
      if (sec < 86400*2) { 
        buf = Math.round(sec/3600);
        return buf + " hour" + (buf==1?"":"s"); 
      }
    buf = Math.round(sec/86400);
    return buf + " day" + (buf==1?"":"s"); 
    }

    private void loadKits() {
      String fname = "plugins/KitPlugin/kits.txt";
      File f;
      
      // Ensure that directory exists
      String pname = "plugins/KitPlugin";
      f = new File(pname);
      if (!f.exists()) {
        if (f.mkdir()) {
          logger.info( "[Kit] Created directory '" + pname + "'" );
        }
      }
      // Ensure that kits.txt exists
      f = new File(fname);
      if (!f.exists()) {
      BufferedWriter output;
      String newline = System.getProperty("line.separator");
      try {
        output = new BufferedWriter(new FileWriter(fname));
        output.write("# Name;ID Amount;ID Amount;ID amount (etc)[;-cooldown]" + newline);
        output.write("Starterkit;268 1;269 1;-300" + newline);
        output.write("Rock;1 256" + newline);
        output.close();
          logger.info( "[Kit] Created config file '" + fname + "'" );
      } catch (Exception e) {
        e.printStackTrace();
      }
      }

      
    String line = null;
    Integer lineno = 0;
      try {
          BufferedReader input =  new BufferedReader(new FileReader(fname));
        while (( line = input.readLine()) != null) {
          lineno++;
          line = line.trim();
          if (!line.matches("^#.*") && !line.matches("")) {
            // System.out.println("Kit loaded: " + line );
            kits.add(new KitObject(line));
          }
        }
        input.close();
        logger.info("[Kit] "+fname+" reloaded OK");
      }
      catch (FileNotFoundException e) {
        logger.warning("[Kit] Error reading config file '" + fname + "': " + e.getLocalizedMessage());
      }
      catch (Exception e) {
        logger.warning("[Kit] "+fname+" line "+lineno+": "+line);
        e.printStackTrace();
      }

    }
}