package in.projecteka.gateway.common;

import java.util.Map;
import java.util.Optional;

public enum Role {
    HIP,
    HIU,
    CM,
    GATEWAY,
    ADMIN;

    public static Optional<Role> valueOfIgnoreCase(String mayBeRole) {
        var hip = Map.of(HIP.name().toLowerCase(), HIP,
                HIU.name().toLowerCase(), HIU,
                CM.name().toLowerCase(), CM,
                GATEWAY.name().toLowerCase(), GATEWAY);
        return mayBeRole == null
                ? Optional.empty()
                : Optional.ofNullable(hip.getOrDefault(mayBeRole.toLowerCase(), null));
    }

    public static Optional<Role> valOfIgnoreCase(String mayBeRole) {
        return  mayBeRole == null
                ? Optional.empty()
                : mayBeRole.equalsIgnoreCase(ADMIN.name()) ? Optional.of(ADMIN) : Optional.empty();
    }
}
