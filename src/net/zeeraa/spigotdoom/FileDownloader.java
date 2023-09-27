package net.zeeraa.spigotdoom;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

public class FileDownloader {
	public static final void downloadFile(String fileUrl, String savePath) throws FileNotFoundException, IOException {
		URL url = new URL(fileUrl);

		// Open a connection to the URL
		try (BufferedInputStream in = new BufferedInputStream(url.openStream());
				FileOutputStream fileOutputStream = new FileOutputStream(savePath)) {

			byte[] dataBuffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
			}

			in.close();
			fileOutputStream.close();
		}
	}
}