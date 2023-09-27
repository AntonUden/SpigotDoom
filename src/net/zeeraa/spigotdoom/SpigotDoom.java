package net.zeeraa.spigotdoom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.zeeraa.mochadoom.Engine;

public class SpigotDoom extends JavaPlugin {
	public static final String WAD_SOURCE = "https://ia600609.us.archive.org/16/items/DoomsharewareEpisode/doom.ZIP";

	private JFrame jFrame;
	private Engine doom;

	@Override
	public void onEnable() {
		getDataFolder().mkdir();
		File wadFile = new File(getDataFolder().getAbsoluteFile() + File.separator + "DOOM1.WAD");

		if (!wadFile.exists()) {
			getLogger().info("wad file not found. trying to download it from " + WAD_SOURCE);

			try {
				File tmpFile = new File(getDataFolder().getAbsolutePath() + File.separator + "tmp.zip");
				tmpFile.deleteOnExit();
				FileDownloader.downloadFile(WAD_SOURCE, tmpFile.getAbsolutePath());
				ZipFile zipFile = new ZipFile(tmpFile);
				ZipEntry entry = zipFile.getEntry("DOOM1.WAD");

				InputStream inputStream = zipFile.getInputStream(entry);
				OutputStream outputStream = new FileOutputStream(wadFile);
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}

				outputStream.close();
				zipFile.close();
				tmpFile.delete();
			} catch (Exception e) {
				e.printStackTrace();
				getLogger().warning("Failed to download wad file. Shutting down");
				Bukkit.getServer().getPluginManager().disablePlugin(this);
				return;
			}
		}

		jFrame = null;

		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		getLogger().info("Version is " + version);

		getLogger().info("Now lets try to fetch the JFrame using reflection");
		try {
			Field consoleField = Bukkit.getServer().getClass().getDeclaredField("console");
			consoleField.setAccessible(true);
			Object dedicatedServer = consoleField.get(Bukkit.getServer());
			getLogger().info("Dedicated server: " + dedicatedServer.getClass().getName());

			JComponent serverGUI = null;
			try {
				Field serverGUIField = dedicatedServer.getClass().getDeclaredField("gui");
				serverGUIField.setAccessible(true);
				serverGUI = (JComponent) serverGUIField.get(dedicatedServer);
			} catch (Exception e) {
				getLogger().warning("Could not find field gui. Trying to scan for the field instead");
				Class<?> guiClass = Class.forName("net.minecraft.server.gui.ServerGUI");
				Field[] fields = dedicatedServer.getClass().getDeclaredFields();
				for (Field field : fields) {
					if (field.getType().equals(guiClass)) {
						getLogger().info("Found field of type " + guiClass.getName() + ". Field name: " + field.getName());
						field.setAccessible(true);
						serverGUI = (JComponent) field.get(dedicatedServer);
						break;
					}
				}
			}

			jFrame = (JFrame) SwingUtilities.getWindowAncestor(serverGUI);
		} catch (Exception e) {
			e.printStackTrace();
			getLogger().warning("First implementation of gettting the jFrame failed");
		}

		if (jFrame == null) {
			getLogger().warning("Failed to fetch gui JFrame. Shutting down");
			Bukkit.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		getLogger().info("jFrame found. We are ready to inject doom to the console window");

		getLogger().info("Removing old components");
		jFrame.getContentPane().removeAll();
		jFrame.repaint();

		getLogger().info("Loading game engine");
		try {
			doom = new Engine(jFrame, "-iwad", wadFile.getAbsolutePath());
		} catch (IOException e) {
			getLogger().warning("Failed to init doom");
			e.printStackTrace();
			Bukkit.getServer().getPluginManager().disablePlugin(this);
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					getLogger().info("Calling DOOM.setupLoop()");
					doom.DOOM.setupLoop();
				} catch (IOException e) {
					getLogger().warning("setupLoop() failed");
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(this);
	}
}
