// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.libs.generic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Forwards mouse events to another component, for example from a tab icon to its tabbed pane.
// This keeps tab selection working when a child component would otherwise swallow the event.
public class MouseEventForwarder extends MouseAdapter {
    private final Component target;

    public MouseEventForwarder(Component target) {
        this.target = target;
    }

    private void forward(MouseEvent e) {
        target.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, target));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        forward(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        forward(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        forward(e);
    }
}
