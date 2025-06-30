package ch.bzz.backend.controller;

import ch.bzz.backend.model.Measurement;
import ch.bzz.backend.model.MeterModel;
import ch.bzz.backend.parser.ESLParser;
import ch.bzz.backend.parser.MeasurementMerger;
import ch.bzz.backend.parser.SDATParser;
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

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    private static final String BASE_PATH = System.getProperty("user.dir") + "/uploads/";

    @PostMapping("/upload")
    public ResponseEntity<MeterModel> uploadFiles(
            @RequestParam(value = "sdatFiles", required = false) List<MultipartFile> sdatFiles,
            @RequestParam(value = "eslFiles", required = false) List<MultipartFile> eslFiles) {

        try {
            if ((sdatFiles == null || sdatFiles.isEmpty()) && (eslFiles == null || eslFiles.isEmpty())) {
                logger.warn("No SDAT or ESL files provided.");
                return ResponseEntity.badRequest().body(null); // Or a specific error message object
            }

            // Parse ESL files first and merge into a combined map
            Map<String, Double> combinedEslMap = new java.util.HashMap<>();
            if (eslFiles != null) {
                for (MultipartFile eslFile : eslFiles) {
                    File savedFile = saveToFile(eslFile, "esl-files");
                    Map<String, Double> parsedEsl = ESLParser.parseESLFile(savedFile);
                    combinedEslMap.putAll(parsedEsl);
                }
                logger.info("Successfully parsed {} ESL files.", eslFiles.size());
            }

            MeterModel meterModel = new MeterModel();

            if (sdatFiles != null) {
                for (MultipartFile sdatFile : sdatFiles) {
                    File savedFile = saveToFile(sdatFile, "sdat-files");
                    SDATParser.ParsedSDAT parsedSDAT = SDATParser.parseSDATFile(savedFile);
                    logger.info("SDAT file '{}' parsed with {} raw measurements.", sdatFile.getOriginalFilename(), parsedSDAT.getValues().size());

                    String documentId = parsedSDAT.getDocumentId();
                    if (documentId == null || documentId.trim().isEmpty()) {
                        documentId = "unknown_sensor"; // Provide a fallback ID
                        logger.warn("SDAT file {} has no DocumentID, assigning to '{}'", sdatFile.getOriginalFilename(), documentId);
                    }

                    // Determine OBIS keys based on DocumentID pattern
                    String obis1, obis2;
                    if (documentId.contains("ID742")) {
                        obis1 = "1-1:1.8.1";
                        obis2 = "1-1:1.8.2";
                    } else if (documentId.contains("ID735")) {
                        obis1 = "1-1:2.8.1";
                        obis2 = "1-1:2.8.2";
                    } else {
                        // Fallback OBIS codes or error if unexpected ID
                        logger.warn("Unknown DocumentID pattern '{}' for SDAT file {}. Using fallback OBIS codes.", documentId, sdatFile.getOriginalFilename());
                        obis1 = "1-0:1.8.0"; // fallback example, adjust as per your data
                        obis2 = "1-0:2.8.0"; // fallback example, adjust as per your data
                    }

                    List<Measurement> mergedMeasurements = MeasurementMerger.mergeWithESL(parsedSDAT.getValues(), combinedEslMap, obis1, obis2);
                    logger.info("SDAT file '{}' resulted in {} merged measurements.", sdatFile.getOriginalFilename(), mergedMeasurements.size());

                    // CORRECTED LOGIC: Add merged measurements directly to MeterModel,
                    // which handles adding to the correct MeterData object or creating a new one.
                    for (Measurement m : mergedMeasurements) {
                        meterModel.addMeasurement(documentId, m);
                    }
                    logger.info("Total measurements in meterModel after processing SDAT file(s): {}", meterModel.getAllMeterData().stream().mapToInt(md -> md.getMeasurements().size()).sum());
                }
                logger.info("Successfully processed {} SDAT files.", sdatFiles.size());
            }

            if (meterModel.getAllMeterData().isEmpty()) {
                return ResponseEntity.badRequest().body(null); // Or a specific error message object
            }

            // Spring/Jackson will automatically serialize the MeterModel object
            // into the JSON structure expected by the frontend's MeterModelResponse.
            return ResponseEntity.ok(meterModel);

        } catch (Exception e) {
            logger.error("Error during file upload or processing: {}", e.getMessage(), e);
            // Return a more informative error message to the frontend
            return ResponseEntity.internalServerError().body(null); // Or a specific error message object
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

        logger.info("Saved file to: {}", filePath.toAbsolutePath());
        return filePath.toFile();
    }
}