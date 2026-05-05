package com.askmydoc.service;

import com.askmydoc.exceptions.ParsingFailureException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ParsingService {

    private static final Logger logger = LoggerFactory.getLogger(ParsingService.class);

    public String parse(MultipartFile file) throws ParsingFailureException {

        try (InputStream inputStream = file.getInputStream()) {

            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // no limit
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);

            return handler.toString();
        } catch (TikaException | SAXException | IOException e) {
            throw new ParsingFailureException
                    (String.format("Failed to parse file : %s", file.getOriginalFilename()), e);
        }
    }

}
