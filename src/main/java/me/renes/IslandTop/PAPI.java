package me.renes.IslandTop;

import com.wasteofplastic.askyblock.ASkyBlockAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class PAPI extends PlaceholderExpansion {

    private IslandTop plugin;
    private Configuration configuration;

    /**
     * Since we register the expansion inside our own plugin, we
     * can simply use this method here to get an instance of our
     * plugin.
     *
     * @param plugin
     *        The instance of our plugin.
     */
    public PAPI(IslandTop plugin, Configuration configuration){
        this.plugin = plugin;
        this.configuration = configuration;
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist(){
        return true;
    }

    /**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister(){
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * <br>For convienience do we return the author from the plugin.yml
     *
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>This must be unique and can not contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier(){
        return "islandtop";
    }

    /**
     * This is the version of the expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.entity.Player Player}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        recalcTops();
        switch (identifier.toLowerCase()) {
            case "1_name":
                return (plugin.istopTen.isEmpty() ? "N/A" : (plugin.getServer().getPlayer(plugin.istopTen.get(0)).getName()));
            case "1_value":
                return (plugin.istopTen.isEmpty() ? "N/A" : formatLevels((Math.floor(plugin.istops.get(plugin.istopTen.get(0))))));
            case "2_name":
                return (plugin.istopTen.size() > 2 ? "N/A" : (plugin.getServer().getPlayer(plugin.istopTen.get(1)).getName()));
            case "2_value":
                return (plugin.istopTen.size() > 2 ? "N/A" : formatLevels((Math.floor(plugin.istops.get(plugin.istopTen.get(1))))));
            case "3_name":
                return (plugin.istopTen.size() > 3 ? "N/A" : (plugin.getServer().getPlayer(plugin.istopTen.get(2)).getName()));
            case "3_value":
                return (plugin.istopTen.size() > 3 ? "N/A" : formatLevels((Math.floor(plugin.istops.get(plugin.istopTen.get(2))))));
            case "4_name":
                return (plugin.istopTen.size() > 4 ? "N/A" : (plugin.getServer().getPlayer(plugin.istopTen.get(3)).getName()));
            case "4_value":
                return (plugin.istopTen.size() > 4 ? "N/A" : formatLevels((Math.floor(plugin.istops.get(plugin.istopTen.get(3))))));
            case "5_name":
                return (plugin.istopTen.size() > 5 ? "N/A" : (plugin.getServer().getPlayer(plugin.istopTen.get(4)).getName()));
            case "5_value":
                return (plugin.istopTen.size() > 5 ? "N/A" : formatLevels((Math.floor(plugin.istops.get(plugin.istopTen.get(4))))));
            case "islevel":
                if (player == null) {
                    return "N/A3";
                }
                UUID owner = player.getUniqueId();
                if (ASkyBlockAPI.getInstance().inTeam(player.getUniqueId()) && ASkyBlockAPI.getInstance().getTeamLeader(player.getUniqueId()) != owner) {
                    owner = ASkyBlockAPI.getInstance().getTeamLeader(player.getUniqueId());
                }
                if (!plugin.istops.isEmpty() && plugin.istops.containsKey(owner)) {
                    return formatLevels(plugin.istops.get(owner));
                } else {
                    return configuration.getLevelFromFile(owner);

                }
            default:
                return null;
        }
    }

    private HashMap<UUID, Double> temp = new HashMap<>();
    public void recalcTops() {
        if(plugin.lastcal + 5*60*1000 < System.currentTimeMillis()) {
            configuration.reDoIstopsList();
            temp.clear();
            temp = plugin.istops.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(10)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            plugin.istopTen.clear();
            plugin.istopTen.addAll(temp.keySet());
            plugin.lastcal = System.currentTimeMillis();
        }
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
}
