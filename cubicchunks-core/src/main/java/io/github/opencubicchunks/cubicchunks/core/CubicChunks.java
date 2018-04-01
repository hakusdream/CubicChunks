/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core;

import io.github.opencubicchunks.cubicchunks.core.debug.DebugWorldType;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.proxy.CommonProxy;
import io.github.opencubicchunks.cubicchunks.core.world.type.VanillaCubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.CubeGeneratorsRegistry;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(modid = CubicChunks.MODID,
        name = "CubicChunks",
        version = CubicChunks.VERSION,
        guiFactory = "io.github.opencubicchunks.cubicchunks.core.client.GuiFactory",
        //@formatter:off
        dependencies = "after:forge@[13.20.1.2454,)"/*@@DEPS_PLACEHOLDER@@*/)// This will be replaced by gradle with full deps list not alter it
        //@formatter:on
@Mod.EventBusSubscriber
public class CubicChunks {

    public static final int FIXER_VERSION = 0;

    public static final VersionRange SUPPORTED_SERVER_VERSIONS;
    public static final VersionRange SUPPORTED_CLIENT_VERSIONS;

    static {
        try {
            // currently no known unsupported version. Versions newer than current will be only checked on the other side
            // (I know this can be hard to actually fully understand)
            SUPPORTED_SERVER_VERSIONS = VersionRange.createFromVersionSpec("*");
            SUPPORTED_CLIENT_VERSIONS = VersionRange.createFromVersionSpec("*");
        } catch (InvalidVersionSpecificationException e) {
            throw new Error(e);
        }
    }

    public static final int MIN_BLOCK_Y = Integer.MIN_VALUE >> 1;
    public static final int MAX_BLOCK_Y = Integer.MAX_VALUE >> 1;

    public static final boolean DEBUG_ENABLED = System.getProperty("cubicchunks.debug", "false").equalsIgnoreCase("true");
    public static final String MODID = "cubicchunks";
    public static final String VERSION = "@@VERSION@@";
    public static final String MALISIS_VERSION = "@@MALISIS_VERSION@@";

    @Nonnull
    public static Logger LOGGER = LogManager.getLogger("EarlyCubicChunks");//use some logger even before it's set. useful for unit tests

    @SidedProxy(clientSide = "io.github.opencubicchunks.cubicchunks.core.proxy.ClientProxy", serverSide = "io.github.opencubicchunks.cubicchunks.core.proxy.ServerProxy")
    public static CommonProxy proxy;

    @Nullable
    private static Config config;

    @Nonnull
    private static Set<IConfigUpdateListener> configChangeListeners = Collections.newSetFromMap(new WeakHashMap<>());

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        LOGGER = e.getModLog();

        config = new Config(new Configuration(e.getSuggestedConfigurationFile()));

        CCFixType.addFixableWorldType(VanillaCubicWorldType.create());
        CCFixType.addFixableWorldType(DebugWorldType.create());
        LOGGER.debug("Registered world types");

        CCFixType.registerWalkers();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        proxy.registerEvents();

