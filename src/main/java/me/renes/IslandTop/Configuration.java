package me.renes.IslandTop;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

class Configuration {

    private IslandTop istop;


    Configuration(IslandTop islandTop) {
        this.istop  = islandTop;
    }

    HashMap<Material, Double> blocks = new HashMap<>();
    HashMap<EntityType, Double> spawners = new HashMap<>();
    private long calculatemillis = 60*5;

    double getBlockValue(Material m, Location l) {
        if(m == null) return 0;
        if(m == Material.MOB_SPAWNER) {
            Block b = l.getBlock();
            //bc("x " + l.getBlockX() + ", y " + l.getBlockY() + ", z" + l.getBlockZ());
            // bc("Block is creaturespawner");
            CreatureSpawner creatureSpawner = (CreatureSpawner) b.getState();
            if(WildStackerAPI.getWildStacker().getSystemManager().isStackedSpawner(b)) {
                //bc("Is Wildstacker spawner");
                EntityType spawn = WildStackerAPI.getWildStacker().getSystemManager().getStackedSpawner(creatureSpawner).getSpawnedType();
                int spawneramount = WildStackerAPI.getWildStacker().getSystemManager().getStackedSpawner(creatureSpawner).getStackAmount();
                if(spawners.containsKey(spawn)) return spawneramount * spawners.get(spawn);
                return 0;
            } else {
                //bc("single spawner");
                //istop.getServer().getPlayer("F1r3Ston3").sendMessage(l.toString());
                //istop.getServer().getPlayer("F1r3Ston3").sendMessage(creatureSpawner.getSpawnedType().toString());
                EntityType spawn = creatureSpawner.getSpawnedType();
                if(spawners.containsKey(spawn)) return spawners.get(spawn);
                return 0;
            }
        }
        if(!blocks.containsKey(m)) return 0;
        return blocks.get(m);

    }
    /*double getBlockvalue(Block b) {
        if(b == null) return 0;
        if(b.getType() == null) return 0;
        Material m = b.getType();
        if(m == Material.MOB_SPAWNER) {
            if(WildStackerAPI.getWildStacker().getSystemManager().isStackedSpawner(b)) {
                EntityType spawn = WildStackerAPI.getWildStacker().getSystemManager().getStackedSpawner(b.getLocation()).getSpawnedType();
                int spawneramount = WildStackerAPI.getWildStacker().getSystemManager().getStackedSpawner(b.getLocation()).getStackAmount();
                return spawneramount * spawners.get(spawn);
            } else {
                CreatureSpawner creatureSpawner = (CreatureSpawner) b.getState();
                return spawners.get(creatureSpawner.getSpawnedType());
            }
        }
        if(!blocks.containsKey(m)) return 0;
        return blocks.get(m);
    }*/

