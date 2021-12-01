/*
 * Copyright (c) 2018 Tobias Briones. All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 *
 * This file is part of Example Project: SD Backup.
 *
 * This source code is licensed under the MIT License found in the
 * LICENSE file in the root directory of this source tree or at
 * https://opensource.org/licenses/MIT.
 */

package dev.tobiasbriones.ep.sdbackup.ui;

import dev.tobiasbriones.ep.sdbackup.model.BackupTask;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class MainWindow extends JFrame implements ActionListener {
    private static final String APP_TITLE = "SD Backup";
    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;
    private static final String ABOUT_DIALOG_STR;

    interface Controller {
        List<BackupTask> readBackupTasks();

        void newBackupTask(BackupTask backupTask) throws IOException;

        void updateTask(String oldName, BackupTask update) throws IOException;

        void deleteTask(BackupTask delete) throws IOException;

        void backup(BackupTask backupTask) throws IOException;
    }

    private final Controller controller;
    private final DefaultListModel<BackupTask> listModel;
    private final JList<BackupTask> list;

    static {
        ABOUT_DIALOG_STR = "<html><body><strong>SD Backup v0.1.0</strong><br>" +
                           "<br><strong>© Tobias Briones - SD Backup " +
                           "[2018-2021]</strong><br>" +
                           "GitHub: https://github.com/tobiasbriones/ep-sd-backup" +
                           "<br></body></html>";
    }

    public MainWindow(Controller controller) {
        super(APP_TITLE);
        this.controller = controller;
        this.listModel = new DefaultListModel<>();
        this.list = new JList<>(listModel);

        init();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Component src = (Component) e.getSource();

        if (src.getName().equals("new")) {
            onCreateNewBackupItem();
        }
        else {
            runBackup();
        }
    }

    private void init() {
        final List<BackupTask> tasks = controller.readBackupTasks();
        final JPanel panel = new JPanel();
        final JPanel aboutPanel = new JPanel();
        final JPanel bottomPanel = new JPanel();
        final JButton backupButton = new JButton("NOW");
        final JButton newTaskButton = new JButton("NEW TASK");
        final JScrollPane scroll = new JScrollPane(list);
        final JLabel aboutLabel = new JLabel();
        final MouseListener ml = new ListMouseAdapter();

        list.setCellRenderer(new TaskListCellRendered());
        list.addMouseListener(ml);
        tasks.forEach(listModel::addElement);
        backupButton.setName("backup");
        backupButton.addActionListener(this);
        newTaskButton.setName("new");
        newTaskButton.addActionListener(this);
        aboutLabel.setText("ABOUT");
        aboutLabel.setFont(aboutLabel.getFont().deriveFont(Font.BOLD, 10.0F));
        aboutLabel.setForeground(Color.decode("#404040"));
        aboutLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        aboutLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        aboutLabel.addMouseListener(new AboutMouseAdapter());
        aboutPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        aboutPanel.setBackground(Color.decode("#FFE082"));
        aboutPanel.add(aboutLabel);

        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        bottomPanel.setBackground(Color.decode("#FFE082"));
        bottomPanel.add(backupButton, BorderLayout.LINE_START);
        bottomPanel.add(newTaskButton, BorderLayout.LINE_END);
        bottomPanel.add(aboutPanel, BorderLayout.PAGE_END);

        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.decode("#FFE082"));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.PAGE_END);
        getContentPane().add(panel);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setIconImage(getToolkit().getImage("ic_app.png"));
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onCreateNewBackupItem() {
        new NewTaskDialog(this, backupTask -> {
            try {
                controller.newBackupTask(backupTask);
                listModel.addElement(backupTask);
            }
            catch (IOException ex) {
                final int mt = JOptionPane.ERROR_MESSAGE;

                JOptionPane.showMessageDialog(this, ex.getMessage(), "Fail", mt);
            }
        });
    }

    private void runBackup() {
        if (list.getSelectedValue() != null) {
            final WorkingDialog dialog = new WorkingDialog();

            dialog.execute();
        }
    }

    private final class WorkingDialog extends JDialog {
        private final BackupTask task;

        WorkingDialog() {
            super(MainWindow.this, "Working");
            this.task = list.getSelectedValue();
            final JPanel panel = new JPanel();

            panel.setBackground(Color.WHITE);
            panel.add(new JLabel("Backing up " + task.getName() + "..."));
            getContentPane().add(panel);

            pack();
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setModalityType(ModalityType.APPLICATION_MODAL);
            setLocationRelativeTo(null);
        }

        void execute() {
            final BackupTaskWorker worker = new BackupTaskWorker(task, this);

            worker.execute();
            setVisible(true);
        }
    }

    private final class BackupTaskWorker extends SwingWorker<Void, Void> {
        private final BackupTask task;
        private final Dialog dialog;
        private Exception e;

        BackupTaskWorker(BackupTask task, Dialog dialog) {
            super();
            this.task = task;
            this.dialog = dialog;
            e = null;
        }

        @Override
        protected Void doInBackground() {
            try {
                controller.backup(task);
            }
            catch (Exception e) {
                this.e = e;
            }
            return null;
        }

        @Override
        protected void done() {
            dialog.dispose();

            if (e == null) {
                final String msg = "Backup completed.";

                JOptionPane.showMessageDialog(
                    MainWindow.this,
                    msg,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            else {
                JOptionPane.showMessageDialog(
                    MainWindow.this,
                    e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private final class ListMouseAdapter extends MouseAdapter {
        private ListMouseAdapter() {
            super();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                final BackupTask edit = list.getSelectedValue();

                new TaskEditDialog(
                    MainWindow.this,
                    edit,
                    (oldName, update) -> {
                        controller.updateTask(oldName, update);
                        list.updateUI();
                    }
                );
            }
            else if (SwingUtilities.isRightMouseButton(e)) {
                final JPopupMenu popup = new JPopupMenu();
                final JMenuItem deleteItem = new JMenuItem("Delete");

                popup.add(deleteItem);
                list.setSelectedIndex(list.locationToIndex(e.getPoint()));
                popup.show(list, e.getX(), e.getY());
                final BackupTask delete = list.getSelectedValue();

                if (delete == null) {
                    return;
                }
                deleteItem.addActionListener(e1 -> {
                    try {
                        controller.deleteTask(delete);
                        listModel.removeElement(delete);
                    }
                    catch (IOException ex) {
                        final MainWindow mw = MainWindow.this;

                        JOptionPane.showMessageDialog(
                            mw,
                            ex.getMessage(),
                            "Fail",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                });
            }
        }
    }

    private final class AboutMouseAdapter extends MouseAdapter {
        private AboutMouseAdapter() {
            super();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            final JDialog about = new JDialog(MainWindow.this);
            final JPanel panel = new JPanel();
            final JButton acceptButton = new JButton("ACCEPT");

            acceptButton.addActionListener((e1) -> about.dispose());

            panel.setLayout(new BorderLayout());
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            panel.setBackground(Color.WHITE);
            panel.add(new JLabel(ABOUT_DIALOG_STR), BorderLayout.CENTER);
            panel.add(acceptButton, BorderLayout.PAGE_END);

            about.add(panel);
            about.pack();
            about.setLocationRelativeTo(null);
            about.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            about.setVisible(true);
        }
    }

    private static final class TaskListCellRendered extends DefaultListCellRenderer {
        private final JPanel[] panels;
        private final JPanel sdPanel;
        private final JLabel nameLabel;
        private final JLabel targetLabel;
        private final JLabel destinationsLabel;
        private final JLabel sdOwnerLabel;
        private final JLabel sdTypeLabel;

        private TaskListCellRendered() {
            super();
            final JPanel panel = new JPanel();
            final JPanel contentPanel = new JPanel();
            final JPanel infoPanel = new JPanel();
            final JPanel valuePanel = new JPanel();
            this.panels = new JPanel[] {
                panel,
                contentPanel,
                infoPanel,
                valuePanel
            };
            this.sdPanel = new JPanel();
            this.nameLabel = new JLabel();
            this.targetLabel = new JLabel();
            this.destinationsLabel = new JLabel();
            this.sdOwnerLabel = new JLabel();
            this.sdTypeLabel = new JLabel();

            sdOwnerLabel.setForeground(Color.decode("#303F9F"));
            sdOwnerLabel.setFont(sdOwnerLabel.getFont().deriveFont(Font.ITALIC));
            sdTypeLabel.setForeground(Color.decode("#7B1FA2"));
            sdPanel.setLayout(new GridLayout(2, 2, 0, 10));
            sdPanel.setBackground(Color.WHITE);
            sdPanel.setBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.decode("#D0D0D0"))
            );
            sdPanel.add(new JLabel("Owner"));
            sdPanel.add(sdOwnerLabel);
            sdPanel.add(new JLabel(("Type")));
            sdPanel.add(sdTypeLabel);

            infoPanel.setLayout(new GridLayout(3, 1, 0, 10));
            infoPanel.setBackground(Color.WHITE);
            infoPanel.add(new JLabel("Name"));
            infoPanel.add(new JLabel("Target"));
            infoPanel.add(new JLabel("Destinations"));

            valuePanel.setLayout(new GridLayout(3, 1, 0, 10));
            valuePanel.setBorder(new EmptyBorder(0, 56, 0, 20));
            valuePanel.setBackground(Color.WHITE);
            valuePanel.add(nameLabel);
            valuePanel.add(targetLabel);
            valuePanel.add(destinationsLabel);

            contentPanel.setLayout(new BorderLayout());
            contentPanel.setBorder(new EmptyBorder(10, 10, 20, 10));
            contentPanel.setBackground(Color.WHITE);
            contentPanel.add(infoPanel, BorderLayout.LINE_START);
            contentPanel.add(valuePanel, BorderLayout.CENTER);
            contentPanel.add(sdPanel, BorderLayout.LINE_END);

            panel.setLayout(new BorderLayout());
            panel.setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#737373"))
            );
            panel.add(contentPanel);
        }

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            final BackupTask task = (BackupTask) value;
            final StringBuilder builder = new StringBuilder(10);

            nameLabel.setText(task.getName());
            targetLabel.setText(task.getTarget().toString());
            destinationsLabel.setText("");

            task.forEach(d -> {
                builder.append(", ");
                builder.append(d);
            });
            String dl = builder.toString();

            if (dl.contains(", ")) {
                dl = dl.substring(2);
            }
            destinationsLabel.setText(dl);
            if (task.isSdBackup()) {
                sdOwnerLabel.setText(task.getSdOwner());
                sdTypeLabel.setText(task.getSdType());
                sdPanel.setVisible(true);
            }
            else {
                sdPanel.setVisible(false);
            }
            if (isSelected) {
                sdPanel.setBackground(Color.decode("#B39DDB"));
                for (JPanel panel : panels) {
                    panel.setBackground(Color.decode("#B39DDB"));
                }
            }
            else {
                sdPanel.setBackground(Color.WHITE);
                for (JPanel panel : panels) {
                    panel.setBackground(Color.WHITE);
                }
            }
            return panels[0];
        }
    }

    private static final class NewTaskDialog extends JDialog {
        interface Callback {
            void save(BackupTask backupTask);
        }

        NewTaskDialog(MainWindow mw, Callback callback) {
            super(mw, "Create new task");
            final JPanel panel = new JPanel();
            final JPanel formPanel = new JPanel();
            final JPanel actionsPanel = new JPanel();
            final JTextField nameTF = new JTextField();
            final JTextField targetTF = new JTextField();
            final JTextField destinationsTF = new JTextField(60);
            final JButton cancelButton = new JButton("Cancel");
            final JButton saveButton = new JButton("Save");
            final ActionListener l = (e) -> {
                if (e.getSource() == saveButton) {
                    final String name = nameTF.getText();
                    final File target = new File(targetTF.getText());
                    final String[] destinations = destinationsTF.getText().split(";");

                    if (targetTF.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(mw, "Empty target!");
                        return;
                    }
                    if (destinationsTF.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(mw, "Empty destinations!");
                        return;
                    }
                    final BackupTask backupTask = new BackupTask();

                    backupTask.setName(name);
                    backupTask.setTarget(target);
                    for (String destination : destinations) {
                        backupTask.addDestination(new File(destination));
                    }
                    callback.save(backupTask);
                }
                dispose();
            };

            cancelButton.addActionListener(l);
            saveButton.addActionListener(l);

            formPanel.setLayout(new GridLayout(6, 1));
            formPanel.setBackground(Color.WHITE);
            formPanel.add(new JLabel("Name"));
            formPanel.add(nameTF);
            formPanel.add(new JLabel("Target folder"));
            formPanel.add(targetTF);
            formPanel.add(new JLabel("Backup destinations"));
            formPanel.add(destinationsTF);

            actionsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 5));
            actionsPanel.setBackground(Color.WHITE);
            actionsPanel.add(cancelButton, BorderLayout.LINE_START);
            actionsPanel.add(saveButton, BorderLayout.LINE_START);

            panel.setLayout(new BorderLayout());
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            panel.setBackground(Color.WHITE);
            panel.add(formPanel, BorderLayout.CENTER);
            panel.add(actionsPanel, BorderLayout.PAGE_END);
            getContentPane().add(panel);

            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }
    }

    private static final class TaskEditDialog extends JDialog {
        interface Callback {
            void update(String oldName, BackupTask update) throws IOException;

        }

        TaskEditDialog(
            MainWindow mw,
            BackupTask edit,
            Callback callback
        ) {
            super(mw, "Edit - " + edit.getName());
            final String oldName = edit.getName();
            final JPanel panel = new JPanel();
            final JPanel editPanel = new JPanel();
            final JPanel actionsPanel = new JPanel();
            final JTextField nameTF = new JTextField();
            final JTextField targetTF = new JTextField();
            final JTextField destinationsTF = new JTextField(60);
            final JButton cancelButton = new JButton("Cancel");
            final JButton saveButton = new JButton("Save");
            final ActionListener l = (e) -> {
                if (e.getSource() == saveButton) {
                    final String name = nameTF.getText();
                    final File target = new File(targetTF.getText());
                    final String[] destinations = destinationsTF.getText().split(";");

                    if (targetTF.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(mw, "Empty target!");
                        return;
                    }
                    if (destinationsTF.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(mw, "Empty destinations!");
                        return;
                    }

                    edit.clear();
                    edit.setName(name);
                    edit.setTarget(target);
                    for (String destination : destinations) {
                        edit.addDestination(new File(destination));
                    }
                    try {
                        callback.update(oldName, edit);
                    }
                    catch (IOException ex) {
                        JOptionPane.showMessageDialog(
                            mw,
                            ex.getMessage(),
                            "Fail",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
                dispose();
            };

            nameTF.setText(edit.getName());
            targetTF.setText(edit.getTarget().toString());
            edit.forEach(file -> destinationsTF
                .setText(destinationsTF.getText() + ";" + file));

            if (
                destinationsTF.getText().length() > 0 &&
                destinationsTF.getText().charAt(0) == ';'
            ) {
                destinationsTF.setText(destinationsTF.getText().substring(1));
            }
            cancelButton.addActionListener(l);
            saveButton.addActionListener(l);

            editPanel.setLayout(new GridLayout(6, 1));
            editPanel.setBackground(Color.WHITE);
            editPanel.add(new JLabel("Name"));
            editPanel.add(nameTF);
            editPanel.add(new JLabel("Target folder"));
            editPanel.add(targetTF);
            editPanel.add(new JLabel("Backup destinations"));
            editPanel.add(destinationsTF);

            actionsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 5));
            actionsPanel.setBackground(Color.WHITE);
            actionsPanel.add(cancelButton, BorderLayout.LINE_START);
            actionsPanel.add(saveButton, BorderLayout.LINE_START);

            panel.setLayout(new BorderLayout());
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            panel.setBackground(Color.WHITE);
            panel.add(editPanel, BorderLayout.CENTER);
            panel.add(actionsPanel, BorderLayout.PAGE_END);
            getContentPane().add(panel);

            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }
    }
}
