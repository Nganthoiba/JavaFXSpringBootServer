package com.javafxserver.web.controllers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.javafxserver.config.Config;
import com.javafxserver.service.TokenService;
import com.javafxserver.ui.PinPrompt;
import com.javafxserver.utils.ConvertFile;
import com.javafxserver.digitalsigner.Coordinate;
import com.javafxserver.digitalsigner.PDFSigner;
import com.javafxserver.digitalsigner.SignatureDetail;
import com.javafxserver.exceptions.HandleExceptionMessage;


@RestController
public class AppController {

    @PostMapping("/uploadPDFtoEsign")
    public ResponseEntity<?> digitallySignPdf(
            @RequestParam("my_file") MultipartFile multipartFile,
            @RequestParam("x") float x,
            @RequestParam("y") float y,
            @RequestParam("location") String location) {

        byte[] signedPdfBytes = null;
        File tempFile = null;
        try {
            if (multipartFile.isEmpty()) {
                throw new IllegalArgumentException("No file is uploaded");
            }

            String secretPin = PinPrompt.requestUserPinBlocking(pin -> {
                if (Config.PIN != null && !pin.equals(Config.PIN)) {
                    return false;
                }
                return true;
            });

            TokenService tokenService = new TokenService();
            tokenService.cleanup();
            tokenService.detectToken(Config.getConfigFile(), secretPin);

            SignatureDetail signDetail = new SignatureDetail();
            signDetail.coordinate = new Coordinate(x, y);
            signDetail.location = location;
            tempFile = ConvertFile.toFile(multipartFile);
            signedPdfBytes = PDFSigner.signPDF(tempFile, secretPin, tokenService, signDetail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "signed_doc.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(signedPdfBytes);

        } catch (IllegalArgumentException e) {
        	String messageString = HandleExceptionMessage.getMessage(e);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", messageString);
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody);

        } catch (Exception e) {
        	String messageString = HandleExceptionMessage.getMessage(e);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", messageString);
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody);

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
