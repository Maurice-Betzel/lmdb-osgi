/*
Copyright 2017 Maurice Betzel

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package net.betzel.osgi.karaf.lmdb.impl;

import org.lmdbjava.Dbi;
import org.lmdbjava.Env;
import org.lmdbjava.EnvInfo;
import org.lmdbjava.Txn;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.System.getProperty;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static org.lmdbjava.DbiFlags.MDB_CREATE;
import static org.lmdbjava.Env.create;

/**
 * Created by mbetzel on 16.03.2017.
 */
public class LMDb {

    public static final String DISABLE_EXTRACT_PROP = "lmdbjava.disable.extract";
    public static final String OSGI_NATIVE_LMDB_PROP = "osgi.native.lmdb";

    static boolean linux;
    static boolean osx;
    static boolean windows;

    static {
        final String arch = getProperty("os.arch");
        final boolean arch64 = "x64".equals(arch) || "amd64".equals(arch) || "x86_64".equals(arch);
        final String os = getProperty("os.name");
        linux = os.toLowerCase(ENGLISH).startsWith("linux");
        osx = os.startsWith("Mac OS X");
        windows = os.startsWith("Windows");
        if (arch64 && linux) {
            System.loadLibrary("lmdbjava-native-linux-x86_64.so");
        } else if (arch64 && osx) {
            System.loadLibrary("lmdbjava-native-osx-x86_64.dylib");
        } else if (arch64 && windows) {
            System.loadLibrary("lmdbjava-native-windows-x86_64");
        } else {
            throw new UnsupportedOperationException("Unsupported platform or architecture");
        }
        System.out.println("LMDB binary loaded!");
        Bundle bundle = org.osgi.framework.FrameworkUtil.getBundle(LMDb.class);
        String bundleId = String.valueOf(bundle.getBundleId());
        BundleContext bundleContext = bundle.getBundleContext();
        String cachePath = bundleContext.getProperty("org.osgi.framework.storage");
        // OSGi does not provide a bundle cache strategy
        // Assuming native library gets unpacked within folder having this bundle ID
        File bundlePath = locateBundlePath(cachePath, bundleId);
        if (bundlePath == null) {
            System.out.println("Classpath resource not found");
        }
        File nativeFile = locateNativeFile(bundlePath, "lmdbjava-native-");
        if (nativeFile == null) {
            System.out.println("Classpath resource not found");
        }
        System.setProperty(DISABLE_EXTRACT_PROP, "true");
        System.setProperty(OSGI_NATIVE_LMDB_PROP, nativeFile.getAbsolutePath());
    }

    private BundleContext bundleContext;
    private final Path dataPath;
    private File filePath;
    private String databaseFile;
    private int databaseSize;
    private Env<ByteBuffer> env;
    private Dbi<ByteBuffer> dbi;

    public LMDb(BundleContext bundleContext) throws IOException, IllegalAccessException {
        this.bundleContext = bundleContext;
        this.dataPath = Paths.get(bundleContext.getProperty("KARAF.DATA"), "store");
    }

    public void init() throws IOException {
        System.out.println("Init!");
        if (Files.exists(dataPath)) {
            filePath = dataPath.toFile();
        } else {
            filePath = Files.createDirectory(dataPath).toFile();
        }
        if (filePath.isDirectory()) {
            env = create()
                    .setMapSize(databaseSize)
                    .setMaxDbs(1)
                    .open(filePath);
            EnvInfo infos = env.info();
            dbi = env.openDbi(databaseFile, MDB_CREATE);
            ByteBuffer key = allocateDirect(env.getMaxKeySize());
            ByteBuffer val = allocateDirect(700);
            key.put("greeting".getBytes(UTF_8)).flip();
            val.put("Hello world".getBytes(UTF_8)).flip();
            int valSize = val.remaining();
            System.out.println("putting key / value: [greeting] / [Hello World]");
            dbi.put(key, val);
            System.out.println("getting key");
            try (Txn<ByteBuffer> txn = env.txnRead()) {
                ByteBuffer found = dbi.get(txn, key);
                ByteBuffer fetchedVal = txn.val();
                System.out.println("Value for key: " + UTF_8.decode(fetchedVal).toString());
            }
            System.out.println("deleting key");
            dbi.delete(key);
        } else {
            throw new IllegalArgumentException("Database path is not a directory!");
        }
    }

    private static File locateBundlePath(final String path, final String bundleId) {
        File root = new File(path);
        File[] files = root.listFiles();
        for (File file : files) {
            if (file.isDirectory() && file.getName().contains(bundleId)) {
                return file;
            }
        }
        return null;
    }

    private static File locateNativeFile(File rootFile, String search) {
        if (rootFile.isDirectory()) {
            File[] files = rootFile.listFiles();
            for (File file : files) {
                File found = locateNativeFile(file, search);
                if (found != null)
                    return found;
            }
        } else {
            if (rootFile.getName().toLowerCase().startsWith(search)) {
                return rootFile;
            }
        }
        return null;
    }


    public void destroy() {
        dbi.close();
        env.close();
    }

    public void setDatabaseFile(String databaseFile) {
        this.databaseFile = databaseFile;
    }

    public void setDatabaseSize(int databaseSize) {
        this.databaseSize = databaseSize;
    }

}