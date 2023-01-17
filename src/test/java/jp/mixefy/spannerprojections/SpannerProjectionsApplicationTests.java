package jp.mixefy.spannerprojections;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.cloud.spanner.*;
import com.google.cloud.spring.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import com.google.cloud.spring.data.spanner.core.admin.SpannerSchemaUtils;
import com.google.cloud.spring.data.spanner.core.mapping.SpannerMappingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.SpannerEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Testcontainers
class SpannerProjectionsApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    Spanner spanner;
    @Autowired
    SpannerDatabaseAdminTemplate spannerAdmin;
    @Autowired
    SpannerSchemaUtils spannerSchemaUtils;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    SpannerMappingContext spannerMappingContext;

    private static final String PROJECT_ID = "test-project";
    private static final String INSTANCE_ID = "test-instance";

    @Container
    public static final SpannerEmulatorContainer spannerEmulator = new SpannerEmulatorContainer(
        DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator")
    );

    @DynamicPropertySource
    static void emulatorProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.cloud.gcp.spanner.emulator-host", spannerEmulator::getEmulatorGrpcEndpoint);
    }

    @TestConfiguration
    static class EmulatorConfiguration {
        @Bean
        CredentialsProvider googleCredentials() {
            return NoCredentialsProvider.create();
        }
    }

    @BeforeEach
    void setup() throws ExecutionException, InterruptedException {
        ensureEmulatorInstanceExists();
        ensureTableExists(Account.class);
    }

    private void ensureEmulatorInstanceExists() throws InterruptedException, ExecutionException {
        var instanceAdminClient = spanner.getInstanceAdminClient();
        var instanceId = InstanceId.of(PROJECT_ID, INSTANCE_ID);

        try {
            instanceAdminClient.getInstance(instanceId.getInstance());
        } catch (InstanceNotFoundException e) {
            var emulatorConfig = InstanceConfigId.of(PROJECT_ID, "emulator-config");
            var instanceInfo = InstanceInfo.newBuilder(instanceId).setInstanceConfigId(emulatorConfig).build();
            instanceAdminClient.createInstance(instanceInfo).get();
        }
    }

    private void ensureTableExists(Class<?> type) {
        var persistentEntity = spannerMappingContext.getPersistentEntity(type);
        if (!spannerAdmin.tableExists(persistentEntity.tableName())) {
            spannerAdmin.executeDdlStrings(
                Arrays.asList(spannerSchemaUtils.getCreateTableDdlString(type)),
                !spannerAdmin.databaseExists());
        }
    }

    @Test
    void testCrud() {
        var account = new Account();
        account.setId(UUID.randomUUID().toString());
        account.setName("Yamada");
        var saved = accountRepository.save(account);

        var retrieved = accountRepository.findById(saved.getId()).get();
        assertEquals("Not Yamada", retrieved.getName());

        var retrievedProjection = accountRepository.findProjectionById(saved.getId());
        assertEquals("Yamada", retrievedProjection.getName());

        var retrievedProjectionUsingQuery = accountRepository.findProjectionUsingQuery(saved.getId());
        assertEquals("Yamada", retrievedProjectionUsingQuery.getName());

        accountRepository.delete(retrieved);
        assertFalse(accountRepository.existsById(saved.getId()));
    }
}
