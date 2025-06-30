package ch.bzz.backend.controller;

import ch.bzz.backend.model.Measurement;
import ch.bzz.backend.parser.ESLParser;
import ch.bzz.backend.parser.MeasurementMerger;
import ch.bzz.backend.parser.SDATParser;
import ch.bzz.backend.util.CSVExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    @PostMapping("/upload")
    public ResponseEntity<List<Measurement>> uploadFiles(
            @RequestParam(value = "sdatFiles", required = false) List<MultipartFile> sdatFiles,
            @RequestParam(value = "eslFiles", required = false) List<MultipartFile> eslFiles) {

        try {
            SDATParser.ParsedSDAT parsedSDAT = null;
            Map<String, Double> eslMap = null;

            if (sdatFiles != null && !sdatFiles.isEmpty()) {
                for (MultipartFile sdatFile : sdatFiles) {
                    File savedFile = saveToFile(sdatFile, "sdat-files");
                    if (parsedSDAT == null) {
                        parsedSDAT = SDATParser.parseSDATFile(savedFile);
                    }
                }
            }

            if (eslFiles != null && !eslFiles.isEmpty()) {
                for (MultipartFile eslFile : eslFiles) {
                    File savedFile = saveToFile(eslFile, "esl-files");
                    if (eslMap == null) {
                        eslMap = ESLParser.parseESLFile(savedFile);
                    }
                }
            }

            if (parsedSDAT == null) return ResponseEntity.badRequest().build();

            String docId = parsedSDAT.getDocumentId();
            String obis1, obis2;

            if (docId != null && docId.contains("ID742")) {
                obis1 = "1-1:1.8.1";
                obis2 = "1-1:1.8.2";
            } else if (docId != null && docId.contains("ID735")) {
                obis1 = "1-1:2.8.1";
                obis2 = "1-1:2.8.2";
            } else {
                obis1 = "1-0:1.8.0";
                obis2 = "1-0:2.8.0";
            }

            List<Measurement> result;
            if (eslMap != null) {
                result = MeasurementMerger.mergeWithESL(parsedSDAT.getValues(), eslMap, obis1, obis2);
            } else {
                result = parsedSDAT.getValues();
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error while processing upload", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/exportCsv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(value = "sdatFiles", required = false) List<MultipartFile> sdatFiles,
            @RequestParam(value = "eslFiles", required = false) List<MultipartFile> eslFiles) {

        ResponseEntity<List<Measurement>> response = uploadFiles(sdatFiles, eslFiles);
        List<Measurement> list = response.getBody();
        if (list == null) {
            return ResponseEntity.badRequest().build();
        }
        String csv = CSVExporter.toCSV(list);
        return ResponseEntity.ok(csv);
    }

    private File saveToFile(MultipartFile multipartFile, String subFolder) throws Exception {
        Path dirPath = Paths.get(BASE_PATH, subFolder);
        Files.createDirectories(dirPath);

        String filename = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
        Path filePath = dirPath.resolve(filename);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(multipartFile.getBytes());
        }

        log.debug("Saved file to: {}", filePath.toAbsolutePath());
        return filePath.toFile();
    }
}
