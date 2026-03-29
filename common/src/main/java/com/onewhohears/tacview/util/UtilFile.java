package com.onewhohears.tacview.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.architectury.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtilFile {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static CompoundTag readNbtInGamePath(String path) {
        Path gamePath = getGameFolder();
        Path resolved = gamePath.resolve(path);
        CompoundTag nbt;
        try {
            nbt = NbtIo.readCompressed(resolved.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            return new CompoundTag();
        }
        return nbt;
    }

    public static void writeNbtInGamePath(String path, CompoundTag nbt) {
        Path gamePath = getGameFolder();
        Path resolved = gamePath.resolve(path);
        File file = resolved.toFile();
        Path p = Path.of(path).getParent().normalize();
        p.toFile().mkdirs();
        try {
            NbtIo.writeCompressed(nbt, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printGamePath(String path, JsonObject json) {
        Path gamePath = getGameFolder();
        Path resolved = gamePath.resolve(path);
        printJsonAbsolutePath(resolved.toString(), json);
    }

    public static void printJsonAbsolutePath(String path, JsonObject json) {
        Path p = Path.of(path).getParent().normalize();
        new File(p.toUri()).mkdirs();
        try {
            Writer writer = new FileWriter(path);
            GSON.toJson(json, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonObject readJsonGamePath(String path) {
        Path gamePath = getGameFolder();
        Path resolved = gamePath.resolve(path);
        return readJsonAbsolutePath(resolved.toString());
    }

    public static JsonObject readJsonAbsolutePath(String path) {
        Path p = Path.of(path);
        try (Reader reader = Files.newBufferedReader(p)) {
            return GSON.fromJson(reader, JsonObject.class).getAsJsonObject();
        } catch (IOException e) {
            return new JsonObject();
        }
    }

    public static boolean doesFileExistGamePath(String path) {
        Path gamePath = getGameFolder();
        Path resolved = gamePath.resolve(path);
        return doesFileExistAbsolutePath(resolved.toString());
    }

    public static boolean doesFileExistAbsolutePath(String path) {
        return Files.exists(Path.of(path));
    }

    public static Set<String> getJsonFileNamesInGamePath(String path) {
        Path gamePath = getGameFolder();
        Path resolved = gamePath.resolve(path);
        return getJsonFileNamesInAbsolutePath(resolved.toString());
    }

    public static Set<String> getJsonFileNamesInAbsolutePath(String path) {
        return getFileNamesEndingWithInAbsolutePath(path, ".json");
    }

    public static Set<String> getFileNamesEndingWithInGamePath(String path, String suffix) {
        Path gamePath = getGameFolder();
        Path resolved = gamePath.resolve(path);
        return getFileNamesEndingWithInAbsolutePath(resolved.toString(), suffix);
    }

    public static Set<String> getFileNamesEndingWithInAbsolutePath(String path, String suffix) {
        Path dir = Paths.get(path);
        if (!dir.isAbsolute() || !Files.isDirectory(dir)) {
            return Set.of();
        }
        int suffixLength = suffix.length();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.toLowerCase().endsWith(suffix))
                    .map(name -> name.substring(0, name.length() - suffixLength))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return Set.of();
        }
    }

    public static Path getGameFolder() {
        return Platform.getGameFolder();
    }

}
