package me.renes.IslandTop;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class IstopCommand implements CommandExecutor, Listener {
    private IslandTop istop;
    private Configuration configuration;

    public IstopCommand(IslandTop islandTop, Configuration configuration) {
        this.istop = islandTop;
        this.configuration = configuration;
        islandTop.getCommand("istop").setExecutor(this);
        islandTop.getServer().getPluginManager().registerEvents(this, islandTop);
    }

    public boolean onCommand(CommandSender cs,Command cmd, String label, String[] args) {
            if(!cmd.getName().equalsIgnoreCase("istop")) return true;
            if(!(cs instanceof Player)) return true;
            Player p = (Player) cs;
            if(p.hasPermission("istop.use")) {
                switch (args.length) {
                    case 1:
                        switch (args[0].toLowerCase()) {
                            case "level":
                                if (!(ASkyBlockAPI.getInstance().hasIsland(p.getUniqueId()) || ASkyBlockAPI.getInstance().inTeam(p.getUniqueId()))) {
                                    p.sendMessage(color("You are not part of any island", true));
                                    break;
                                }
                                UUID mem = ASkyBlock.getPlugin().getPlayers().getUUID(p.getName());
                                if (ASkyBlockAPI.getInstance().hasIsland(mem) || ASkyBlockAPI.getInstance().inTeam(mem)) {
                                    double level = configuration.getValue(mem, true);
                                    if (level > -50) p.sendMessage(color("Your island value is: &6" + level, true));
                                }
                                break;
                            case "show":
                                showtop(p);
                                break;
                            case "value":
                                showValue(p, true, 0);
                                break;
                            case "reload":
                                if (p.hasPermission("istop.admin")) {
                                    configuration.loadConfig();
                                    p.sendMessage(color("Reloaded block values!", true));
                                }
                                break;
                            case "debug":
                                if (p.hasPermission("istop.admin")) {
                                    p.sendMessage(color("&3Islandtop debugging:", false));
                                    p.sendMessage(color("&3Islands for calc: " + ((istop.toCalculate.isEmpty() ? 0 : istop.toCalculate.size())), false));
                                    p.sendMessage(color("&3Islands are calc: " + ((istop.calculating.isEmpty() ? 0 : istop.calculating.size())), false));
                                }
                                break;
                            case "recalcall":
                                if (p.hasPermission("istop.admin")) {
                                    for (UUID owners : ASkyBlockAPI.getInstance().getOwnedIslands().keySet()) {
                                        configuration.getValue(owners, false);
                                    }
                                    int totalcalc = istop.toCalculate.size();
                                    int lastnotice = 0;
                                    while (true) {
                                        if(System.currentTimeMillis()-lastnotice > 5000 && !istop.toCalculate.isEmpty()) {
                                            p.sendMessage(color("&7Calculating progress: "+ ((int) ((1-istop.toCalculate.size()/totalcalc)*100)) + " %",true));
                                        }
                                        if(istop.calculating.isEmpty()) {
                                            p.sendMessage(color("&aAll islands have been recalculated", true));
                                            break;
                                        }
                                    }
                                }
                                break;
                            default:
                                helplist(p);
                                break;
                        }
                        break;
                    case 2:
                        if ("value".equalsIgnoreCase(args[0])) {
                            if (args[1].equalsIgnoreCase("blocks") || args[1].equalsIgnoreCase("spawners")) {
                                showValue(p, args[1].equalsIgnoreCase("blocks"), 0);
                                break;
                            }
                        } else if(args[0].equalsIgnoreCase("forcecalc")) {
                            if (!p.hasPermission("istop.admin")) {
                                p.sendMessage(color("&cNo Permission for this command", true));
                                return true;
                            }
                            UUID player;
                            if(istop.getServer().getPlayer(args[1]) == null) {
                                if(istop.getServer().getOfflinePlayer(args[1]) == null) {
                                    p.sendMessage(color("Player " + args[1] + " does not exist", true));
                                    return true;
                                } else {
                                    player = istop.getServer().getOfflinePlayer(args[1]).getUniqueId();
                                }
                            } else {
                                player = istop.getServer().getPlayer(args[1]).getUniqueId();
                            }
                            if(player == null) {
                                p.sendMessage(color("Player " + args[1] + " does not exist, UUID is null", true));
                                return true;
                            }
                            p.sendMessage(color("&cForcefully calculating the island of " +  args[1], true));
                            configuration.getValue(player, false, p.getUniqueId(), false);
                            break;
                        }else if(args[0].equalsIgnoreCase("log")) {
                            if (!p.hasPermission("istop.admin")) {
                                p.sendMessage(color("&cNo Permission for this command", true));
                                return true;
                            }
                            UUID player;
                            if(istop.getServer().getPlayer(args[1]) == null) {
                                if(istop.getServer().getOfflinePlayer(args[1]) == null) {
                                    p.sendMessage(color("Player " + args[1] + " does not exist", true));
                                    return true;
                                } else {
                                    player = istop.getServer().getOfflinePlayer(args[1]).getUniqueId();
                                }
                            } else {
                                player = istop.getServer().getPlayer(args[1]).getUniqueId();
                            }
                            if(player == null) {
                                p.sendMessage(color("Player " + args[1] + " does not exist, UUID is null", true));
                                return true;
                            }
                            p.sendMessage(color("&cForcefully calculating the island of " +  args[1], true));
                            configuration.getValue(player, false, p.getUniqueId(), true);
                            break;
                        }
                        helplist(p);
                        break;

                    default:
                        helplist(p);
                        break;
                }
            }
        return true;
    }

    private void helplist(Player p) {
        p.sendMessage(color("&3Available commands", true));
        p.sendMessage(color("&6/istop level                      &7| &3Updates your island level", false));
        p.sendMessage(color("&6/istop show                       &7| &3Shows Top 10 islands", false));
        p.sendMessage(color("&6/istop value [blocks/spawners]    &7| &3Shows the worth of block", false));
        if(p.hasPermission("istop.admin")) p.sendMessage(color("&6/istop reload                       &7| &3Reloads block-values", false));
        if(p.hasPermission("istop.admin")) p.sendMessage(color("&6/istop debug                       &7| &3Debugs the plugin", false));

    }

    private String color(String s, boolean prefix) {
        return ChatColor.translateAlternateColorCodes('&', (prefix ? "&3IS&bTop &7>" :"") + s);
    }



    private void showValue(Player p, boolean blocks, int page) {
        Inventory inv = istop.getServer().createInventory(null, 54, color("&7[" + page + "] &3Island Top values (" +(blocks ? "Blocks)" : "Spawners)"), false));
        if(blocks) {
            for (int i = 0; i < 5 * 9; i++) {
                if(!istop.sortedMatList.isEmpty() && istop.sortedMatList.size() > (i + page *5*9)) {
                    inv.setItem(i, getValue(istop.sortedMatList.get(i + page * 5 * 9)));
                    if(inv.getItem(i) == null && p.getName().equalsIgnoreCase("f1r3ston3")) {
                        p.sendMessage(istop.sortedMatList.get(i + page * 5 * 9).toString());
                    }
                }
            }
        } else {
            for (int i = 0; i < 5 * 9; i++) {
                if(!istop.sortedSpawnerList.isEmpty() && istop.sortedSpawnerList.size() > (i + page *5*9)) {
                    inv.setItem(i, getValue(istop.sortedSpawnerList.get(i + page * 5 * 9)));
                    if(inv.getItem(i) == null && p.getName().equalsIgnoreCase("f1r3ston3")) {
                        p.sendMessage(istop.sortedSpawnerList.get(i + page * 5 * 9).toString());
                    }
                }
            }
        }
        if(page > 0) {
            inv.setItem(45, getPaper(false));
        }
        inv.setItem(53, getPaper(true));

        p.openInventory(inv);
    }

    private ItemStack getPaper(boolean next) {
        ItemStack it = new ItemStack(Material.PAPER, 1);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(color("&f&l" + (next ? ">> Next Page >>" : "<< Previous Page <<"), false));
        it.setItemMeta(im);
        return it;
    }

    private ItemStack getValue(Material m) {
        ItemStack ret = new ItemStack(m, 1);
        switch (m.toString().toLowerCase()) {
            case "cauldron":
                ret = new ItemStack(Material.CAULDRON_ITEM, 1);
                break;
            case "bed_block":
                ret = new ItemStack(Material.BED, 1);
                break;
            case "iron_door_block":
                ret = new ItemStack(Material.IRON_DOOR, 1);
                break;
            case "brewing_stand":
                ret = new ItemStack(Material.BREWING_STAND_ITEM, 1);
                break;
            case "cocoa":
                ret = new ItemStack(Material.INK_SACK, 1, (short) 3);
                break;
            case "melon_stem":
                ret = new ItemStack(Material.MELON_SEEDS, 1);
                break;
            case "pumpkin_stem":
                ret = new ItemStack(Material.PUMPKIN_SEEDS, 1);
                break;
            case "nether_warts":
                ret = new ItemStack(Material.NETHER_STALK, 1);
                break;
            case "burning_furnace":
                ret = new ItemStack(Material.FURNACE, 1);
                break;
            default:
                break;
        }
        ItemMeta met = ret.getItemMeta();
        met.setDisplayName(color("&e" + formatEnum(m.toString()), false));
        met.setLore(Collections.singletonList(color("&3Levels: &6" + istop.sortedMaterial.get(m)/100, false)));
        ret.setItemMeta(met);
        return ret;
    }

    private ItemStack getValue(EntityType e) {
        ItemStack ret = new ItemStack(Material.MOB_SPAWNER, 1);
        ItemMeta met = ret.getItemMeta();
        met.setDisplayName(color("&e" + formatEnum(e.toString()), false));
        met.setLore(Collections.singletonList(color("&3Levels: &6" + istop.sortedSpawner.get(e)/100, false)));
        ret.setItemMeta(met);
        return ret;
    }

    private String formatEnum(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        if(s.contains("_")) {
            String[] en = s.split("_");
            for (String s1 : en) {
                stringBuilder.append(s1.substring(0, 1).toUpperCase()).append(s1.substring(1).toLowerCase()).append(" ");
            }
            return stringBuilder.toString();
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }



    private HashMap<UUID, Double> temp = new HashMap<>();
    private void showtop(Player p) {
        if(istop.istops.isEmpty()) {
            p.sendMessage(color("There are no islands available to show the top 10", true));
            return;
        }
        if(istop.lastcal + 5*60*1000 < System.currentTimeMillis()) {
            p.sendMessage(color("Calculating Top 10 islands....", true));
            configuration.reDoIstopsList();
            temp.clear();
             temp = istop.istops.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(10)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            istop.istopTen.clear();
            istop.istopTen.addAll(temp.keySet());
            istop.lastcal = System.currentTimeMillis();
        }
        p.openInventory(getIsTopInv());
    }

    private List<Integer> tops = Arrays.asList(4, 12, 14, 19, 20, 21, 22, 23, 24, 25);
    private List<Material> mats = Arrays.asList(Material.DIAMOND_AXE, Material.GOLD_AXE, Material.IRON_AXE, Material.DIAMOND_BLOCK, Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.DIAMOND_ORE, Material.GOLD_ORE, Material.IRON_ORE, Material.REDSTONE_ORE);


    private Inventory getIsTopInv() {
        Inventory inv = istop.getServer().createInventory(null, 27, color("&8Top 10 Islands [" + new SimpleDateFormat("HH:mm dd-MMM-yy").format(new Date(istop.lastcal)) + "]", false));
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, getFiller());
        }
        for (int i:tops) {
            inv.setItem(i, getTop(tops.indexOf(i)+1));
        }
        return inv;
    }

    private ItemStack getFiller() {
        ItemStack im = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 8);
        ItemMeta im2 = im.getItemMeta();
        im2.setDisplayName(color("&0",false));
        im.setItemMeta(im2);
        return im;
    }

    private ItemStack getTop(int place) {
        if(place > istop.istopTen.size()) return null;
        ItemStack im = new ItemStack(mats.get(place-1), 1);
        ItemMeta im2 = im.getItemMeta();
        UUID placeuuid = istop.istopTen.get(place-1);
        String name = "";
        if(istop.getServer().getPlayer(placeuuid) != null) {
            name = istop.getServer().getPlayer(placeuuid).getName();
        } else {
            name = istop.getServer().getOfflinePlayer(placeuuid).getName();
        }
        im2.setDisplayName(color("&e&l<!> Island: &6&n"+ name + "&7 (#" + place + ")",false));
        List<String> lore = new ArrayList<>();
        lore.add(color("&eIsland Level " + istop.istops.get(placeuuid), false));
        for (UUID u: ASkyBlockAPI.getInstance().getTeamMembers(placeuuid)) {
            if(istop.getServer().getPlayer(u) != null) {
                lore.add(color("&b" + istop.getServer().getPlayer(u).getName(), false));
            } else {
                lore.add(color("&b" + istop.getServer().getOfflinePlayer(u).getName(), false));
            }
        }
        im2.setLore(lore);
        im.setItemMeta(im2);
        return im;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(e.getClickedInventory() == null) return;
        if(ChatColor.stripColor(e.getClickedInventory().getTitle()).contains("Island Top values")) {
            //e.getWhoClicked().sendMessage(String.valueOf(e.getSlot()));
            switch (e.getSlot()) {
                case 45:
                    if(e.getClickedInventory().getItem(e.getSlot()) == null) {
                        e.setCancelled(true);
                        break;
                    } else {
                        int page = Integer.parseInt(ChatColor.stripColor(e.getClickedInventory().getTitle()).substring(1).split("]")[0]);
                        showValue((Player) e.getWhoClicked(), (ChatColor.stripColor(e.getClickedInventory().getTitle()).split("\\(")[1].endsWith("Blocks)")), page-1);
                        e.setCancelled(true);
                    }
                    break;
                case 53:
                    int page = Integer.parseInt(ChatColor.stripColor(e.getClickedInventory().getTitle()).substring(1).split("]")[0]);
                    showValue((Player) e.getWhoClicked(), (ChatColor.stripColor(e.getClickedInventory().getTitle()).split("\\(")[1].endsWith("Blocks)")), page+1);
                    e.setCancelled(true);
                    break;
                default:
                    e.setCancelled(true);
                    break;

            }
        }
        if(!ChatColor.stripColor(e.getClickedInventory().getTitle()).startsWith("Top 10 Islands")) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID mem = ASkyBlock.getPlugin().getPlayers().getUUID(e.getPlayer().getName());
        if (ASkyBlock.getPlugin().getPlayers().isAKnownPlayer(e.getPlayer().getUniqueId()) && (ASkyBlockAPI.getInstance().hasIsland(mem) || ASkyBlockAPI.getInstance().inTeam(mem))) {
            e.getPlayer().sendMessage(color("&7Your latest calculated islandlevel is: &6" + configuration.getLevelFromFile(e.getPlayer().getUniqueId()), true));
            e.getPlayer().sendMessage(color("&7This value was calculated " + configuration.formatTime((System.currentTimeMillis() / 1000) - configuration.getLastCalculated(mem)) + " ago", false));
            e.getPlayer().sendMessage(color("&7You can recalculate your island level with /istop level", false));
        }
    }
}
