package ro.binnar.projects.alfa.storage;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

	void init();

	void store(MultipartFile file);

	void store(MultipartFile file, String key);

	Stream<Path> loadAll();

	Stream<Path> load(String key);

	Path loadAsPath(String filename);

	Resource loadAsResource(String filename);

	void deleteAll();

	void delete(String key);
}