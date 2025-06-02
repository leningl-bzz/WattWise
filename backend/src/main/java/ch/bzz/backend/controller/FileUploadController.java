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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:63342"})
public class FileUploadController {

    // POST /api/files/upload
    @PostMapping("/upload")
    public ResponseEntity<List<Measurement>> uploadFiles(@RequestParam(value = "sdat", required = false) MultipartFile sdatFile, @RequestParam(value = "esl", required = false) MultipartFile eslFile) {
        try {
            File tempSDAT = null;
            File tempESL = null;
            SDATParser.ParsedSDAT parsedSDAT = null;
            Map<String, Double> eslMap = null;

            if (sdatFile != null && !sdatFile.isEmpty()) {
                tempSDAT = convertMultipartToFile(sdatFile);
                parsedSDAT = SDATParser.parseSDATFile(tempSDAT);
            }

            if (eslFile != null && !eslFile.isEmpty()) {
                tempESL = convertMultipartToFile(eslFile);
                eslMap = ESLParser.parseESLFile(tempESL);
            }

            List<Measurement> result;

            if (parsedSDAT != null && eslMap != null) {
                // Merge if both files are present
                result = MeasurementMerger.mergeWithESL(
                        parsedSDAT.getValues(),
                        eslMap,
                        "1-0:1.8.0",
                        "1-0:2.8.0"
                );
            } else if (parsedSDAT != null) {
                // Only SDAT provided
                result = parsedSDAT.getValues();
            } else {
                // If only ESL or none is provided, return bad request
                return ResponseEntity.badRequest().body(null);
            }

            if (tempSDAT != null) tempSDAT.delete();
            if (tempESL != null) tempESL.delete();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
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
