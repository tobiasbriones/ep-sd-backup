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

package dev.tobiasbriones.ep.sdbackup.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Defines a backup task item that provides the target source and list of
 * destinations where the source is to be copied.
 *
 * @author Tobias Briones
 */
public final class BackupTask implements Serializable, Iterable<File> {
    private static final String SD_DIR_NAME = "Software Development";
    private static final int INITIAL_CAPACITY = 3;
    private static final char SEP_CHAR = '\\';
    private static final long serialVersionUID = 1L;
    private final List<File> destinations;
    private String name;
    private File target;
    private String sdPath;
    private String sdOwner;
    private String sdType;

    public BackupTask() {
        this.destinations = new ArrayList<>(INITIAL_CAPACITY);
        this.name = "";
        this.target = null;
        this.sdPath = null;
        this.sdOwner = null;
        this.sdType = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public File getTarget() {
        return target;
    }

    public void setTarget(File value) {
        this.target = value;

        updateSdPath();
    }

    public String getSdPath() {
        return sdPath;
    }

    public String getSdOwner() {
        return sdOwner;
    }

    public String getSdType() {
        return sdType;
    }

    /**
     * Returns the SD Backup path or null. If a parent directory called
     * "Software Development" exists then this value is present.
     *
     * @return the SD Backup path or null
     */
    public boolean isSdBackup() {
        return sdPath != null;
    }

    @Override
    public Iterator<File> iterator() {
        return destinations.iterator();
    }

    public void addDestination(File destination) {
        destinations.add(destination);
    }

    public void clear() {
        name = "";
        target = null;
        sdPath = null;
        sdOwner = null;
        sdType = null;

        destinations.clear();
    }

    private void updateSdPath() {
        File parent = target;
        boolean hasFound = false;

        while (!hasFound && (parent = parent.getParentFile()) != null) {
            if (parent.getName().equals(SD_DIR_NAME)) {
                hasFound = true;
            }
        }
        if (hasFound) {
            final String totalPath = target.getAbsolutePath();
            final String rootPath = parent.getAbsolutePath();

            sdPath = totalPath.substring(rootPath.length());

            if (sdPath.charAt(0) == SEP_CHAR) {
                sdPath = sdPath.substring(1);
            }
            final String[] x = sdPath.split("\\\\");

            try {
                sdOwner = x[0];
                sdType = x[1];
                name = x[x.length - 1];
            }
            catch (IndexOutOfBoundsException ignore) {
                sdPath = null;
                sdOwner = null;
                sdType = null;
            }
        }
    }
}
