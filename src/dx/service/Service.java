package dx.service;

public interface Service {

    interface Listener {

        void onStart();

        void onStop();
    }

    void start();

    void stop();

    boolean isRunning();
}
