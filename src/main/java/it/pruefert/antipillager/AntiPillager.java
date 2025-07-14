package it.pruefert.antipillager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AntiPillager implements ModInitializer {

    private static final Gson GSON = new Gson();
    private static List<Zone> zones = new ArrayList<>();

    @Override
    public void onInitialize() {
        System.out.println("[PillagerBlocker] Initializing mod...");
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        if (zones.isEmpty()) {
            loadZones(server);
        }

        for (ServerWorld world : server.getWorlds()) {
            for (Zone zone : zones) {
                Box box = new Box(
                        zone.x - zone.radius, zone.y - zone.radius, zone.z - zone.radius,
                        zone.x + zone.radius, zone.y + zone.radius, zone.z + zone.radius
                );

                List<PillagerEntity> pillagers = world.getEntitiesByType(
                        EntityType.PILLAGER,
                        box,
                        entity -> entity.squaredDistanceTo(new Vec3d(zone.x, zone.y, zone.z)) <= (zone.radius * zone.radius)
                );

                for (PillagerEntity pillager : pillagers) {
                    pillager.discard(); // Equivalent to kill()
                }
            }
        }
    }

    private void loadZones(MinecraftServer server) {
        try {
            File configFile = new File(server.getSavePath(WorldSavePath.ROOT).toFile(), "pillagerblocker.json");
            if (!configFile.exists()) {
                System.out.println("[PillagerBlocker] No config found, creating default... @ " + configFile.getAbsolutePath());
                configFile.createNewFile();
                List<Zone> defaultZones = List.of(new Zone(100, 70, 100, 64));
                String json = GSON.toJson(new ZoneWrapper(defaultZones));
                Files.write(configFile.toPath(), json.getBytes());
                zones = defaultZones;
                return;
            }

            try (FileReader reader = new FileReader(configFile)) {
                Type type = new TypeToken<ZoneWrapper>() {}.getType();
                ZoneWrapper wrapper = GSON.fromJson(reader, type);
                zones = wrapper.zones != null ? wrapper.zones : new ArrayList<>();
                System.out.println("[PillagerBlocker] Loaded " + zones.size() + " zones. @ " + configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[PillagerBlocker] Failed to load zones: " + e.getMessage());
        }
    }

    static class ZoneWrapper {
        List<Zone> zones;
        ZoneWrapper(List<Zone> zones) {
            this.zones = zones;
        }
    }
}
