package dev.astra.module;

import dev.astra.event.EventHandler;
import dev.astra.event.events.KeyEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        registerModules();
    }

    private void registerModules() {
        String packageName = "dev.astra.module.impl";
        String packagePath = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    scanDirectory(new File(URLDecoder.decode(resource.getFile(), "UTF-8")), packageName);
                } else if (resource.getProtocol().equals("jar")) {
                    String jarPath = URLDecoder.decode(resource.getPath().substring(5, resource.getPath().indexOf("!")), "UTF-8");
                    scanJar(jarPath, packagePath, packageName);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void scanDirectory(File directory, String packageName) throws ClassNotFoundException {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                loadClass(className);
            }
        }
    }

    private void scanJar(String jarPath, String packagePath, String packageName) {
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    loadClass(className);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (Module.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isInterface()) {
                Module module = (Module) clazz.getDeclaredConstructor().newInstance();
                registerModule(module);
            }
        } catch (Throwable ignored) {
        }
    }

    public void registerModule(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public <T extends Module> T getModuleByClass(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return clazz.cast(module);
            }
        }
        return null;
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        for (Module module : modules) {
            if (module.getKeycode() == event.getKey()) {
                module.toggle();
            }
        }
    }
}