package ro.binnar.projects.alfa.fileupload;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ro.binnar.projects.alfa.jobs.JobLauncherController;
import ro.binnar.projects.alfa.storage.StorageService;
import ro.binnar.projects.alfa.storage.exception.StorageFileNotFoundException;

@Controller
public class FileUploadController {

	@Autowired
	private StorageService storageService;

	@GetMapping("/")
	public String listUploadedFiles(Model model) {
		List<String> files = storageService.loadAll()
				.map(path -> path.toString().replace("\\", "/"))
				.collect(Collectors.toList());
		
		Map<String, String> fileLinks = files.stream()
				.collect(Collectors.toMap(
						filename -> filename, 
						filename -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", filename).build().toString()
				));
	
		Map<String, String> fileJobLinks = files.stream()
				.collect(Collectors.toMap(
						filename -> filename, 
						filename -> MvcUriComponentsBuilder.fromMethodName(JobLauncherController.class, "launchJob", storageService.loadAsPath(filename).toAbsolutePath().toUri().toString()).build().toString()
				));
		
		model.addAttribute("files", files);
		model.addAttribute("fileLinks", fileLinks);
		model.addAttribute("fileJobLinks", fileJobLinks);

		return "uploadForm";
	}

	@GetMapping("/files")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@RequestParam("filename") String filename) {
		Resource file = storageService.loadAsResource(filename);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
		storageService.store(file, "TEST");
		
		redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");

		return "redirect:/";
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<Void> handleStorageFileNotFound(StorageFileNotFoundException e) {
		return ResponseEntity.notFound().build();
	}
}
