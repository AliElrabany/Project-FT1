package com.iwish.client.gui;

import com.iwish.client.Session;
import com.iwish.shared.CatalogItem;
import com.iwish.shared.Command;
import com.iwish.shared.Message;
import com.iwish.shared.WishItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;


public class AddEditItemDialog extends JDialog {

    private JComboBox<CatalogItem> catalogCombo;
    private JTextField nameField;
    private JTextField descriptionField;
    private JTextField priceField;
    private JLabel statusLabel;
    private boolean saved = false;
    private WishItem editingItem;

    public AddEditItemDialog(Frame owner, WishItem itemToEdit) {
        super(owner, itemToEdit == null ? "Add Wish List Item" : "Edit Wish List Item", true);
        this.editingItem = itemToEdit;

        setSize(440, itemToEdit == null ? 460 : 340);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        nameField = UiHelper.field();
        descriptionField = UiHelper.field();
        priceField = UiHelper.field();

        if (itemToEdit == null) {
            root.add(UiHelper.muted("Pick from the catalog (optional)"));
            catalogCombo = new JComboBox<CatalogItem>();
            catalogCombo.addItem(null);
            loadCatalog();
            catalogCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            catalogCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
            catalogCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CatalogItem picked = (CatalogItem) catalogCombo.getSelectedItem();
                    if (picked != null) {
                        nameField.setText(picked.getName());
                        descriptionField.setText(picked.getDescription());
                        priceField.setText(String.valueOf(picked.getPrice()));
                    }
                }
            });
            root.add(catalogCombo);
            root.add(Box.createVerticalStrut(16));
            root.add(new JSeparator());
            root.add(Box.createVerticalStrut(10));
        } else {
            nameField.setText(itemToEdit.getName());
            descriptionField.setText(itemToEdit.getDescription());
            priceField.setText(String.valueOf(itemToEdit.getPrice()));
        }

        root.add(labeled("Item name", nameField));
        root.add(Box.createVerticalStrut(10));
        root.add(labeled("Description", descriptionField));
        root.add(Box.createVerticalStrut(10));
        root.add(labeled("Price ($)", priceField));
        root.add(Box.createVerticalStrut(16));

        JButton saveButton = UiHelper.primaryButton(itemToEdit == null ? "Add to Wish List" : "Save Changes");
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.SMALL_FONT);
        statusLabel.setForeground(Theme.ACCENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(saveButton);
        root.add(Box.createVerticalStrut(8));
        root.add(statusLabel);

        setContentPane(root);
        getRootPane().setDefaultButton(saveButton);
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = UiHelper.muted(label);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        panel.add(l);
        panel.add(field);
        return panel;
    }

    private void loadCatalog() {
        try {
            Message request = new Message(Command.GET_CATALOG_ITEMS);
            Message response = Session.network.send(request);
            @SuppressWarnings("unchecked")
            List<CatalogItem> items = (List<CatalogItem>) response.get("items");
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    catalogCombo.addItem(items.get(i));
                }
            }
        } catch (Exception ex) {
        }
    }

    private void save() {
        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();
        String priceText = priceField.getText().trim();

        if (name.length() == 0) {
            statusLabel.setText("Please enter an item name.");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Price must be a number.");
            return;
        }
        if (price <= 0) {
            statusLabel.setText("Price must be greater than zero.");
            return;
        }

        Message request;
        if (editingItem == null) {
            request = new Message(Command.ADD_WISH_ITEM);
        } else {
            request = new Message(Command.UPDATE_WISH_ITEM);
            request.put("itemId", editingItem.getId());
        }
        request.put("userId", Session.currentUser.getId());
        request.put("name", name);
        request.put("description", description);
        request.put("price", price);

        try {
            Message response = Session.network.send(request);
            if (response.success) {
                saved = true;
                dispose();
            } else {
                statusLabel.setText(response.text);
            }
        } catch (Exception ex) {
            statusLabel.setText("Network error: " + ex.getMessage());
        }
    }

    public boolean wasSaved() {
        return saved;
    }
}