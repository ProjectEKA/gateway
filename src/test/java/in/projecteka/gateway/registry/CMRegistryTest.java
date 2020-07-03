//package in.projecteka.gateway.registry;
//
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static in.projecteka.gateway.registry.ServiceType.CONSENT_MANAGER;
//import static in.projecteka.gateway.testcommon.TestBuilders.string;
//import static in.projecteka.gateway.testcommon.TestBuilders.yamlRegistryMapping;
//import static org.assertj.core.api.Assertions.assertThat;
//
//class CMRegistryTest {
//
//    @Test
//    void returnHostWhenConsentManagerExists() {
//        var clientId = string();
//        var consentManager = yamlRegistryMapping().id(clientId).servesAs(List.of(CONSENT_MANAGER)).build();
//        var cmRegistry = new CMRegistry(new YamlRegistry(List.of(consentManager), null));
//
//        var mayBeHost = cmRegistry.getHostFor(clientId);
//
//        assertThat(mayBeHost).isNotEmpty();
//        assertThat(mayBeHost).contains(consentManager.getHost());
//    }
//}