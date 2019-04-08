package application.ui.simple.devices;

public interface InstallApkListener {
    void onStartInstall();

    void onSuccessInstall();

    void onFailedInstall(String result);
}
