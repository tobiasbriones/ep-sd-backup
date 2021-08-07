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

package com.github.tobiasbriones.sdbackup;

import com.github.tobiasbriones.sdbackup.ui.MWController;
import com.github.tobiasbriones.sdbackup.ui.MainWindow;

import javax.swing.*;

/**
 * @author Tobias Briones
 */
public final class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainWindow(new MWController()));
    }
}
