package ch.bzz.backend.controller;

import ch.bzz.backend.model.Measurement;
import ch.bzz.backend.parser.ESLParser;
import ch.bzz.backend.parser.MeasurementMerger;
import ch.bzz.backend.parser.SDATParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:63342"})
public class FileUploadController {

    private static final String BASE_PATH = System.getProperty("user.dir") + "/uploads/";

    @PostMapping("/upload")
    public ResponseEntity<List<Measurement>> uploadFiles(
            @RequestParam(value = "sdatFiles", required = false) List<MultipartFile> sdatFiles,
            @RequestParam(value = "eslFiles", required = false) List<MultipartFile> eslFiles) {

        System.out.println("Received upload request");

        if (sdatFiles == null) {
            System.out.println("sdatFiles is null");
        } else {
            System.out.println("sdatFiles size: " + sdatFiles.size());
            for (MultipartFile f : sdatFiles) {
                System.out.println("  - " + f.getOriginalFilename());
            }
        }

        try {
            SDATParser.ParsedSDAT parsedSDAT = null;
            Map<String, Double> eslMap = null;

            // Handle SDAT files
            if (sdatFiles != null && !sdatFiles.isEmpty()) {
                for (MultipartFile sdatFile : sdatFiles) {
                    File savedFile = saveToFile(sdatFile, "sdat-files");
                    // Just parse the first valid one
                    if (parsedSDAT == null) {
                        parsedSDAT = SDATParser.parseSDATFile(savedFile);
                    }
                }
            }

            // Handle ESL files
            if (eslFiles != null && !eslFiles.isEmpty()) {
                for (MultipartFile eslFile : eslFiles) {
                    File savedFile = saveToFile(eslFile, "esl-files");
                    // Just parse the first valid one
                    if (eslMap == null) {
                        eslMap = ESLParser.parseESLFile(savedFile);
                    }
                }
            }

            List<Measurement> result;

            if (parsedSDAT != null && eslMap != null) {
                result = MeasurementMerger.mergeWithESL(
                        parsedSDAT.getValues(),
                        eslMap,
                        "1-0:1.8.0",
                        "1-0:2.8.0"
                );
            } else if (parsedSDAT != null) {
                result = parsedSDAT.getValues();
            } else {
                return ResponseEntity.badRequest().body(null);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }


    private File saveToFile(MultipartFile multipartFile, String subFolder) throws Exception {
        Path dirPath = Paths.get(BASE_PATH, subFolder);
        Files.createDirectories(dirPath);

        String filename = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
        Path filePath = dirPath.resolve(filename);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(multipartFile.getBytes());
        }

        System.out.println("Saved file to: " + filePath.toAbsolutePath()); // debug
        return filePath.toFile();
    }
}
