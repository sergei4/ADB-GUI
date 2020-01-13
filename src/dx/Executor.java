package dx;

import application.log.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Executor {

    public static String run(String[] command) {
        return run(null, command);
    }

    public static String run(String command) {
        return run(command, null);
    }

    private static String run(String command, String[] commands) {
        //Logger.d("Run: " + command);

        StringBuilder output = new StringBuilder();

        Process p;
        try {
            String[] envp = {};
            if (commands == null) {
                p = Runtime.getRuntime().exec(command, envp);
            } else {
                p = Runtime.getRuntime().exec(commands, envp);
            }
            //p.waitFor(10, TimeUnit.SECONDS);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            boolean firstLine = true;
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (!firstLine) {
                    output.append("\n");
                }
                firstLine = false;
                output.append(line);
                //System.out.println(line);
            }

            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line = reader.readLine()) != null) {
                if (!firstLine) {
                    output.append("\n");
                }
                firstLine = false;
                output.append(line).append("\n");
                //System.out.println(line);
            }

        } catch (Exception e) {
            Logger.e("Run command: " + command);
            e.printStackTrace();
            output.append(e.getMessage());
        }

        return output.toString();
    }

    public static rx.Observable<String> observeProcess(String cmd) {
        return rx.Observable.<String>unsafeCreate(subscriber -> {
            try {
                String[] envp = {};
                Process process = Runtime.getRuntime().exec(cmd, envp);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while (!subscriber.isUnsubscribed() && (line = reader.readLine()) != null) {
                    subscriber.onNext(line);
                }
                process.destroy();
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }
}
