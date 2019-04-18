package application.ui.simple;

import javafx.scene.control.ToggleButton;

/**
 * Company: DevExperts
 * Date: 18-Apr-19
 */
public class RadioToggleButton extends ToggleButton {
    @Override
    public void fire() {
        // we don't toggle from selected to not selected if part of a group
        if (getToggleGroup() == null || !isSelected()) {
            super.fire();
        }
    }
}
