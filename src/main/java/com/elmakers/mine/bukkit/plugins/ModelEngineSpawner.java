package com.elmakers.mine.bukkit.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.ImmutableSet;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

public class ModelEngineSpawner extends JavaPlugin implements TabExecutor {
    private static final ChatColor CHAT_PREFIX = ChatColor.AQUA;
    private static final ChatColor ERROR_PREFIX = ChatColor.RED;
    private static final int MAX_RANGE = 64;
    private static final Set<Material> transparent = ImmutableSet.of(Material.AIR, Material.CAVE_AIR, Material.TALL_GRASS);

    public void onEnable() {
        PluginCommand command = getCommand("spawnmodel");
        if (command != null){
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (!(sender instanceof Player)) {
            sendError(sender, "May only be used in-game");
            return true;
        }
        String modelName = args[0];
        String mobType = args.length > 1 ? args[1] : "husk";
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(mobType.toUpperCase());
        } catch (Exception ex) {
            sendError(sender, "Invalid mod type: " + ChatColor.WHITE + mobType);
            return true;
        }
        Player player = (Player)sender;
        Block block = player.getTargetBlock(transparent, MAX_RANGE);
        if (transparent.contains(block.getType())) {
            sendError(sender, "You must be looking at a block");
            return true;
        }
        block = block.getRelative(BlockFace.UP);
        Entity mob = block.getWorld().spawnEntity(block.getLocation(), entityType);
        if (!mob.isValid()) {
            sendError(sender, "Failed to spawn mob of type: " + ChatColor.WHITE + entityType);
            return true;
        }
        ActiveModel model = ModelEngineAPI.api.getModelManager().createActiveModel(modelName);
        if (model == null) {
            sendError(sender, "Failed to load model: " + ChatColor.WHITE + modelName);
            return true;
        }

        ModeledEntity modeledEntity = ModelEngineAPI.api.getModelManager().createModeledEntity(mob);
        if (modeledEntity == null) {
            sendError(sender, "Failed to create modelled entity");
            return false;
        }

        modeledEntity.addActiveModel(model);
        modeledEntity.detectPlayers();
        modeledEntity.setInvisible(true);

        return true;
    }

    public List<String> onTabComplete(String command, String[] args) {
        List<String> options = new ArrayList<>();
        if (command.equals("spawnmodel")) {
            if (args.length == 1) {
                Collection<String> models = ModelEngineAPI.api.getModelManager().getModelRegistry().getRegisteredModel().keySet();
                options.addAll(models);
            } else if (args.length == 2) {
                for (EntityType entityType : EntityType.values()) {
                    options.add(entityType.name().toLowerCase());
                }
            }
        }
        return options;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String completeCommand = args.length > 0 ? args[args.length - 1] : "";

        completeCommand = completeCommand.toLowerCase();
        Collection<String> allOptions = onTabComplete(command.getName(), args);
        List<String> options = new ArrayList<>();
        for (String option : allOptions) {
            String lowercase = option.toLowerCase();
            if (lowercase.startsWith(completeCommand)) {
                options.add(option);
            }
        }
        Collections.sort(options);

        return options;
    }

    protected void sendMessage(CommandSender sender, String string) {
        sender.sendMessage(CHAT_PREFIX + string);
    }

    protected void sendError(CommandSender sender, String string) {
        sender.sendMessage(ERROR_PREFIX + string);
    }
}