  private File values;
  private YamlConfiguration valconf;
  private File islevels;
    private YamlConfiguration levconf;
  void loadConfig() {
      if(!istop.getDataFolder().exists()) {
          istop.getDataFolder().mkdir();
      }
      values = new File(istop.getDataFolder(), "values.yml");
      if(!values.exists()) {
          try {
              values.createNewFile();
              valconf = YamlConfiguration.loadConfiguration(values);
              ConfigurationSection blocks = valconf.createSection("blocks");
              ConfigurationSection spawners = valconf.createSection("spawners");
              for (Material m : Material.values()) {
                  if(m.isBlock()) {
                      blocks.set(m.name().toLowerCase(), 0D);
                  }
              }
              for (EntityType e : EntityType.values()) {
                  if(e.isAlive()) {
                      spawners.set(e.name().toLowerCase(), 0D);
                  }
              }
              valconf.save(values);
          } catch (IOException e) {
              e.printStackTrace();
          }
      } else{
          valconf = YamlConfiguration.loadConfiguration(values);
      }
      if(!blocks.isEmpty()) blocks.clear();
      for (String s: valconf.getConfigurationSection("blocks").getKeys(false)) {
          if (valconf.getConfigurationSection("blocks").getDouble(s) > 0) {
              blocks.put(Material.valueOf(s.toUpperCase()), valconf.getConfigurationSection("blocks").getDouble(s));
          }
      }
      if(!spawners.isEmpty()) spawners.clear();
      for (String s: valconf.getConfigurationSection("spawners").getKeys(false)) {
          if (valconf.getConfigurationSection("spawners").getDouble(s) > 0) {
              spawners.put(EntityType.valueOf(s.toUpperCase()), valconf.getConfigurationSection("spawners").getDouble(s));
          }
      }

      if(!istop.sortedMaterial.isEmpty()) istop.sortedMaterial.clear();
      if(!istop.sortedMatList.isEmpty()) istop.sortedMatList.clear();

      istop.sortedMaterial = blocks.entrySet().stream()
              .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
              .collect(Collectors.toMap(
                      Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
      istop.sortedMatList.addAll(istop.sortedMaterial.keySet());

      if(!istop.sortedSpawner.isEmpty()) istop.sortedMaterial.clear();
      if(!istop.sortedSpawnerList.isEmpty()) istop.sortedMatList.clear();
      istop.sortedSpawner = spawners.entrySet().stream()
              .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
              .collect(Collectors.toMap(
                      Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
      istop.sortedSpawnerList.addAll(istop.sortedSpawner.keySet());

      islevels = new File(istop.getDataFolder(), "islevels.yml");
      if(!islevels.exists()) {
          try {
              islevels.createNewFile();
              levconf = YamlConfiguration.loadConfiguration(islevels);
              levconf.createSection("islandowners");
              levconf.save(islevels);
          } catch (IOException e) {
              e.printStackTrace();
          }
      } else {
          levconf = YamlConfiguration.loadConfiguration(islevels);
      }

      reDoIstopsList();
  }

  void reDoIstopsList() {
      ArrayList<String> removeNonOwners = new ArrayList<>();
      if(!istop.istops.isEmpty()) istop.istops.clear();
      for(String s : levconf.getConfigurationSection("islandowners").getKeys(false)) {
          if(ASkyBlockAPI.getInstance().hasIsland(UUID.fromString(s))) {
              istop.istops.put(UUID.fromString(s), levconf.getConfigurationSection("islandowners").getDouble(s + ".islandlevel"));
          } else {
              removeNonOwners.add(s);
          }
      }
      if(!removeNonOwners.isEmpty()) {
          for(String s : removeNonOwners) {
              levconf.set("islandowners." + s, null);
              istop.getServer().getConsoleSender().sendMessage("Removing " + s +  " from levels config are there are not an islandowner");
          }
      }
  }

    void setValue(UUID owner, double level) {
      ConfigurationSection isowner = levconf.getConfigurationSection("islandowners");
        if(isowner.contains(owner.toString())) {
            isowner.set(owner.toString() + ".islandlevel", level);
            isowner.set(owner.toString() + ".last-calculate", System.currentTimeMillis()/1000);
        } else {
            ConfigurationSection cs = isowner.createSection(owner.toString());
            cs.set("islandlevel", level);
            cs.set("last-calculate", System.currentTimeMillis()/1000);
        }
        try {
            levconf.save(islevels);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void bc(String s) {
      this.istop.getServer().broadcastMessage(s);
    }

    long getLastCalculated(UUID requester) {
        UUID owner = requester;
        if(levconf == null) {
            islevels = new File(istop.getDataFolder(), "islevels.yml");
            levconf = YamlConfiguration.loadConfiguration(islevels);
            double level = 0;
            //bc(owner.toString());
            if(ASkyBlockAPI.getInstance().inTeam(owner) && ASkyBlockAPI.getInstance().getTeamLeader(requester) != requester) {
                owner = ASkyBlockAPI.getInstance().getTeamLeader(requester);
            }
        }

        if(levconf.isConfigurationSection("islandowners") &&
                levconf.getConfigurationSection("islandowners").getKeys(false).isEmpty()) {
            return -1;
        }

        if(levconf.getConfigurationSection("islandowners").contains(owner.toString()) &&
                levconf.getConfigurationSection("islandowners").isConfigurationSection(owner.toString())) {
            return levconf.getConfigurationSection("islandowners." + owner.toString()).getLong("last-calculate");
        }
        return -1;
    }

    String getLevelFromFile(UUID requester) {
        UUID owner = requester;
        if(levconf == null) {
            islevels = new File(istop.getDataFolder(), "islevels.yml");
            levconf = YamlConfiguration.loadConfiguration(islevels);
            double level = 0;
            //bc(owner.toString());
            if(ASkyBlockAPI.getInstance().inTeam(owner) && ASkyBlockAPI.getInstance().getTeamLeader(requester) != requester) {
                owner = ASkyBlockAPI.getInstance().getTeamLeader(requester);
            }
        }

        if(levconf.isConfigurationSection("islandowners") &&
                levconf.getConfigurationSection("islandowners").getKeys(false).isEmpty()) {
            return "N/A";
        }

        if(levconf.getConfigurationSection("islandowners").contains(owner.toString()) &&
                levconf.getConfigurationSection("islandowners").isConfigurationSection(owner.toString())) {
            return formatLevels((levconf.getConfigurationSection("islandowners." + owner.toString()).getDouble("islandlevel")));
        }
       return "N/A";
    }

    private String formatLevels(Double aDouble) {
        DecimalFormat decimalFormat = new DecimalFormat("###.#");
        if(aDouble > 1000000000) {
            return decimalFormat.format(aDouble/1000000000) + " B";
        }
        if(aDouble > 1000000) {
            return decimalFormat.format(aDouble/1000000) + " M";
        }
        if(aDouble > 1000) {
            return decimalFormat.format(aDouble/1000) + " k";
        }
        return aDouble + "";
    }

    double getValue(UUID req, boolean nofity) {
      return getValue(req, nofity, null, false);
    }

    double getValue(UUID requester, boolean notify, UUID forceUUID, boolean tracking) {
        //bc("Blocks hashmap size:" + blocks.size());
        //bc("Spawners hashmap size:" + spawners.size());
      if(levconf == null) {
          islevels = new File(istop.getDataFolder(), "islevels.yml");
          levconf = YamlConfiguration.loadConfiguration(islevels);
      }
        double level = 0;
        UUID owner = requester;
        //bc(owner.toString());
        if(ASkyBlockAPI.getInstance().inTeam(owner) && ASkyBlockAPI.getInstance().getTeamLeader(requester) != requester) {
            owner = ASkyBlockAPI.getInstance().getTeamLeader(requester);
        }
        if(forceUUID != null) {
            requester = forceUUID;
            istop.addCalculate(owner, requester, tracking);
            return -999999;
        }
        //bc(owner.toString());
        if(istop.isCalculating(owner)) {
            istop.getServer().getPlayer(requester).sendMessage(color("The island is already in the queue to be calculated!", true));
            return -99999;
        }
        if(levconf.isConfigurationSection("islandowners") &&
                levconf.getConfigurationSection("islandowners").getKeys(false).isEmpty()) {
            //bc("islandowners empty");
            istop.addCalculate(owner, null);
            if(notify) {
                istop.notifyOnDone.add(owner);
                istop.getServer().getPlayer(requester).sendMessage(color("The island is added to the queue to be (re)calculated, you will be notified when it is done.", true));
            }
            return -999999;
        }
        if(levconf.getConfigurationSection("islandowners").contains(owner.toString()) &&
                levconf.getConfigurationSection("islandowners").isConfigurationSection(owner.toString())) {
            //time in seconds
            long lastcalc = levconf.getConfigurationSection("islandowners." + owner.toString()).getLong("last-calculate");
            if((lastcalc + calculatemillis) < System.currentTimeMillis()/1000 || istop.getServer().getPlayer(requester).hasPermission("istop.cooldown.bypass")) {
                istop.addCalculate(owner, null);
                if(notify) {
                    istop.notifyOnDone.add(owner);
                    istop.getServer().getPlayer(requester).sendMessage(color("The island is added to the queue to be (re)calculated, you will be notified when it is done.", true));
                }
                return -999999;
            } else {
                level = levconf.getConfigurationSection("islandowners." + owner.toString()).getDouble("islandlevel");
                istop.getServer().getPlayer(requester).sendMessage(color("The shown islandlevel is from cache, you can recalculate in " + formatTime((lastcalc+calculatemillis)-(System.currentTimeMillis()/1000)), true));
            }
        } else {
            istop.addCalculate(owner, null);
            if(notify) {
                istop.notifyOnDone.add(owner);
                istop.getServer().getPlayer(requester).sendMessage(color("The island is added to the queue to be (re)calculated, you will be notified when it is done.", true));
            }
            return -999999;
        }
        return level;
    }

    String formatTime(long diff) {
      String returnString = "";
      if(diff >= (60*60*24)) {
          int days = (int) Math.floor(diff/(60*60*24));
          returnString += days + " day(s)";
          diff = diff - (days * (60*60*24));
      }
      if(diff >= (60*60)) {
          int hours = (int) Math.floor(diff/(60*60));
          returnString += hours + " hr(s)";
          diff = diff - (hours * (60*60));
      }
      if(diff >= (60)) {
          int mins = (int) Math.floor(diff/(60));
          returnString += mins + " min(s)";
          diff = diff - (mins * 60);
      }
        return returnString + diff + " sec(s)";
    }

    String color(String s, boolean prefix) {
        return ChatColor.translateAlternateColorCodes('&', (prefix ? "&3IS&bTop &7>" :"") + s);
    }
}
