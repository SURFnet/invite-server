package guests.domain;

import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.text.Normalizer;

public interface ServiceProviderIdentifier {

    String getServiceProviderId();

    void setServiceProviderId(String serviceProviderId);
}
