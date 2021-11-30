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

package dev.tobiasbriones.sdbackup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileUtils {
    public static void copyDirectory(File src, File dst) throws IOException {
        copyDirectory(src.toPath(), dst.toPath());
    }

    public static void copyDirectory(Path src, Path dst) throws IOException {
        Files.createDirectories(dst);
        try (Stream<Path> walk = Files.walk(src)) {
            final List<Path> paths = walk.collect(Collectors.toList());

            for (Path path : paths) {
                copyFile(src, path, dst);
            }
        }
    }

    private static void copyFile(Path rootSrc, Path src, Path rootDst) throws IOException {
        final String srcStr = src.toString();
        final String relDst = srcStr.substring(rootSrc.toString().length());
        final Path dst = Paths.get(rootDst.toString(), relDst);
        Files.copy(src, dst);
    }

    private FileUtils() {}
}
