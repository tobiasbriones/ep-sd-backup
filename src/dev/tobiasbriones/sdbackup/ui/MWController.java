/*
 * Copyright (c) 2018 Tobias Briones. All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 *
 * This file is part of SD Backup.
 *
 * This source code is licensed under the MIT License found in the
 * LICENSE file in the root directory of this source tree or at
 * https://opensource.org/licenses/MIT.
 */

package dev.tobiasbriones.sdbackup.ui;

import dev.tobiasbriones.sdbackup.FileUtils;
import dev.tobiasbriones.sdbackup.model.BackupTask;

import javax.swing.*;
import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class MWController implements MainWindow.Controller {
    private static final String TASKS_FOLDER = "tasks";
    private final List<BackupTask> backupTasks;

    public MWController() {
        this.backupTasks = new ArrayList<>();

        init();
    }

    @Override
    public List<BackupTask> readBackupTasks() {
        return Collections.unmodifiableList(backupTasks);
    }

    @Override
    public void newBackupTask(BackupTask backupTask) throws IOException {
        for (BackupTask task : backupTasks) {
            if (task.getName().equals(backupTask.getName())) {
                throw new IOException("Task name already in use!");
            }
        }
        backupTasks.add(backupTask);
        saveTasks();
    }

    @Override
    public void updateTask(String oldName, BackupTask update) throws IOException {
        final File file = new File(getFolder(TASKS_FOLDER), update.getName());

        try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(update);
        }
        if (!(oldName.equals(update.getName()) || new File(getFolder(TASKS_FOLDER), oldName).delete())) {
            throw new IOException("It couldn't delete old backup task");
        }
    }

    @Override
    public void deleteTask(BackupTask delete) throws IOException {
        if (!new File(getFolder(TASKS_FOLDER), delete.getName()).delete()) {
            throw new IOException("Fail to delete");
        }
        backupTasks.remove(delete);
    }

    @Override
    public void backup(BackupTask backupTask) throws IOException {
        final File originFolder = backupTask.getTarget();
        final SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH_mm");
        final String date = sdf.format(new Date()).replace(':', '_').replace('/', '.');
        final String sdPath = backupTask.isSdBackup() ? backupTask.getSdPath() : "";

        if (!(originFolder.exists() && originFolder.isDirectory())) {
            throw new IOException("Target doesn't exist or is not a directory");
        }
        for (File destination : backupTask) {
            final File finalDestination = Paths.get(destination.getAbsolutePath(), sdPath, date).toFile();
            FileUtils.copyDirectory(originFolder, finalDestination);
        }
    }

    private void init() {
        try {
            loadTasks();
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Fail", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveTasks() throws IOException {
        final File folder = getFolder(TASKS_FOLDER);
        File currentFile;

        for (final BackupTask task : backupTasks) {
            currentFile = new File(folder, task.getName());

            try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(currentFile))) {
                oos.writeObject(task);
            }
        }
    }

    private void loadTasks() throws IOException {
        final File[] files = getFolder(TASKS_FOLDER).listFiles();

        if (files == null) {
            throw new IOException("Fail to read data");
        }
        for (File file : files) {
            try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                backupTasks.add((BackupTask) ois.readObject());
            }
            catch (ClassNotFoundException e) {
                throw new IOException("Class not found. " + e.getMessage());
            }
        }
    }

    private static File getFolder(String path) throws IOException {
        final File file = new File(path);

        if ((file.exists() && !file.isDirectory()) || !file.exists()) {
            if (!file.mkdirs()) {
                throw new IOException("Couldn't create folder " + path);
            }
        }
        return file;
    }
}
