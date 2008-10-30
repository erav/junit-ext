package com.googlecode.junit.ext;

public class OSChecker implements Checker {
    public static final String MAC = "mac";
    public static final String LINUX = "linux";
    public static final String WINDOWS = "win";

    private final String targetOS;

    public OSChecker(String targetOS) {
        this.targetOS = targetOS;
    }

    public boolean isExist() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains(targetOS);
    }
}