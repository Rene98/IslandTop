package me.renes.IslandTop;

import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class IslandTop extends JavaPlugin {

     List<UUID> toCalculate = new ArrayList<>();
     List<UUID> calculating = new ArrayList<>();
    ArrayList<UUID> istopTen = new ArrayList<>();
    Map<UUID, Double> istops = new HashMap<>();
    long lastcal = 0;
    List<UUID> notifyOnDone = new ArrayList<>();

    ArrayList<Material> sortedMatList = new ArrayList<>();
    HashMap<Material, Double> sortedMaterial = new HashMap<>();
    ArrayList<EntityType> sortedSpawnerList = new ArrayList<>();
    HashMap<EntityType, Double> sortedSpawner = new HashMap<>();

    private IstopCommand istopCommand;
    private Configuration configuration;

    public void onEnable() {
        configuration = new Configuration(this);
        istopCommand = new IstopCommand(this, configuration);
        configuration.loadConfig();

        if(getServer().getPluginManager().getPlugin("PlaceholderAPI") != null){
            new PAPI(this, configuration).register();
        }
    }

    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    private long lastislandcalc = 0;
    void removeCalculating(UUID owner) {
        calculating.remove(owner);
        if(!notifyOnDone.isEmpty() && notifyOnDone.contains(owner)) {
            notifyOnDone.remove(owner);
            for (UUID id:ASkyBlockAPI.getInstance().getIslandOwnedBy(owner).getMembers()) {
                if(getServer().getPlayer(id) != null) {
                    getServer().getPlayer(id).sendMessage(configuration.color("&7Your island level is &6" + istops.get(owner), true));
                }
            }
        }
        if(!toCalculate.isEmpty()) {
            while(true) {
                if(System.currentTimeMillis()-lastislandcalc > 1000) {
                    lastislandcalc = System.currentTimeMillis();
                    StaggeredRunnable staggeredRunnable = new StaggeredRunnable(this, ASkyBlockAPI.getInstance().getIslandOwnedBy(owner), this.configuration, false, "", false, null);
                    calculating.add(toCalculate.get(0));
                    toCalculate.remove(0);
                    break;
                }
            }
        }
    }
    void addCalculate(UUID owner, UUID forcing) {
        addCalculate(owner, forcing, false);
    }

    void addCalculate(UUID owner, UUID forcing, boolean track) {
        //getServer().broadcastMessage("calculating " + owner.toString());
        Island is = ASkyBlockAPI.getInstance().getIslandOwnedBy(owner);
        if(is == null) {
            //getServer().broadcastMessage("island is null");
            return;
        }
        if(forcing != null) {
            if(track){
                new StaggeredRunnable(this, is, this.configuration, true, this.getServer().getOfflinePlayer(is.getOwner()).getName(), true, forcing);
            } else {
                new StaggeredRunnable(this, is, this.configuration, false, "", true, forcing);

            }
            calculating.add(owner);
            return;
        }
        if(calculating.isEmpty()) {
            StaggeredRunnable staggeredRunnable = new StaggeredRunnable(this, is, this.configuration, false, "", false, null);
            calculating.add(owner);
        } else {
            toCalculate.add(owner);
        }
    }

    boolean isCalculating(UUID owner) {
        return (!calculating.isEmpty() && calculating.contains(owner)) || (!toCalculate.isEmpty() && toCalculate.contains(owner));
    }
}
