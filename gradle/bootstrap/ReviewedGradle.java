// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** JDK-only bootstrap for the maintainer-approved, locally composed toolchain. */
public class ReviewedGradle {
    static final String VERSION = "9.9.1";
    static final String GUAVA = "33.8.0-jre";
    static final String ALIAS = "gradle-" + VERSION + "/lib/guava-33.4.6-jre.jar";
    static final long MAX_DOWNLOAD = 256L * 1024 * 1024;
    static final long MAX_EXPANDED = 1024L * 1024 * 1024;
    static final int MAX_ENTRIES = 10000;

    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() != 21) throw new IOException("Use the reviewed JDK 21");
        if (args.length == 0) throw new IOException("Run the checked-in gradlew launcher");
        Path root = Path.of(args[0]).toRealPath();
        String[] forwarded = Arrays.copyOfRange(args, 1, args.length);
        boolean acquire = Arrays.asList(forwarded).contains("--sharpener-acquire-toolchain");
        if (acquire && forwarded.length != 1)
            throw new IOException("Toolchain acquisition is a separate command; do not combine it with build/offline arguments");
        if (Arrays.asList(forwarded).contains("--daemon"))
            throw new IOException("This toolchain requires --no-daemon to avoid reusing an unpatched vendor daemon");
        Properties pin = readProperties(root.resolve("gradle/security-toolchain.properties"));
        Properties wrapper = readProperties(root.resolve("gradle/wrapper/gradle-wrapper.properties"));
        validatePins(pin, wrapper);
        verifyFile(root.resolve("gradle/wrapper/gradle-wrapper.jar"), pin.getProperty("wrapper.sha256"));
        Path cache = privateCache(root);
        Path distribution = cache.resolve("gradle-" + VERSION + "-bin.zip");
        Path guava = cache.resolve("guava-" + GUAVA + ".jar");
        Path pom = cache.resolve("guava-" + GUAVA + ".pom");
        Path installation = cache.resolve("installation");
        try (FileChannel channel = FileChannel.open(cache.resolve("bootstrap.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IOException("Another toolchain preparation is running; retry when it completes");
            acquireInput(distribution, wrapper.getProperty("distributionUrl"), wrapper.getProperty("distributionSha256Sum"), acquire);
            acquireInput(guava, pin.getProperty("guava.jar.url"), pin.getProperty("guava.jar.sha256"), acquire);
            acquireInput(pom, pin.getProperty("guava.pom.url"), pin.getProperty("guava.pom.sha256"), acquire);
            verifyGuava(guava);
            if (!Files.exists(installation, LinkOption.NOFOLLOW_LINKS)) {
                Path staging = Files.createTempDirectory(cache, "staging-");
                try {
                    assemble(distribution, guava, staging, pin.getProperty("guava.original.sha256"));
                    if (!validateInstallation(distribution, guava, staging).equals(pin.getProperty("composition.sha256")))
                        throw new IOException("Assembled toolchain does not match the reviewed composition");
                    Files.move(staging, installation, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    removeOwnedStaging(cache, staging);
                }
            }
            String digest = validateInstallation(distribution, guava, installation);
            if (!digest.equals(pin.getProperty("composition.sha256"))) throw new IOException("Wrong toolchain composition");
            System.out.println("Sharpener toolchain: Gradle " + VERSION + " + Guava " + GUAVA
                    + " (maintainer-approved local composition; not an upstream distribution)");
            System.out.println("Verified composition SHA-256: " + digest);
        }
        if (acquire) return;
        // Delegate to the verified public Gradle launcher, as the vendor wrapper
        // does. The minimized wrapper jar does not expose BootstrapMainStarter.
        // Never reuse a daemon from an unpatched stock Gradle installation.
        String[] gradleArgs = new String[forwarded.length + 1];
        gradleArgs[0] = "--no-daemon";
        System.arraycopy(forwarded, 0, gradleArgs, 1, forwarded.length);
        Path launcher = installation.resolve("gradle-" + VERSION + "/lib/gradle-launcher-" + VERSION + ".jar");
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{launcher.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            loader.loadClass("org.gradle.launcher.GradleMain").getMethod("main", String[].class)
                    .invoke(null, (Object) gradleArgs);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception cause) throw cause;
            throw failure;
        } finally { Thread.currentThread().setContextClassLoader(previous); }
    }

    static Properties readProperties(Path path) throws IOException {
        Properties result = new Properties();
        try (InputStream stream = Files.newInputStream(path)) { result.load(stream); }
        return result;
    }

    static void validatePins(Properties pin, Properties wrapper) throws IOException {
        String base = "https://repo.maven.apache.org/maven2/com/google/guava/guava/" + GUAVA + "/guava-" + GUAVA;
        if (!VERSION.equals(pin.getProperty("gradle.version")) || !GUAVA.equals(pin.getProperty("guava.version"))
                || !("https://services.gradle.org/distributions/gradle-" + VERSION + "-bin.zip").equals(wrapper.getProperty("distributionUrl"))
                || !(base + ".jar").equals(pin.getProperty("guava.jar.url"))
                || !(base + ".pom").equals(pin.getProperty("guava.pom.url")))
            throw new IOException("Toolchain inputs must match the exact approved canonical composition");
        for (String field : List.of("composition.sha256", "wrapper.sha256", "guava.jar.sha256", "guava.pom.sha256", "guava.original.sha256"))
            if (!pin.getProperty(field, "").matches("[0-9a-f]{64}")) throw new IOException("Missing exact SHA-256: " + field);
        if (!wrapper.getProperty("distributionSha256Sum", "").matches("[0-9a-f]{64}"))
            throw new IOException("Missing official distribution SHA-256");
    }

    static Path privateCache(Path root) throws IOException {
        Path cache = root.resolve(".gradle/security-toolchain/gradle-" + VERSION + "-guava-" + GUAVA);
        Path next = root;
        for (Path part : root.relativize(cache)) {
            next = next.resolve(part);
            if (Files.exists(next, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(next) || !Files.isDirectory(next, LinkOption.NOFOLLOW_LINKS)
                        || !next.toRealPath().startsWith(root)) throw new IOException("Unsafe toolchain cache directory");
            } else Files.createDirectory(next);
        }
        return cache;
    }

    static void acquireInput(Path target, String url, String expected, boolean acquire) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) { verifyFile(target, expected); return; }
        if (!acquire) throw new IOException("Verified toolchain input missing: " + target.getFileName()
                + "; first run ./gradlew --sharpener-acquire-toolchain online");
        URI source = URI.create(url);
        for (int redirects = 0; redirects <= 5; redirects++) {
            HttpURLConnection connection = (HttpURLConnection) source.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Missing download redirect");
                    URI next = source.resolve(location);
                    if (!allowedGradleRedirect(source, next)) throw new IOException("Unexpected toolchain download redirect");
                    source = next;
                    continue;
                }
                if (status != 200 || connection.getContentLengthLong() > MAX_DOWNLOAD)
                    throw new IOException("Toolchain download failed or exceeded size limit: HTTP " + status);
                try (InputStream stream = connection.getInputStream()) { copyVerified(stream, target, expected, MAX_DOWNLOAD); }
                return;
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many toolchain download redirects");
    }

    static boolean allowedGradleRedirect(URI from, URI to) {
        if (!"https".equals(to.getScheme()) || to.getUserInfo() != null || (to.getPort() != -1 && to.getPort() != 443)) return false;
        String filename = "/gradle-" + VERSION + "-bin.zip";
        if (from.getHost().equals("services.gradle.org"))
            return (to.getHost().equals("github.com") && to.getPath().equals("/gradle/gradle-distributions/releases/download/v" + VERSION + filename))
                    || (to.getHost().equals("downloads.gradle.org") && to.getPath().equals("/distributions" + filename));
        return from.getHost().equals("github.com") && to.getHost().equals("release-assets.githubusercontent.com");
    }

    static void copyVerified(InputStream stream, Path target, String expected, long limit) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Refusing to overwrite a cached input");
        Path staging = Files.createTempFile(target.getParent(), "download-", ".part");
        try {
            long total = 0;
            try (OutputStream output = Files.newOutputStream(staging)) {
                byte[] block = new byte[65536];
                for (int n; (n = stream.read(block)) != -1;) {
                    if ((total += n) > limit) throw new IOException("Toolchain download exceeds size limit");
                    output.write(block, 0, n);
                }
            }
            verifyFile(staging, expected);
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(staging); }
    }

    static String hash(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[65536];
            for (int n; (n = input.read(block)) != -1;) digest.update(block, 0, n);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    static String hash(Path file) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Expected a regular verified file: " + file.getFileName());
        try (InputStream stream = Files.newInputStream(file)) { return hash(stream); }
    }

    static void verifyFile(Path file, String expected) throws IOException {
        if (!hash(file).equals(expected)) throw new IOException("Toolchain SHA-256 mismatch: " + file.getFileName());
    }

    static void verifyGuava(Path jar) throws IOException {
        try (ZipFile archive = new ZipFile(jar.toFile())) {
            ZipEntry entry = archive.getEntry("META-INF/maven/com.google.guava/guava/pom.properties");
            if (entry == null) throw new IOException("Missing Guava identity");
            Properties metadata = new Properties();
            try (InputStream stream = archive.getInputStream(entry)) { metadata.load(stream); }
            if (!GUAVA.equals(metadata.getProperty("version")) || !"guava".equals(metadata.getProperty("artifactId"))
                    || !"com.google.guava".equals(metadata.getProperty("groupId"))) throw new IOException("Wrong actual Guava identity");
        }
    }

    static List<ZipEntry> entries(ZipFile archive) throws IOException {
        List<ZipEntry> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        long total = 0;
        for (var values = archive.entries(); values.hasMoreElements();) {
            ZipEntry entry = values.nextElement();
            String name = entry.getName();
            String[] parts = name.split("/", -1);
            if (name.contains("\\") || name.contains("\0") || name.contains("\n") || name.contains("\r")
                    || name.contains("//") || parts.length < 1 || !parts[0].equals("gradle-" + VERSION)
                    || Arrays.stream(parts).anyMatch(s -> s.equals(".") || s.equals("..") || s.contains(":"))
                    || !names.add(name.toLowerCase(Locale.ROOT)) || entry.getSize() < 0
                    || (total += entry.getSize()) > MAX_EXPANDED || result.size() >= MAX_ENTRIES)
                throw new IOException("Unsafe, duplicate or excessive Gradle archive entry");
            result.add(entry);
        }
        if (result.isEmpty()) throw new IOException("Empty Gradle archive");
        return result;
    }

    static void assemble(Path distribution, Path replacement, Path staging, String originalHash) throws IOException {
        long expanded = 0;
        try (ZipFile archive = new ZipFile(distribution.toFile())) {
            for (ZipEntry entry : entries(archive)) {
                Path target = staging.resolve(entry.getName()).normalize();
                if (!target.startsWith(staging)) throw new IOException("Archive path escapes staging");
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    long written = 0;
                    try (InputStream input = archive.getInputStream(entry);
                         OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                        byte[] block = new byte[65536];
                        for (int n; (n = input.read(block)) != -1;) {
                            written += n;
                            if ((expanded += n) > MAX_EXPANDED || written > entry.getSize())
                                throw new IOException("Gradle archive expands beyond declared limits");
                            output.write(block, 0, n);
                        }
                    }
                    if (written != entry.getSize()) throw new IOException("Wrong expanded Gradle entry size");
                    if (entry.getName().equals("gradle-" + VERSION + "/bin/gradle"))
                        if (!target.toFile().setExecutable(true, true)) throw new IOException("Cannot set Gradle executable permission");
                }
            }
        }
        Path alias = staging.resolve(ALIAS);
        verifyFile(alias, originalHash);
        Files.copy(replacement, alias, StandardCopyOption.REPLACE_EXISTING);
    }

    static String validateInstallation(Path distribution, Path replacement, Path installation) throws IOException {
        if (Files.isSymbolicLink(installation) || !Files.isDirectory(installation, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Unsafe toolchain installation");
        SortedMap<String, String> manifest = new TreeMap<>();
        try (ZipFile archive = new ZipFile(distribution.toFile())) {
            for (ZipEntry entry : entries(archive)) {
                if (entry.isDirectory()) continue;
                String expected;
                try (InputStream input = archive.getInputStream(entry)) { expected = hash(input); }
                if (entry.getName().equals(ALIAS)) expected = hash(replacement);
                Path target = installation.resolve(entry.getName());
                verifyFile(target, expected);
                manifest.put(entry.getName(), expected);
            }
        }
        try (var tree = Files.walk(installation)) {
            for (Path file : tree.toList()) {
                if (Files.isSymbolicLink(file) || (!Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)
                        && !manifest.containsKey(installation.relativize(file).toString().replace(File.separatorChar, '/'))))
                    throw new IOException("Unexpected file/link in verified Gradle installation");
                if (!file.toRealPath().startsWith(installation.toRealPath())) throw new IOException("Installation path escapes cache");
            }
        }
        StringBuilder identity = new StringBuilder();
        manifest.forEach((name, sha) -> identity.append(name).append('\0').append(sha).append('\n'));
        return hash(new ByteArrayInputStream(identity.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    static void removeOwnedStaging(Path cache, Path staging) throws IOException {
        if (!Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) return;
        if (!staging.toRealPath().startsWith(cache.toRealPath()) || Files.isSymbolicLink(staging))
            throw new IOException("Unsafe staging cleanup");
        try (var tree = Files.walk(staging)) {
            for (Path file : tree.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
        }
    }
}
