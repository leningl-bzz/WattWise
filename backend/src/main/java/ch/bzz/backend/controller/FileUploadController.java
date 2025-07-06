package ch.bzz.backend.controller;

import ch.bzz.backend.model.MeterModel;
import ch.bzz.backend.parser.ESLParser;
import ch.bzz.backend.parser.MeasurementMerger;
import ch.bzz.backend.parser.SDATParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:4200")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private static final String UPLOAD_DIR_BASE = "uploads" + File.separator;
    private static final String ESL_UPLOAD_DIR = UPLOAD_DIR_BASE + "esl-files" + File.separator;
    private static final String SDAT_UPLOAD_DIR = UPLOAD_DIR_BASE + "sdat-files" + File.separator;

    // Static initializer to create directories if they don't exist
    static {
        try {
            Files.createDirectories(Paths.get(ESL_UPLOAD_DIR));
            Files.createDirectories(Paths.get(SDAT_UPLOAD_DIR));
        } catch (IOException e) {
            logger.error("Failed to create upload directories: {}", e.getMessage());
        }
    }

    // This method will now be used by both upload and loadExistingFiles
    private MeterModel processFilesInternal(List<File> eslFiles, List<File> sdatFiles) throws Exception {
        MeterModel meterModel = new MeterModel();
        Map<String, Double> combinedEslMap = new HashMap<>();

        if (eslFiles != null && !eslFiles.isEmpty()) {
            for (File eslFile : eslFiles) {
                logger.info("Processing existing ESL file: {}", eslFile.getName());
                Map<String, Double> eslValues = ESLParser.parseESLFile(eslFile);
                combinedEslMap.putAll(eslValues); // Combine all ESL values
            }
            logger.info("Successfully parsed {} ESL files.", eslFiles.size());
        } else {
            logger.warn("No ESL files provided for processing.");
        }

        if (sdatFiles != null && !sdatFiles.isEmpty()) {
            for (File sdatFile : sdatFiles) {
                logger.info("Processing existing SDAT file: {}", sdatFile.getName());
                SDATParser.ParsedSDAT parsedSDAT = SDATParser.parseSDATFile(sdatFile);

                String documentId = parsedSDAT.getDocumentId();
                if (documentId == null || documentId.trim().isEmpty()) {
                    // Assign a unique ID if missing
                    documentId = "unknown_sensor_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                    logger.warn("SDAT file {} has no DocumentID, assigning to '{}'", sdatFile.getName(), documentId);
                }

                String obis1, obis2;
                if (documentId.contains("ID742")) {
                    obis1 = "1-1:1.8.1";
                    obis2 = "1-1:1.8.2";
                } else if (documentId.contains("ID735")) {
                    obis1 = "1-1:2.8.1";
                    obis2 = "1-1:2.8.2";
                } else {
                    logger.warn("Unknown DocumentID pattern '{}' for SDAT file {}. Using fallback OBIS codes.", documentId, sdatFile.getName());
                    obis1 = "1-0:1.8.0";
                    obis2 = "1-0:2.8.0";
                }

                List<ch.bzz.backend.model.Measurement> mergedMeasurements = MeasurementMerger.mergeWithESL(parsedSDAT.getValues(), combinedEslMap, obis1, obis2);
                logger.info("SDAT file '{}' resulted in {} merged measurements.", sdatFile.getName(), mergedMeasurements.size());

                for (ch.bzz.backend.model.Measurement m : mergedMeasurements) {
                    meterModel.addMeasurement(documentId, m);
                }
            }
            logger.info("Successfully processed {} SDAT files.", sdatFiles.size());
            logger.info("Total measurements in meterModel after processing SDAT file(s): {}", meterModel.getAllMeterData().stream().mapToInt(md -> md.getMeasurements().size()).sum());
        } else {
            logger.warn("No SDAT files provided for processing.");
        }

        return meterModel;
    }


    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles( // Changed return type to <?>
                                          @RequestParam(value = "sdatFiles", required = false) List<MultipartFile> sdatFiles,
                                          @RequestParam(value = "eslFiles", required = false) List<MultipartFile> eslFiles) {

        if ((sdatFiles == null || sdatFiles.isEmpty()) && (eslFiles == null || eslFiles.isEmpty())) {
            logger.warn("No SDAT or ESL files provided in the request.");
            // Returning a Map<String, String> for error message
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Es wurden keine Dateien zum Hochladen bereitgestellt."));
        }

        // Convert MultipartFile to File and save them
        List<File> savedEslFiles = new ArrayList<>();
        if (eslFiles != null) {
            for (MultipartFile file : eslFiles) {
                try {
                    Path filePath = Paths.get(ESL_UPLOAD_DIR, System.currentTimeMillis() + "_" + file.getOriginalFilename());
                    file.transferTo(filePath);
                    savedEslFiles.add(filePath.toFile());
                    logger.info("Saved ESL file to: {}", filePath);
                } catch (IOException e) {
                    logger.error("Failed to save ESL file {}: {}", file.getOriginalFilename(), e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Fehler beim Speichern der ESL-Dateien."));
                }
            }
        }

        List<File> savedSdatFiles = new ArrayList<>();
        if (sdatFiles != null) {
            for (MultipartFile file : sdatFiles) {
                try {
                    Path filePath = Paths.get(SDAT_UPLOAD_DIR, System.currentTimeMillis() + "_" + file.getOriginalFilename());
                    file.transferTo(filePath);
                    savedSdatFiles.add(filePath.toFile());
                    logger.info("Saved SDAT file to: {}", filePath);
                } catch (IOException e) {
                    logger.error("Failed to save SDAT file {}: {}", file.getOriginalFilename(), e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Fehler beim Speichern der SDAT-Dateien."));
                }
            }
        }

        try {
            MeterModel meterModel = processFilesInternal(savedEslFiles, savedSdatFiles);

            if (meterModel.getAllMeterData().isEmpty()) {
                logger.warn("No meter data was processed from the provided files. Check file content.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Es konnten keine gültigen Messdaten aus den bereitgestellten Dateien extrahiert werden."));
            }

            return ResponseEntity.ok(meterModel);

        } catch (Exception e) {
            logger.error("Error during file upload or processing: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Ein unerwarteter Fehler ist bei der Dateiverarbeitung aufgetreten."));
        }
    }


    @GetMapping("/load-existing")
    public ResponseEntity<?> loadExistingFiles() { // Changed return type to <?>
        logger.info("Attempting to load existing files from upload directories.");
        List<File> existingEslFiles = new ArrayList<>();
        List<File> existingSdatFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(ESL_UPLOAD_DIR))) {
            existingEslFiles = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            logger.info("Found {} existing ESL files.", existingEslFiles.size());
        } catch (IOException e) {
            logger.error("Error scanning ESL upload directory: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Fehler beim Laden der bestehenden ESL-Dateien."));
        }

        try (Stream<Path> paths = Files.walk(Paths.get(SDAT_UPLOAD_DIR))) {
            existingSdatFiles = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            logger.info("Found {} existing SDAT files.", existingSdatFiles.size());
        } catch (IOException e) {
            logger.error("Error scanning SDAT upload directory: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Fehler beim Laden der bestehenden SDAT-Dateien."));
        }

        try {
            MeterModel meterModel = processFilesInternal(existingEslFiles, existingSdatFiles);

            if (meterModel.getAllMeterData().isEmpty()) {
                logger.warn("No meter data found in existing files.");
                // Return an empty MeterModel or a success message if no files are found.
                // It's not an error condition if the directory is empty.
                return ResponseEntity.ok(meterModel);
            }

            logger.info("Successfully loaded and processed existing files. Total measurements: {}",
                    meterModel.getAllMeterData().stream().mapToInt(md -> md.getMeasurements().size()).sum());
            return ResponseEntity.ok(meterModel);

        } catch (Exception e) {
            logger.error("Error processing existing files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Ein unerwarteter Fehler ist bei der Verarbeitung bestehender Dateien aufgetreten."));
        }
    }
}