/*
 * This file is part of PlaceholderAPI
 *
 * PlaceholderAPI
 * Copyright (c) 2015 - 2024 PlaceholderAPI Team
 *
 * PlaceholderAPI free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlaceholderAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.clip.placeholderapi.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;

/**
 * Utility class for managing tasks in a way that's compatible with both Folia and Bukkit
 */
public final class SchedulerUtil {

    private static final boolean IS_FOLIA;
    private static Method GET_GLOBAL_REGION_SCHEDULER;
    private static Method GET_ASYNC_SCHEDULER;
    private static Method GET_REGION_SCHEDULER;

    static {
        boolean isFolia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            GET_GLOBAL_REGION_SCHEDULER = Bukkit.class.getMethod("getGlobalRegionScheduler");
            GET_ASYNC_SCHEDULER = Bukkit.class.getMethod("getAsyncScheduler");
            GET_REGION_SCHEDULER = Bukkit.class.getMethod("getRegionScheduler");
            isFolia = true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            isFolia = false;
        }
        IS_FOLIA = isFolia;
    }

    private SchedulerUtil() {
        // Utility class
    }

    /**
     * Check if the server is using Folia
     * 
     * @return true if the server is using Folia, false otherwise
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }

    /**
     * Run a task on the main thread (global for Folia)
     * 
     * @param plugin The plugin
     * @param runnable The task to run
     */
    public static void runTask(Plugin plugin, Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        
        if (IS_FOLIA) {
            try {
                Object scheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(null);
                scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class).invoke(scheduler, plugin, runnable);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Schedule a task to run later on the main thread (global for Folia)
     * 
     * @param plugin The plugin
     * @param runnable The task to run
     * @param delay The delay in ticks
     */
    public static void runTaskLater(Plugin plugin, Runnable runnable, long delay) {
        if (IS_FOLIA) {
            try {
                Object scheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(null);
                Class<?> taskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
                scheduler.getClass().getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, long.class)
                        .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), delay);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    /**
     * Run a task asynchronously
     * 
     * @param plugin The plugin
     * @param runnable The task to run
     */
    public static void runTaskAsync(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            try {
                Object scheduler = GET_ASYNC_SCHEDULER.invoke(null);
                Class<?> taskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
                scheduler.getClass().getMethod("runNow", Plugin.class, java.util.function.Consumer.class)
                        .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    /**
     * Cancel all tasks from the plugin
     * 
     * @param plugin The plugin
     */
    public static void cancelTasks(Plugin plugin) {
        if (IS_FOLIA) {
            try {
                Object globalScheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(null);
                globalScheduler.getClass().getMethod("cancelTasks", Plugin.class).invoke(globalScheduler, plugin);
                
                Object asyncScheduler = GET_ASYNC_SCHEDULER.invoke(null);
                asyncScheduler.getClass().getMethod("cancelTasks", Plugin.class).invoke(asyncScheduler, plugin);
                
                Object regionScheduler = GET_REGION_SCHEDULER.invoke(null);
                regionScheduler.getClass().getMethod("cancelTasks", Plugin.class).invoke(regionScheduler, plugin);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                Bukkit.getScheduler().cancelTasks(plugin);
            }
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }
} 