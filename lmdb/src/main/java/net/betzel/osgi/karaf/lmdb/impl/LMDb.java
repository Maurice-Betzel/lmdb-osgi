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

    private static java.lang.reflect.Field LIBRARIES;

    static {
        final String arch = getProperty("os.arch");
        final boolean arch64 = "x64".equals(arch) || "amd64".equals(arch) || "x86_64".equals(arch);
        final String os = getProperty("os.name");
        final boolean linux = os.toLowerCase(ENGLISH).startsWith("linux");
        final boolean osx = os.startsWith("Mac OS X");
        final boolean windows = os.startsWith("Windows");
        if (arch64 && linux) {
            System.loadLibrary("lmdbjava-native-linux-x86_64");
        } else if (arch64 && osx) {
            System.loadLibrary("lmdbjava-native-osx-x86_64");
        } else if (arch64 && windows) {
            System.loadLibrary("lmdbjava-native-windows-x86_64");
        } else {
            throw new UnsupportedOperationException("Unsupported platform or architecture");
        }
        System.out.println("LMDB binary loaded!");
    }

    private final Path dataPath;
    private File filePath;
    private String databaseFile;
    private int databaseSize;
    private Env<ByteBuffer> env;
    private Dbi<ByteBuffer> dbi;

    public LMDb() throws IOException, IllegalAccessException {
        this.dataPath = Paths.get(System.getenv("KARAF_DATA"), "store");
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