package com.njxs.lims.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SamplePhotoUploadTests {

  private static final String BUCKET = "lims-test-files";

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static final GenericContainer<?> MINIO = new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
      .withEnv("MINIO_ROOT_USER", "minioadmin")
      .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
      .withExposedPorts(9000)
      .withCommand("server", "/data")
      .waitingFor(Wait.forHttp("/minio/health/ready")
          .forPort(9000)
          .forStatusCode(200)
          .withStartupTimeout(Duration.ofSeconds(90)));

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("lims.storage.endpoint", SamplePhotoUploadTests::minioEndpoint);
    registry.add("lims.storage.access-key", () -> "minioadmin");
    registry.add("lims.storage.secret-key", () -> "minioadmin");
    registry.add("lims.storage.bucket", () -> BUCKET);
  }

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  SamplePhotoUploadTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void uploadedSamplePhotoStoresObjectAndMetadataSeparatelyFromSample() throws Exception {
    String applicantToken = login("portal", "portal.applicant", "Applicant123!");
    String salesToken = login("internal", "internal.sales", "Sales123!");
    String sampleToken = login("internal", "internal.sample", "Sales123!");
    UUID sampleId = createSample(applicantToken, salesToken);

    byte[] content = "todo-42-sample-photo".getBytes();
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "sample-label.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        content
    );

    String response = mockMvc.perform(multipart("/samples/{id}/photos", sampleId)
            .file(file)
            .param("note", "receiving photo")
            .header(HttpHeaders.AUTHORIZATION, bearer(sampleToken)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sampleId").value(sampleId.toString()))
        .andExpect(jsonPath("$.fileName").value("sample-label.jpg"))
        .andExpect(jsonPath("$.sha256").isNotEmpty())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode photo = objectMapper.readTree(response);
    UUID fileId = UUID.fromString(photo.get("fileId").asText());
    String objectKey = photo.get("objectKey").asText();

    String sha256 = jdbcTemplate.queryForObject(
        "select sha256 from file_metadata where id = ?",
        String.class,
        fileId
    );
    assertThat(sha256).hasSize(64);
    Long sizeBytes = jdbcTemplate.queryForObject(
        "select size_bytes from file_metadata where id = ?",
        Long.class,
        fileId
    );
    assertThat(sizeBytes).isEqualTo(content.length);

    assertThat(minioClient().statObject(StatObjectArgs.builder()
        .bucket(BUCKET)
        .object(objectKey)
        .build()).size()).isEqualTo(content.length);

    jdbcTemplate.update("delete from sample where id = ?", sampleId);
    Integer metadataCount = jdbcTemplate.queryForObject(
        "select count(*) from file_metadata where id = ?",
        Integer.class,
        fileId
    );
    assertThat(metadataCount).isEqualTo(1);
    assertThat(minioClient().statObject(StatObjectArgs.builder()
        .bucket(BUCKET)
        .object(objectKey)
        .build()).size()).isEqualTo(content.length);
  }

  private UUID createSample(String applicantToken, String salesToken) throws Exception {
    UUID submissionId = createDraft(applicantToken);
    mockMvc.perform(post("/submissions/{id}/submit", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
        .andExpect(status().isOk());
    mockMvc.perform(post("/submissions/{id}/accept", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk());
    String splitResponse = mockMvc.perform(post("/submissions/{id}/split-projects", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "splitStrategy": "MANUAL",
                  "projectDrafts": [
                    {
                      "projectType": "STD_ACCREDITED",
                      "qualificationType": "CMA",
                      "serviceType": "NORMAL"
                    }
                  ]
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID projectId = UUID.fromString(objectMapper.readTree(splitResponse).get(0).get("id").asText());
    return jdbcTemplate.queryForObject(
        "select id from sample where project_id = ?",
        UUID.class,
        projectId
    );
  }

  private UUID createDraft(String accessToken) throws Exception {
    ObjectNode request = objectMapper.createObjectNode();
    request.put("formCode", "SUBMISSION_VER15");
    request.put("schemaVersion", 1);
    request.set("data", completeSubmissionData());

    String response = mockMvc.perform(post("/submissions")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private ObjectNode completeSubmissionData() {
    ObjectNode data = objectMapper.createObjectNode();
    data.put("sponsorNameZh", "Todo 42 Sponsor");
    data.put("sponsorNameEn", "Todo 42 Sponsor");
    data.put("sponsorAddressZh", "No. 42 Sponsor Road");
    data.put("sponsorAddressEn", "No. 42 Sponsor Road");
    data.put("manufacturerNameZh", "Todo 42 Manufacturer");
    data.put("manufacturerNameEn", "Todo 42 Manufacturer");
    data.put("manufacturerAddressZh", "No. 42 Manufacturer Road");
    data.put("manufacturerAddressEn", "No. 42 Manufacturer Road");
    data.put("contactName", "Todo Contact");
    data.put("contactPhone", "13800000042");
    data.put("contactEmail", "todo42@example.com");
    data.put("officialReportEmail", "reports.todo42@example.com");
    data.put("shippingInfo", "Ship to Todo 42 Sponsor");
    data.put("requiresGlp", false);
    data.put("requiresHardCopyOfElectronicReport", false);
    data.put("protocolLanguage", "CHS");
    data.put("reportLanguage", "CHS");
    data.put("submittedQuantityText", "10 pcs");
    data.put("sampleDisposition", "DESTROY_DISCARD");
    data.put("sampleNameZh", "Todo 42 Sample");
    data.put("sampleNameEn", "Todo 42 Sample");
    data.put("sterilizationState", "NON_STERILE");
    data.put("casCode", "N/A");
    data.put("modelRef", "MODEL-42");
    data.put("sizeSpec", "10 mm");
    data.put("lotBatch", "LOT-42");
    data.put("rawMaterial", "Polymer");
    data.put("packagingMaterial", "Pouch");
    data.put("physicalState", "SOLID");
    data.put("color", "White");
    data.put("density", "N/A");
    data.put("stability", "Stable");
    data.put("solubility", "Insoluble");
    data.put("storageCondition", "ROOM_TEMPERATURE");
    data.put("intendedUse", "Validation sample");
    data.put("additionalInfo", "No additional info");
    data.put("canBeCut", true);
    data.put("componentsToTestZh", "N/A");
    data.put("componentsToTestEn", "N/A");
    data.put("workingTemperatureCelsius", "37");
    data.put("withstand50C", false);
    data.put("clinicalContactDuration", "A_LIMITED");
    data.put("extractionBasis", "SURFACE_AREA");
    data.put("thinnestThicknessMm", "1");
    data.put("specialInstructions", "N/A");
    return data;
  }

  private String login(String channel, String username, String password) throws Exception {
    String response = mockMvc.perform(post("/auth/{channel}/login", channel)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode token = objectMapper.readTree(response);
    return token.get("accessToken").asText();
  }

  private MinioClient minioClient() {
    return MinioClient.builder()
        .endpoint(minioEndpoint())
        .credentials("minioadmin", "minioadmin")
        .build();
  }

  private static String minioEndpoint() {
    return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
