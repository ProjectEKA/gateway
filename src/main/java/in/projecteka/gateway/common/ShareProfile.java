package in.projecteka.gateway.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "gateway.shareprofile")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class ShareProfile {
    private final boolean enable;
}
