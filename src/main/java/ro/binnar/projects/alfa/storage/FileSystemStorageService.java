package ro.binnar.projects.alfa.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import ro.binnar.projects.alfa.storage.exception.StorageException;
import ro.binnar.projects.alfa.storage.exception.StorageFileNotFoundException;

@Service
public class FileSystemStorageService implements StorageService {

	private final Path rootLocation;

	@Autowired
	public FileSystemStorageService(StorageProperties properties) {
		this.rootLocation = Paths.get(properties.getLocation());
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(this.rootLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}

	@Override
	public void store(MultipartFile file) {
		store(file, null);
	}

	@Override
	public void store(MultipartFile file, String key) {
		key = (key == null) ? "" : key;

		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file: " + file.getOriginalFilename());
			}

			Path fileParentDirectory = this.rootLocation.resolve(key);

			Files.createDirectories(fileParentDirectory);
			Files.copy(file.getInputStream(), fileParentDirectory.resolve(file.getOriginalFilename()));
		} catch (IOException e) {
			throw new StorageException("Failed to store file: " + file.getOriginalFilename(), e);
		}
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootLocation, Integer.MAX_VALUE)
					.filter(path -> !path.equals(this.rootLocation))
					.filter(path -> !path.toFile().isDirectory())
					.map(path -> this.rootLocation.relativize(path));
		} catch (IOException e) {
			throw new StorageException("Failed to read stored files", e);
		}
	}

	@Override
	public Stream<Path> load(String key) {
		if(!this.rootLocation.resolve(key).toFile().exists()) {
			return Stream.empty();
		}
		
		try {
			return Files.walk(this.rootLocation.resolve(key), 1)
					.filter(path -> !path.toFile().isDirectory())
					.map(path -> this.rootLocation.relativize(path));
		} catch (IOException e) {
			throw new StorageException("Failed to read stored files", e);
		}
	}

	@Override
	public Path loadAsPath(String filename) {
		return rootLocation.resolve(filename);
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path file = loadAsPath(filename);
			Resource resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new StorageFileNotFoundException("Could not read file: " + filename);
			}
		} catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(this.rootLocation.toFile());
	}
	
	@Override
	public void delete(String key) {
		if(!this.rootLocation.resolve(key).toFile().exists()) {
			return;
		}
		
		try {
			Files.walk(this.rootLocation.resolve(key), 1)
				.filter(path -> !path.toFile().isDirectory())
				.forEach(path -> path.toFile().delete());
		} catch (IOException e) {
			throw new StorageException("Failed to delete files", e);
		}
	}
}
