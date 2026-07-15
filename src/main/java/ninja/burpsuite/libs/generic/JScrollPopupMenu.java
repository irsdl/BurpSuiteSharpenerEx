// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

// from https://stackoverflow.com/questions/9288350/adding-vertical-scroll-to-a-jpopupmenu

package ninja.burpsuite.libs.generic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

public final class JScrollPopupMenu extends JPopupMenu {
    private static final long serialVersionUID = 1L;
    protected int maximumVisibleRows = 10;

    public JScrollPopupMenu() {
        this(null);
    }

    public JScrollPopupMenu(String label) {
        super(label);
        setLayout(new ScrollPopupMenuLayout());

        super.add(getScrollBar());
        addMouseWheelListener(event -> {
            JScrollBar scrollBar = getScrollBar();
            int amount = (event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
                    ? event.getUnitsToScroll() * scrollBar.getUnitIncrement()
                    : (event.getWheelRotation() < 0 ? -1 : 1) * scrollBar.getBlockIncrement();

            scrollBar.setValue(scrollBar.getValue() + amount);
            event.consume();
        });
    }

    private JScrollBar popupScrollBar;

    protected JScrollBar getScrollBar() {
        if (popupScrollBar == null) {
            popupScrollBar = new JScrollBar(Adjustable.VERTICAL);
            popupScrollBar.addAdjustmentListener(e -> {
                doLayout();
                repaint();
            });

            popupScrollBar.setVisible(false);
        }

        return popupScrollBar;
    }

    public int getMaximumVisibleRows() {
        return maximumVisibleRows;
    }

    public void setMaximumVisibleRows(int maximumVisibleRows) {
        this.maximumVisibleRows = maximumVisibleRows;
    }

    @Override
    public void paintChildren(Graphics g) {
        Insets insets = getInsets();
        g.clipRect(insets.left, insets.top, getWidth(), getHeight() - insets.top - insets.bottom);
        super.paintChildren(g);
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);

        if (maximumVisibleRows < getComponentCount() - 1) {
            getScrollBar().setVisible(true);
        }
    }

    @Override
    public void remove(int index) {
        // can't remove the scrollbar
        ++index;

        super.remove(index);

        if (maximumVisibleRows >= getComponentCount() - 1) {
            getScrollBar().setVisible(false);
        }
    }

    @Override
    public void show(Component invoker, int x, int y) {
        applySizingForVisibleRows();
        super.show(invoker, x, y);
        shrinkToFitScreen();
    }

    private void applySizingForVisibleRows() {
        JScrollBar scrollBar = getScrollBar();
        if (scrollBar.isVisible()) {
            int extent = 0;
            int max = 0;
            int i = 0;
            int unit = -1;
            int width = 0;
            for (Component comp : getComponents()) {
                if (!(comp instanceof JScrollBar)) {
                    Dimension preferredSize = comp.getPreferredSize();
                    width = Math.max(width, preferredSize.width);
                    if (unit < 0) {
                        unit = preferredSize.height;
                    }
                    if (i++ < maximumVisibleRows) {
                        extent += preferredSize.height;
                    }
                    max += preferredSize.height;
                }
            }

            Insets insets = getInsets();
            int widthMargin = insets.left + insets.right;
            int heightMargin = insets.top + insets.bottom;
            scrollBar.setUnitIncrement(unit);
            scrollBar.setBlockIncrement(extent);
            scrollBar.setValues(0, heightMargin + extent, 0, heightMargin + max);

            width += scrollBar.getPreferredSize().width + widthMargin;
            int height = heightMargin + extent;

            setPopupSize(new Dimension(width, height));
        }
    }

    // After the popup is shown, the component sizes are final (the LAF may scale them
    // only at that point, for example on high DPI screens). When the popup runs over
    // the screen edge, the visible rows are reduced and the scrollbar is shown, so no
    // menu item is ever out of reach. On screens where the menu fits, nothing changes.
    private void shrinkToFitScreen() {
        if (!isShowing())
            return;

        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        if (graphicsConfiguration == null)
            return;

        Rectangle screenBounds = graphicsConfiguration.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
        int screenBottom = screenBounds.y + screenBounds.height - screenInsets.bottom;

        int overflow = getLocationOnScreen().y + getHeight() - screenBottom;
        if (overflow <= 0)
            return;

        Insets insets = getInsets();
        int availableHeight = getHeight() - overflow - insets.top - insets.bottom;
        int fittingRows = countRowsThatFit(getComponents(), availableHeight);

        maximumVisibleRows = Math.max(3, fittingRows);
        getScrollBar().setVisible(true);
        // setPopupSize inside this call resizes the popup while it is showing
        applySizingForVisibleRows();
        revalidate();
        repaint();
    }

    // package-private so it can be unit tested without showing a popup
    static int countRowsThatFit(Component[] components, int availableHeight) {
        int rows = 0;
        int usedHeight = 0;
        for (Component comp : components) {
            if (comp instanceof JScrollBar)
                continue;
            usedHeight += comp.getPreferredSize().height;
            if (usedHeight > availableHeight)
                break;
            rows++;
        }
        return rows;
    }

    protected static class ScrollPopupMenuLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            int visibleAmount = Integer.MAX_VALUE;
            Dimension dim = new Dimension();
            for (Component comp : parent.getComponents()) {
                if (comp.isVisible()) {
                    if (comp instanceof JScrollBar scrollBar) {
                        visibleAmount = scrollBar.getVisibleAmount();
                    } else {
                        Dimension pref = comp.getPreferredSize();
                        dim.width = Math.max(dim.width, pref.width);
                        dim.height += pref.height;
                    }
                }
            }

            Insets insets = parent.getInsets();
            dim.height = Math.min(dim.height + insets.top + insets.bottom, visibleAmount);

            return dim;
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            int visibleAmount = Integer.MAX_VALUE;
            Dimension dim = new Dimension();
            for (Component comp : parent.getComponents()) {
                if (comp.isVisible()) {
                    if (comp instanceof JScrollBar scrollBar) {
                        visibleAmount = scrollBar.getVisibleAmount();
                    } else {
                        Dimension min = comp.getMinimumSize();
                        dim.width = Math.max(dim.width, min.width);
                        dim.height += min.height;
                    }
                }
            }

            Insets insets = parent.getInsets();
            dim.height = Math.min(dim.height + insets.top + insets.bottom, visibleAmount);

            return dim;
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();

            int width = parent.getWidth() - insets.left - insets.right;
            int height = parent.getHeight() - insets.top - insets.bottom;

            int x = insets.left;
            int y = insets.top;
            int position = 0;

            for (Component comp : parent.getComponents()) {
                if ((comp instanceof JScrollBar scrollBar) && comp.isVisible()) {
                    Dimension dim = scrollBar.getPreferredSize();
                    scrollBar.setBounds(x + width - dim.width, y, dim.width, height);
                    width -= dim.width;
                    position = scrollBar.getValue();
                }
            }

            y -= position;
            for (Component comp : parent.getComponents()) {
                if (!(comp instanceof JScrollBar) && comp.isVisible()) {
                    Dimension pref = comp.getPreferredSize();
                    comp.setBounds(x, y, width, pref.height);
                    y += pref.height;
                }
            }
        }
    }
}