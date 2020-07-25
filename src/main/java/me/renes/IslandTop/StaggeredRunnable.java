package me.renes.IslandTop;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.util.Pair;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class StaggeredRunnable
{

    private static final int MAX_CHUNKS = 200;
    private static final long SPEED = 1;
    private boolean checking = true;
    private boolean Nchecking = true;
    private BukkitTask task;
    private BukkitTask Ntask;

    private Set<Pair<Integer, Integer>> chunksToScan;
    private Set<Pair<Integer, Integer>> netherChunksToScan;

    private IslandTop plugin;
    private Island island;
    private World world;
    private World nWorld;
    private final Configuration config;
    private boolean NetherDone = false;
    private boolean OverDone = false;
    private YamlConfiguration file;
    private File owner;

    private boolean isTracking;
    private ArrayList<String> blockscalced = new ArrayList<>();
    private HashMap<String, Integer> amountblocks = new HashMap<>();
    private String tracker = "";

    private boolean forcing = false;
    private UUID forcer;

    private double level = 0;


    StaggeredRunnable(final IslandTop plugin, final Island island, final Configuration config, boolean track, String username, boolean force, UUID forcer) {
        this.plugin = plugin;
        this.island = island;
        this.world = ASkyBlockAPI.getInstance().getIslandWorld();
        this.nWorld = ASkyBlockAPI.getInstance().getNetherWorld();
        this.config = config;
        this.isTracking = track;
        this.forcing = force;
        this.forcer = forcer;

        if(island == null) return;
        if(track) {
            tracker = username;
            if (ASkyBlock.getPlugin().getPlayers().get(island.getOwner()).getPlayerName().equalsIgnoreCase(tracker)) {
                File logfolder = new File(plugin.getDataFolder() + "/userlogs");
                if(!logfolder.exists()) {
                    logfolder.mkdirs();
                }
                owner = new File(logfolder, tracker + ".yml");
                if (!owner.exists()) {
                    try {
                        owner.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                this.file = YamlConfiguration.loadConfiguration(this.owner);
            }
        }

        // Get chunks to scan
        chunksToScan = getChunksToScan(island);
        netherChunksToScan = getNetherChunksToScan(island);

        //start scan
        checking = false;
        Nchecking = true;

        OverDone = false;
        NetherDone = false;

        //start scan task til done or canceled (OverWorld)
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, ()-> {
            if(NetherDone) {
                Ntask.cancel();
                if(OverDone) {
                    tidyUp();
                }

                if(this.island.getOwner() == null) {
                   task.cancel();
                   return;
                }

                Set<ChunkSnapshot> chunkSnapshot = new HashSet<>();
                if (checking) {
                    Iterator<Pair<Integer, Integer>> it = chunksToScan.iterator();
                    if (!it.hasNext()) {
                        // Nothing left
                        OverDone = true;
                        return;
                    }

                    // Add chunk snapshots to the list
                    while (it.hasNext() && chunkSnapshot.size() < MAX_CHUNKS) {
                        Pair<Integer, Integer> pair = it.next();
                        if (!world.isChunkLoaded(pair.x, pair.z)) {
                            world.loadChunk(pair.x, pair.z);
                            chunkSnapshot.add(world.getChunkAt(pair.x, pair.z).getChunkSnapshot());
                            world.unloadChunk(pair.x, pair.z);
                        } else {
                            chunkSnapshot.add(world.getChunkAt(pair.x, pair.z).getChunkSnapshot());
                        }
                        it.remove();
                    }

                    // Move to next step
                    checking = false;
                    checkChunksAsync(chunkSnapshot);
                }
            }
        }, 0L, SPEED);

        //start scan task til done or canceled (Nether)
        Ntask = plugin.getServer().getScheduler().runTaskTimer(plugin, ()-> {
           if(this.island.getOwner() == null) {
               Ntask.cancel();
               return;
           }
            Set<ChunkSnapshot> chunkSnapshot = new HashSet<>();
            if (Nchecking) {
                Iterator<Pair<Integer, Integer>> it2 = netherChunksToScan.iterator();
                if (!it2.hasNext()) {
                    // Nothing left
                    checking = true;
                    NetherDone = true;
                    return;
                }
                // Add chunk snapshots to the list
                while (it2.hasNext() && chunkSnapshot.size() < MAX_CHUNKS) {
                    Pair<Integer, Integer> pair = it2.next();
                    if (!nWorld.isChunkLoaded(pair.x, pair.z)) {
                        nWorld.loadChunk(pair.x, pair.z);
                        chunkSnapshot.add(nWorld.getChunkAt(pair.x, pair.z).getChunkSnapshot());
                        nWorld.unloadChunk(pair.x, pair.z);
                    } else {
                        chunkSnapshot.add(nWorld.getChunkAt(pair.x, pair.z).getChunkSnapshot());
                    }
                    it2.remove();
                }
                // Move to next step
                Nchecking = false;
                checkNetherChunksAsync(chunkSnapshot);
            }
        }, 0L, SPEED);
    }

    /**
     * Get a set of all the chunks in island
     * @param island
     * @return
     */
    private Set<Pair<Integer, Integer>> getChunksToScan(Island island) {
            Set<Pair<Integer, Integer>> chunkSnapshot = new HashSet<>();
            for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionSize() + 16); x += 16) {
                for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionSize() + 16); z += 16) {
                    Pair<Integer, Integer> pair = new Pair<>(world.getBlockAt(x, 0, z).getChunk().getX(), world.getBlockAt(x, 0, z).getChunk().getZ());
                    chunkSnapshot.add(pair);
                }
            }

        return chunkSnapshot;
    }

    /**
     * Get a set of all the chunks in island
     * @param island
     * @return
     */
    private Set<Pair<Integer, Integer>> getNetherChunksToScan(Island island) {
            Set<Pair<Integer, Integer>> chunkSnapshot = new HashSet<>();
            for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionSize() + 16); x += 16) {
                for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionSize() + 16); z += 16) {
                    Pair<Integer, Integer> pair = new Pair<>(nWorld.getBlockAt(x, 0, z).getChunk().getX(), nWorld.getBlockAt(x, 0, z).getChunk().getZ());
                    chunkSnapshot.add(pair);
                }
            }

        return chunkSnapshot;
    }

    private void checkChunksAsync(final Set<ChunkSnapshot> chunkSnapshot) {
        // Run async task to scan chunks
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            double progress = 0;
            double lastnotice = 0;
            final int max = chunkSnapshot.size();
            for (ChunkSnapshot chunk : chunkSnapshot) {
                scanChunk(chunk);
                if((progress/max*100)-lastnotice > 24) {
                    lastnotice = (progress/max*100);
                    if(!forcing) {
                        for (UUID id : island.getMembers()) {
                            if (plugin.getServer().getPlayer(id) != null) {
                                plugin.getServer().getPlayer(id).sendMessage(ChatColor.translateAlternateColorCodes('&', "&aOverworld&3 | Island Calculation progress: &a" + (progress / max) * 100 + "%"));
                            }
                        }
                    } else {
                        plugin.getServer().getPlayer(forcer).sendMessage(ChatColor.translateAlternateColorCodes('&', "&aOverworld&3 | [" + plugin.getServer().getOfflinePlayer(island.getOwner()).getName() + "] Island Calculation progress: &a" + (progress / max) * 100 + "%"));
                    }
                }
                 progress++;
            }
            // Nothing happened, change state
            checking = true;
        });
    }

    private void checkNetherChunksAsync(final Set<ChunkSnapshot> chunkSnapshot) {
        // Run async task to scan chunks
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            double progress = 0;
            double lastnotice = 0;
            final int max = chunkSnapshot.size();
            for (ChunkSnapshot chunk : chunkSnapshot) {
                scanChunk(chunk);
                if((progress/max*100)-lastnotice > 24) {
                    lastnotice = (progress/max*100);
                    if(!forcing) {
                        for (UUID id : island.getMembers()) {
                            if (plugin.getServer().getPlayer(id) != null) {
                                plugin.getServer().getPlayer(id).sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNether&3 | Island Calculation progress: &a" + (progress / max) * 100 + "%"));
                            }
                        }
                    } else {
                        plugin.getServer().getPlayer(forcer).sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNether&3 | [" + plugin.getServer().getOfflinePlayer(island.getOwner()).getName() + "] Island Calculation progress: &a" + (progress / max) * 100 + "%"));
                    }
                }
                 progress++;
            }
            // Nothing happened, change state
            Nchecking = true;
        });
    }

    private void scanChunk(ChunkSnapshot chunk) {

        for (int x = 0; x< 16; x++) {
            // Check if the block coord is inside the protection zone and if not, don't count it
            if (chunk.getX() * 16 + x < island.getMinProtectedX() || chunk.getX() * 16 + x >= island.getMinProtectedX() + island.getProtectionSize()) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                // Check if the block coord is inside the protection zone and if not, don't count it
                if (chunk.getZ() * 16 + z < island.getMinProtectedZ() || chunk.getZ() * 16 + z >= island.getMinProtectedZ() + island.getProtectionSize()) {
                    continue;
                }

                for (int y = 0; y < island.getCenter().getWorld().getMaxHeight(); y++) {
                    Material blockType = Material.getMaterial(chunk.getBlockTypeId(x, y, z));
                    // Air is free
                    if (!blockType.equals(Material.AIR)) {
                        checkBlock(blockType, chunk.getBlockData(x, y, z), new Location(this.plugin.getServer().getWorld(chunk.getWorldName()), chunk.getX()*16 + x, y, chunk.getZ()*16 + z));
                        //plugin.getServer().broadcastMessage("x " + x + ", y " + y + ", z" + z);
                    }
                }
            }
        }
    }

    private void checkBlock(Material type, int blockData, Location l) {
        // Currently, there is no alternative to using block data (Feb 2018)
        @SuppressWarnings("deprecation")
        MaterialData md = new MaterialData(type, (byte) blockData);
        double leveltemp = config.getBlockValue(type, l);
        level += leveltemp;
        if(isTracking) {
            if(leveltemp > 0) {
                //blockscalced.add("{" + l.getWorld().getName() + "} [" + level + "] " + type.toString() + " at [" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ() + "] added " + leveltemp);
                if (amountblocks.isEmpty()) {
                    amountblocks.put(type.toString(), 1);
                } else if (amountblocks.containsKey(type.toString())) {
                    int blocks = amountblocks.get(type.toString()) + 1;
                    amountblocks.replace(type.toString(), blocks);
                } else {
                    amountblocks.put(type.toString(), 1);
                }
            }
        }
    }

    private void tidyUp() {
        level = level/100;
        task.cancel();
        if(isTracking) {
            if (ASkyBlock.getPlugin().getPlayers().get(island.getOwner()).getPlayerName().equalsIgnoreCase(tracker)) {
                try {
                    //file.set("levelbuildup", blockscalced);
                    file.set("totallevel", level);
                    file.createSection("blocks", amountblocks);
                    file.save(owner);
                    /*for (String s : amountblocks.keySet()) {

                        file.set("summary." + s, amountblocks.get(s));
                        count++;
                    }*/
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(forcing) {
            plugin.getServer().getPlayer(forcer).sendMessage("Island of " + plugin.getServer().getOfflinePlayer(island.getOwner()).getName() + " is level: " + level);
        }
        config.setValue(island.getOwner(), level);
        if(!plugin.istops.isEmpty() && plugin.istops.containsKey(island.getOwner())) {
            plugin.istops.replace(island.getOwner(), level);
        } else {
            plugin.istops.put(island.getOwner(), level);
        }
        plugin.removeCalculating(island.getOwner());
    }




























    /*private final IslandTop istop;
    private final Island i;
    private final Configuration config;

    private int taskId;
    private boolean done = false;

    private int iteratorCount = 0;
    private final int maxIterationsPerTick = 300;
    private World w;
    private int x, y, z;

    private int minX, minZ, maxX,maxZ;
    private double level =0;

    StaggeredRunnable(IslandTop myPlugin, Island i, Configuration config)
    {
        this.istop = myPlugin;
        this.i = i;
        this.minX = i.getMinProtectedX();
        this.maxX = i.getProtectionSize()+minX;
        this.minZ = i.getMinProtectedZ();
        this.maxZ = i.getProtectionSize()+minZ;
        this.w = ASkyBlockAPI.getInstance().getIslandWorld();
        this.config = config;
    }

    void start()
    {
        // reset whenever we call this method
        iteratorCount = 0;

        long delay_before_starting = 10;
        long delay_between_restarting = 10;
        x = this.minX;
        y = 0;
        z = this.minZ;
        // synchronous - thread safe
        this.taskId = istop.getServer().getScheduler().runTaskTimer(istop, this, delay_before_starting, delay_between_restarting).getTaskId();

    }
    String color(String s) {
        return ChatColor.translateAlternateColorCodes('&',s);
    }

    // this example will stagger parsing a huge list
    public void run()
    {
        iteratorCount = 0;

        // while the list isnt empty, and we havent exceeded matIteraternsPerTick....
        // the loop will stop when it reaches 300 iterations OR the list becomes empty
        // this ensures that the server will be happy clappy, not doing too much per tick.


        while (!done && iteratorCount < maxIterationsPerTick) {
            if (iteratorCount == maxIterationsPerTick - 1) {
                int progress = y / 255 * 10;
                StringBuilder string = new StringBuilder();
                for (int j = 1; j < 10; j++) {
                    if (j < progress) {
                        string.append(color("&a&l◼"));
                    } else {
                        string.append(color("&c◼&l"));
                    }
                }
                for (UUID u : i.getMembers()) {
                    if (istop.getServer().getPlayer(u) != null) {
                        ActionBarAPI.sendActionBar(istop.getServer().getPlayer(u), string.toString(), 20);
                    }
                }

                istop.getServer().broadcastMessage(x + " / " + maxX + " " + y + " " + z + " / " + maxZ);
            }
            Block b = w.getBlockAt(x, y, z);
            if (b != null && b.getType() != null && b.getType() != Material.AIR) {
                istop.getServer().broadcastMessage(b.getType() + " " + config.getBlockvalue(b));
                level += config.getBlockvalue(b);

                if (x < maxX) {
                    x++;
                } else {
                    if (z < maxZ) {
                        z++;
                    } else {
                        if (y > 255) {
                            done = true;
                        }
                        x = minX;
                        z = minZ;
                        y++;
                    }
                }

                iteratorCount++;
            }
        }

        // if our condition/result is met, cancel this task.
        // this can be anything, it is just cancelling this repeating task when we have met a condition we are looking for.
        if (done)
        {
            config.setValue(i.getOwner(), level);
            if(!istop.istops.isEmpty() && istop.istops.containsKey(i.getOwner())) {
                istop.istops.replace(i.getOwner(), level);
            } else {
                istop.istops.put(i.getOwner(), level);
            }
            istop.removeCalculating(i.getOwner());
            istop.getServer().getScheduler().cancelTask(this.taskId);
        }

    }*/

}