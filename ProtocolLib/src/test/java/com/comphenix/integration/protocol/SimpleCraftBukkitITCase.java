package com.comphenix.integration.protocol;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.reflect.FieldUtils;
import com.google.common.collect.Lists;

// Damn final classes ...
@RunWith(org.powermock.modules.junit4.PowerMockRunner.class)
@PrepareForTest(PluginDescriptionFile.class)
public class SimpleCraftBukkitITCase {	
	// The fake plugin
	private static volatile Plugin FAKE_PLUGIN = null;
	
	/**
	 * The maximum amount of time to wait before a server has started.
	 * <p>
	 * We have to give it the ample time of 60 seconds, as a server may have to 
	 * generate the spawn area in three worlds.
	 */
	private static final int TIMEOUT_MS = 60000;
	
	/**
	 * Setup the CraftBukkit server for all the tests.
	 * @throws IOException Unable to setup server.
	 * @throws InterruptedException Thread interrupted.
	 */
	@BeforeClass
	public static void setupCraftBukkit() throws Exception {
		setupPlugins();
		org.bukkit.craftbukkit.Main.main(new String[0]);
		
		// We need to wait until the server object is ready
		while (Bukkit.getServer() == null)
			Thread.sleep(1);
		
		// Make it clear this plugin doesn't exist
		FAKE_PLUGIN = createPlugin("FakeTestPluginIntegration");
	
		// No need to look for updates
		FieldUtils.writeStaticField(ProtocolLibrary.class, "UPDATES_DISABLED", Boolean.TRUE, true);
		
		// Wait until the server and all the plugins have loaded
		Bukkit.getScheduler().callSyncMethod(FAKE_PLUGIN, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return null;
			}
		}).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
	
		// Plugins are now ready
	}
	
	/**
	 * Close the CraftBukkit server when they're done.
	 */
	@AfterClass
	public static void shutdownCraftBukkit() {
		Bukkit.shutdown();
	}
	
	@Test
	public void testPingPacket() throws Throwable {
		TestPingPacket.newTest().startTest(FAKE_PLUGIN);
	}
	
	/**
	 * Copy ProtocolLib into the plugins folder.
	 * @throws IOException If anything went wrong.
	 */
	private static void setupPlugins() throws IOException {
		File pluginDirectory = new File("plugins/");
		File bestFile = null;
		int bestLength = Integer.MAX_VALUE;
		
		// Copy the ProtocolLib plugin to the server
		FileUtils.cleanDirectory(pluginDirectory);
		
		for (File file : new File("../").listFiles()) {
			String name = file.getName();
			
			if (name.startsWith("ProtocolLib") && name.length() < bestLength) {
				bestLength = name.length();
				bestFile = file;
			}
		}
		FileUtils.copyFile(bestFile, new File(pluginDirectory, bestFile.getName()));
	}
	
	/**
	 * Create a mockable plugin for all the tests.
	 * @param fakePluginName - the fake plugin name.
	 * @return The plugin.
	 */
	private static Plugin createPlugin(String fakePluginName) {
		Plugin plugin = mock(Plugin.class);
		PluginDescriptionFile description = mock(PluginDescriptionFile.class);
		
		when(description.getDepend()).thenReturn(Lists.newArrayList("ProtocolLib"));
		when(description.getSoftDepend()).thenReturn(Collections.<String>emptyList());
		when(description.getLoadBefore()).thenReturn(Collections.<String>emptyList());
		when(description.getLoad()).thenReturn(PluginLoadOrder.POSTWORLD);
		
		when(plugin.getName()).thenReturn(fakePluginName);
		when(plugin.getServer()).thenReturn(Bukkit.getServer());
		when(plugin.isEnabled()).thenReturn(true);
		when(plugin.getDescription()).thenReturn(description);
		return plugin;
	}
}
