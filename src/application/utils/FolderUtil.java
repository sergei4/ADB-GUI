package application.utils;

import application.preferences.Preferences;

import java.io.File;

public class FolderUtil {

	public static File getApkFolder() {

		File apkFolder = new File(Preferences.getInstance().getPrimaryAPKFolder());
		if (!apkFolder.exists()){
			apkFolder.mkdir();
		}

		return apkFolder;
	}

	public static File getSnapshotFolder() {

		File folder = new File(Preferences.getInstance().getSnapshotFolder());
		if (!folder.exists()){
			folder.mkdir();
		}

		return folder;
	}

	public static File getLogsFolder() {
		File logFolder = new File(Preferences.getInstance().getLogsFolder());
		if (!logFolder.exists()) {
			logFolder.mkdir();
		}
		return logFolder;
	}
}
