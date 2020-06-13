package in.projecteka.gateway.registry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestBuilders.yamlRegistryMapping;
import static org.assertj.core.api.Assertions.assertThat;

class BridgeRegistryTest {

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, names = {"HIP", "HIU"})
    void returnHostWhenBridgeExists(ServiceType serviceType) {
        var clientId = string();
        var bridge = yamlRegistryMapping().servesAs(List.of(serviceType)).id(clientId).build();
        BridgeRegistry bridgeRegistry = new BridgeRegistry(new YamlRegistry(null, List.of(bridge)));

        var mayBeHost = bridgeRegistry.getHostFor(clientId, serviceType);

        assertThat(mayBeHost).isNotEmpty();
        assertThat(mayBeHost).contains(bridge.getHost());
    }
}