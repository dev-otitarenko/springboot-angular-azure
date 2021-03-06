package com.maestro.examples.app.azure.filestorage.services;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobContainerItemProperties;
import com.azure.storage.blob.models.BlobItem;
import com.maestro.examples.app.azure.filestorage.utils.CurlUtils;
import com.maestro.examples.app.azure.filestorage.domains.DataBlock;
import com.maestro.examples.app.azure.filestorage.domains.FilePrm;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class AzureBlobStorageServiceTest {
    @Autowired
    AzureBlobStorageService filesService;

    final private String idContainer = UUID.randomUUID().toString();
    final private String idFile1 = UUID.randomUUID().toString();
    final private ArrayList<String> file1_blockIds = new ArrayList<>(
        Arrays.asList (
            Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)),
            Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)),
            Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
        )
    );

    @BeforeAll
    public void setup() {

    }

    @Order(1)
    @Test
    @DisplayName("Uploading is starting")
    void beforeUploadFile() {
        filesService.beforeUpload(this.idContainer);
    }

    @Order(2)
    @Test
    @DisplayName("Uploading a file with 3 parts")
    void uploadFile() {
        filesService.uploadFile(
                idContainer,
                idFile1,
                DataBlock
                  .builder()
                    .base64BlockId(file1_blockIds.get(0))
                    .data(Base64.getEncoder().encodeToString("<html><head></head>".getBytes(StandardCharsets.UTF_8)))
                    .build());
        filesService.uploadFile(this.idContainer,
                idFile1,
                DataBlock
                    .builder()
                        .base64BlockId(file1_blockIds.get(1))
                        .data(Base64.getEncoder().encodeToString("<body>Test</body>".getBytes(StandardCharsets.UTF_8)))
                        .build());
        filesService.uploadFile(this.idContainer,
                idFile1,
                DataBlock
                    .builder()
                        .base64BlockId(file1_blockIds.get(2))
                        .data(Base64.getEncoder().encodeToString("</html>".getBytes(StandardCharsets.UTF_8)))
                        .build());
    }

    @Order(3)
    @Test
    @DisplayName("Completing uploading file. The process finishes if we send array of base64 encoded block ids")
    void completeUploadFile() {
        FilePrm fileInfo = FilePrm
                            .builder()
                                .name("test.html")
                                .type("html")
                                .base64BlockIds(file1_blockIds)
                            .build();
        filesService.completeUploadFile(idContainer, idFile1, fileInfo);
    }

    @Order(4)
    @Test
    @DisplayName("Obtaining a link to this file")
    void getLinkDocFile() throws IOException {
            // exec curl
        String command = String.format("curl -X GET %s", filesService.getLinkFile(idContainer, idFile1));
        Process process = Runtime.getRuntime().exec(command);
            // get content
        final String content = CurlUtils.inputStreamToString(process.getInputStream());
            // asserts
        assertTrue(null != content && !content.isEmpty());
        assertEquals(content.trim(), "<html><head></head><body>Test</body></html>");
    }

    @Order(10)
    @Test
    void listOfContainers() {
        PagedIterable<BlobContainerItem> items = filesService.listContainers();
        assertTrue(items.stream().count() >= 1);
        for (BlobContainerItem itm : items) {
            StringBuilder sb = new StringBuilder()/*,
                          sb1 = new StringBuilder()*/;
            sb.append(" -> [" + itm.getName() + "] ");
//            if (itm.getMetadata() != null) {
//                itm.getMetadata().forEach((k, v) -> { if (sb1.length() > 0) sb1.append(", ");  sb1.append(String.format("%s : %s", k, v)); } );
//            }
//            sb.append(", metadata: {");
//            sb.append(sb1);
//            sb.append(" }");
            BlobContainerItemProperties properties = itm.getProperties();
            sb.append(String.format(", \"leaseState\": \"%s\"", properties.getLeaseState().toString()));
            sb.append(String.format(", \"leaseStatus\": \"%s\"", properties.getLeaseStatus().toString()));
            sb.append(String.format(", \"lastModified\": \"%s\"", properties.getLastModified()));
            sb.append(String.format(", \"ETag\": \"%s\"", properties.getETag()));

            System.out.println(sb.toString());
        }
    }

    @Order(11)
    @Test
    void listOfBlobs() {
        PagedIterable<BlobItem> items = filesService.listBlobs(idContainer);
        assertEquals(items.stream().count(), 1);
        for (BlobItem itm : items) {
            System.out.println(itm.getName());
            assertEquals(itm.getName(), idFile1);
        }
    }

    @Order(99)
    @Test
    @DisplayName("Deleting a file from a specific container")
    void deleteDocFile() {
        filesService.deleteFile(idContainer, idFile1);
    }

    @Order(100)
    @Test
    @DisplayName("Removing a specific container")
    void deleteContainer() {
        filesService.deleteContainer(this.idContainer);
    }
}