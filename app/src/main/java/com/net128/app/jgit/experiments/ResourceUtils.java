package com.net128.app.jgit.experiments;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ResourceUtils {

	public static boolean deleteDirectory(File directory) {
		File[] allContents = directory.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directory.delete();
	}

	public static void copyResources(String source, File destination) throws IOException, URISyntaxException {
		URL originUrl = ResourceUtils.class.getResource(source);
		URLConnection urlConnection = Objects.requireNonNull(originUrl).openConnection();
		if (urlConnection instanceof JarURLConnection) {
			copyFromJar(source, destination.toPath());
		} else if ("file".equals(originUrl.getProtocol())) {
			copyFolder(new File(originUrl.getPath()).toPath(), destination.toPath());
		} else {
			throw new IOException("URLConnection[" + urlConnection.getClass().getSimpleName() +
					"] is not a recognized/implemented connection type.");
		}

	}

	public static void copyFolder(Path src, Path dest) throws IOException {
		try (Stream<Path> stream = Files.walk(src)) {
			stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
		}
	}

	private static void copy(Path source, Path dest) {
		try {
			if(!(Files.isDirectory(dest) && Files.exists(dest)))
				Files.copy(source, dest, REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static void copyFromJar(String source, final Path target) throws URISyntaxException, IOException {
		URI resource = Objects.requireNonNull(ResourceUtils.class.getResource("")).toURI();
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");
		env.put("encoding", "UTF-8");
		FileSystem fileSystem = FileSystems.newFileSystem(
				resource,
				Collections.<String, String>emptyMap()
		);

		final Path jarPath = fileSystem.getPath(source);

		Files.walkFileTree(jarPath, new SimpleFileVisitor<Path>() {
			private Path currentTarget;

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				currentTarget = target.resolve(jarPath.relativize(dir).toString());
				Files.createDirectories(currentTarget);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, target.resolve(jarPath.relativize(file).toString()), REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
