package res;

import javafx.scene.text.Font;

public class R {

    public static class Fonts {
        public static Font courierFont13;

        static {
            courierFont13 = Font.loadFont(R.class.getResource("fonts/cour.ttf").toString(), 13);
        }
    }
}
