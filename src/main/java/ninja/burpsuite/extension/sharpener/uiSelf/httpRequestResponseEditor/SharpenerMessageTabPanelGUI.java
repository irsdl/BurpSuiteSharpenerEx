package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import javax.swing.*;
import java.awt.*;

public class SharpenerMessageTabPanelGUI {
    private JComboBox<String> comboBoxActions;
    public JPanel sharpenerMessageMainJPanel;
    public JPanel topPanel;
    public JPanel middlePanel;
    public JPanel bottomPanel;
    public SharpenerMessageEditor_RTextScrollPane sharpenerMessageEditor_RTextScrollPane;
    public JPanel rTextScrollPaneJPanel;

    private ExtensionSharedParameters sharedParameters;

    public JPanel getMainPanel() {
        return sharpenerMessageMainJPanel;
    }

    public SharpenerMessageTabPanelGUI(ExtensionSharedParameters sharedParameters, boolean readOnly) {
        this.sharedParameters = sharedParameters;
        sharpenerMessageEditor_RTextScrollPane = new SharpenerMessageEditor_RTextScrollPane(sharedParameters);
        $$$setupUI$$$();
        middlePanel.setPreferredSize(new Dimension(150, 20));
        rTextScrollPaneJPanel.setPreferredSize(new Dimension(100, 20));
    }

    public boolean isModified() {
        return false;
    }

    private void createUIComponents() {

    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        sharpenerMessageMainJPanel = new JPanel();
        sharpenerMessageMainJPanel.setLayout(new BorderLayout(0, 0));
        sharpenerMessageMainJPanel.setAutoscrolls(false);
        sharpenerMessageMainJPanel.setInheritsPopupMenu(false);
        topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        topPanel.setMinimumSize(new Dimension(84, 30));
        topPanel.setPreferredSize(new Dimension(78, 30));
        sharpenerMessageMainJPanel.add(topPanel, BorderLayout.NORTH);
        comboBoxActions = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("test1");
        defaultComboBoxModel1.addElement("test2");
        comboBoxActions.setModel(defaultComboBoxModel1);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(comboBoxActions, gbc);
        middlePanel = new JPanel();
        middlePanel.setLayout(new BorderLayout(0, 0));
        middlePanel.setPreferredSize(new Dimension(0, 0));
        middlePanel.setRequestFocusEnabled(true);
        sharpenerMessageMainJPanel.add(middlePanel, BorderLayout.CENTER);
        rTextScrollPaneJPanel = new JPanel();
        rTextScrollPaneJPanel.setLayout(new BorderLayout(0, 0));
        rTextScrollPaneJPanel.setOpaque(true);
        middlePanel.add(rTextScrollPaneJPanel, BorderLayout.CENTER);
        rTextScrollPaneJPanel.add(sharpenerMessageEditor_RTextScrollPane, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        panel1.setOpaque(true);
        panel1.setPreferredSize(new Dimension(10, 0));
        middlePanel.add(panel1, BorderLayout.EAST);
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        sharpenerMessageMainJPanel.add(bottomPanel, BorderLayout.SOUTH);
        final JLabel label1 = new JLabel();
        label1.setText("Status");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        bottomPanel.add(label1, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(spacer1, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        bottomPanel.add(spacer2, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return sharpenerMessageMainJPanel;
    }

}
