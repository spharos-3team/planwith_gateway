package com.planwith.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDotenvLoaderTest {

	private static final String KEY = "PLANWITH_DOTENV_LOADER_TEST";

	@AfterEach
	void clearProperty() {
		System.clearProperty(KEY);
	}

	@Test
	void loadsDotenvFromWorkingDirectory(@TempDir Path tempDir) throws Exception {
		Files.writeString(tempDir.resolve(".env"), KEY + "=from-env\n");

		Path loaded = LocalDotenvLoader.load("planwith_gateway", tempDir);

		assertThat(loaded).isEqualTo(tempDir.resolve(".env"));
		assertThat(System.getProperty(KEY)).isEqualTo("from-env");
	}

	@Test
	void returnsNullWhenDotenvMissing(@TempDir Path tempDir) {
		assertThat(LocalDotenvLoader.load("planwith_gateway", tempDir)).isNull();
	}
}