        PacketDispatcher.registerPackets();
        CubeGeneratorsRegistry.computeSortedGeneratorList();
    }

    @EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.setBuildLimit(event.getServer());
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.getModID().equals(CubicChunks.MODID)) {
            config.syncConfig();
            for (IConfigUpdateListener l : configChangeListeners) {
                l.onConfigUpdate(config);
            }
        }
    }

    @NetworkCheckHandler
    public static boolean checkCanConnectWithMods(Map<String, String> modVersions, Side remoteSide) {
        String remoteFullVersion = modVersions.get(MODID);
        if (remoteFullVersion == null) {
            if (remoteSide.isClient()) {
                return false; // don't allow client without CC to connect
            }
            return true; // allow connecting to server without CC
        }
        if (!checkVersionFormat(VERSION, remoteSide.isClient() ? Side.SERVER : Side.CLIENT)) {
            return true;
        }
        if (!checkVersionFormat(remoteFullVersion, remoteSide)) {
            return true;
        }

        ArtifactVersion version = new DefaultArtifactVersion(remoteFullVersion);
        ArtifactVersion currentVersion = new DefaultArtifactVersion(VERSION);
        if (currentVersion.compareTo(version) < 0) {
            return true; // allow connection if this version is older, let newer one decide
        }
        return (remoteSide.isClient() ? SUPPORTED_CLIENT_VERSIONS : SUPPORTED_SERVER_VERSIONS).containsVersion(version);
    }

    // returns true if version format is known. Side can be null if not logging connection attempt
    private static boolean checkVersionFormat(String version, @Nullable Side remoteSide) {
        int mcVersionSplit = version.indexOf('-');
        if (mcVersionSplit < 0) {
            LOGGER.warn("Connection attempt with unexpected " + remoteSide + " version string: " + version + ". Cannot split into MC "
                    + "version and mod version. Assuming dev environment or special/unknown version, connection will be allowed.");
            return false;
        }

        String modVersion = version.substring(mcVersionSplit + 1);

        if (modVersion.isEmpty()) {
            LOGGER.warn("Connection attempt with unexpected " + remoteSide + " version string: " + version + ". Mod version part not "
                    + "found. Assuming dev environment or special/unknown version,, connection will be allowed");
            return false;
        }

        final String versionRegex = "\\d+\\." + "\\d+\\." + "\\d+\\." + "\\d+" + "(-.+)?";//"MAJORMOD.MAJORAPI.MINOR.PATCH(-final/rcX/betaX)"

        if (!modVersion.matches(versionRegex)) {
            LOGGER.warn("Connection attempt with unexpected " + remoteSide + " version string: " + version + ". Mod version part (" +
                    modVersion + ") does not match expected format ('MAJORMOD.MAJORAPI.MINOR.PATCH(-optionalText)'). Assuming dev " +
                    "environment or special/unknown version, connection will be allowed");
            return false;
        }
        return true;
    }

    public static ResourceLocation location(String location) {
        return new ResourceLocation(MODID, location);
    }

    public static void addConfigChangeListener(IConfigUpdateListener listener) {
        configChangeListeners.add(listener);
        //notify if the config is already there
        if (config != null) {
            listener.onConfigUpdate(config);
        }
    }

    // essentially a copy of FMLLog.bigWarning, with more lines of stacktrace
    public static void bigWarning(String format, Object... data)
    {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        LOGGER.log(Level.WARN, "****************************************");
        LOGGER.log(Level.WARN, "* "+format, data);
        for (int i = 2; i < 10 && i < trace.length; i++)
        {
            LOGGER.log(Level.WARN, "*  at {}{}", trace[i].toString(), i == 9 ? "..." : "");
        }
        LOGGER.log(Level.WARN, "****************************************");
    }

    public static class Config {

        public static enum IntOptions {
            MAX_GENERATED_CUBES_PER_TICK(1, Integer.MAX_VALUE, 49 * 16, "The number of cubic chunks to generate per tick."),
            VERTICAL_CUBE_LOAD_DISTANCE(2, 32, 8, "Similar to Minecraft's view distance, only for vertical chunks."),
            CHUNK_G_C_INTERVAL(1, Integer.MAX_VALUE, 20 * 10,
                    "Chunk garbage collector update interval. A more lower it is - a more CPU load it will generate. "
                            + "A more high it is - a more memory will be used to store cubes between launches.");

            private final int minValue;
            private final int maxValue;
            private final int defaultValue;
            private final String description;
            private int value;

            private IntOptions(int minValue1, int maxValue1, int defaultValue1, String description1) {
                minValue = minValue1;
                maxValue = maxValue1;
                defaultValue = defaultValue1;
                description = description1;
                value = defaultValue;
            }

            public float getNormalValue() {
                return (float) (value - minValue) / (maxValue - minValue);
            }

            public void setValueFromNormal(float sliderValue) {
                value = minValue + (int) ((maxValue - minValue) * sliderValue);
                config.configuration.get(Configuration.CATEGORY_GENERAL, getNicelyFormattedName(this.name()), value).set(value);
                config.configuration.save();
                for (IConfigUpdateListener l : configChangeListeners) {
                    l.onConfigUpdate(config);
                }
            }

            public int getValue() {
                return value;
            }
        }
        
        public static enum BoolOptions {
            // We need USE_FAST_COLLISION_CHECK here because if we save
            // config within mixin configuration plugin all description lines will be stripped.
            USE_FAST_ENTITY_SPAWNER(false,
                    "Enabling this option allow using fast entity spawner instead of vanilla-alike."
                            + " Fast entity spawner can reduce server lag."
                            + " In contrary entity respawn speed will be slightly slower (only one pack per tick)"
                            + " and amount of spawned mob will depend only from amount of players."),
            USE_VANILLA_CHUNK_WORLD_GENERATORS(false,
                    "Enabling this option will force " + CubicChunks.MODID
                            + " to use world generators designed for two dimensional chunks, which are often used for custom ore generators added by mods. To do so "
                            + CubicChunks.MODID + " will pregenerate cubes in a range of height from 0 to 255."),
            FORCE_CUBIC_CHUNKS(false,
                    "Enabling this will force creating a cubic chunks world, even if it's not cubic chunks world type. This option is automatically"
                            + " set in world creation GUI when creating cubic chunks world with non-cubicchunks world type");

            private final boolean defaultValue;
            private final String description;
            private boolean value;

            private BoolOptions(boolean defaultValue1, String description1) {
                defaultValue = defaultValue1;
                description = description1;
                value = defaultValue;
            }

            public boolean getValue() {
                return value;
            }

            public void flip() {
                this.value = !this.value;
            }
        }
        
        public static String getNicelyFormattedName(String name) {
            StringBuffer out = new StringBuffer();
            char char_ = '_';
            char prevchar = 0;
            for (char c : name.toCharArray()) {
                if (c != char_ && prevchar != char_) {
                    out.append(String.valueOf(c).toLowerCase());
                } else if (c != char_) {
                    out.append(String.valueOf(c));
                }
                prevchar = c;
            }
            return out.toString();
        }

        private Configuration configuration;

        private Config(Configuration configuration) {
            loadConfig(configuration);
            syncConfig();
        }

        void loadConfig(Configuration configuration) {
            this.configuration = configuration;
        }

        void syncConfig() {
            for (IntOptions configOption : IntOptions.values()) {
                configOption.value = configuration.getInt(getNicelyFormattedName(configOption.name()), Configuration.CATEGORY_GENERAL,
                        configOption.defaultValue, configOption.minValue, configOption.maxValue, configOption.description);
            }
            for (BoolOptions configOption : BoolOptions.values()) {
                configOption.value = configuration.getBoolean(getNicelyFormattedName(configOption.name()), Configuration.CATEGORY_GENERAL,
                        configOption.defaultValue, configOption.description);
            }
            if (configuration.hasChanged()) {
                configuration.save();
            }
        }

        public int getMaxGeneratedCubesPerTick() {
            return IntOptions.MAX_GENERATED_CUBES_PER_TICK.value;
        }

        public int getVerticalCubeLoadDistance() {
            return IntOptions.VERTICAL_CUBE_LOAD_DISTANCE.value;
        }

        public int getChunkGCInterval() {
            return IntOptions.CHUNK_G_C_INTERVAL.value;
        }

        public boolean useFastEntitySpawner() {
            return BoolOptions.USE_FAST_ENTITY_SPAWNER.value;
        }

        public static class GUI extends GuiConfig {

            public GUI(GuiScreen parent) {
                super(parent, new ConfigElement(config.configuration.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), MODID, false,
                        false, GuiConfig.getAbridgedConfigPath(config.configuration.toString()));
            }
        }
    }
}
