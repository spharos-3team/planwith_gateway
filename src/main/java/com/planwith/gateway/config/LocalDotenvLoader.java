package com.planwith.gateway.config;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

/**
 * 로컬 실행 시 모듈 루트 {@code .env}를 읽어 Spring {@code ${ENV}} 플레이스홀더에 넣는다.
 * OS/IntelliJ에 이미 있는 값은 덮어쓰지 않는다. 테스트는 {@code main}을 타지 않아 영향 없다.
 */
public final class LocalDotenvLoader {

	private LocalDotenvLoader() {
	}

	public static void load(String moduleDirectoryName) {
		load(moduleDirectoryName, Path.of("").toAbsolutePath().normalize());
	}

	static Path load(String moduleDirectoryName, Path workingDirectory) {
		if (moduleDirectoryName == null || moduleDirectoryName.isBlank()) {
			throw new IllegalArgumentException("moduleDirectoryName is required");
		}

		Path envFile = resolveEnvFile(moduleDirectoryName, workingDirectory);
		if (envFile == null) {
			return null;
		}

		Dotenv dotenv = Dotenv.configure()
				.directory(envFile.getParent().toString())
				.filename(envFile.getFileName().toString())
				.ignoreIfMalformed()
				.ignoreIfMissing()
				.load();

		for (DotenvEntry entry : dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (key == null || key.isBlank() || value == null || value.isBlank()) {
				continue;
			}
			if (hasValue(System.getenv(key)) || hasValue(System.getProperty(key))) {
				continue;
			}
			System.setProperty(key, value);
		}

		System.out.println("[planwith] loaded " + envFile.toAbsolutePath().normalize());
		return envFile;
	}

	private static Path resolveEnvFile(String moduleDirectoryName, Path workingDirectory) {
		Path cwd = workingDirectory.toAbsolutePath().normalize();
		Path[] candidates = {
				cwd.resolve(".env"),
				cwd.resolve(moduleDirectoryName).resolve(".env")
		};
		for (Path candidate : candidates) {
			if (Files.isRegularFile(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean hasValue(String value) {
		return value != null && !value.isBlank();
	}
}
