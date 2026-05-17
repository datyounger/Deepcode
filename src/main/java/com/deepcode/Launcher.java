package com.deepcode;

/**
 * Launcher class to work around JavaFX module system requirements
 * when creating a fat JAR with maven-shade-plugin.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
