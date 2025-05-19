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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileUploadController {

    // POST /api/files/upload
    @PostMapping("/upload")
    public ResponseEntity<List<Measurement>> uploadFiles(@RequestParam("sdat") MultipartFile sdatFile, @RequestParam("esl") MultipartFile eslFile) {
        try {
            // Save uploaded files temporarily
            File tempSDAT = convertMultipartToFile(sdatFile);
            File tempESL = convertMultipartToFile(eslFile);

            // Parse SDAT and ESL files
            SDATParser.ParsedSDAT parsedSDAT = SDATParser.parseSDATFile(tempSDAT);
            Map<String, Double> eslMap = ESLParser.parseESLFile(tempESL);

            // Merge parsed data (OBIS codes: Bezug = 1-0:1.8.0, Einspeisung = 1-0:2.8.0)
            List<Measurement> merged = MeasurementMerger.mergeWithESL(
                    parsedSDAT.getValues(),
                    eslMap,
                    "1-0:1.8.0",
                    "1-0:2.8.0"
            );

            tempSDAT.delete();
            tempESL.delete();

            return ResponseEntity.ok(merged);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    private File convertMultipartToFile(MultipartFile multipartFile) throws Exception {
        File convFile = File.createTempFile("upload_", "_" + multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(multipartFile.getBytes());
        }
        return convFile;
    }
}
