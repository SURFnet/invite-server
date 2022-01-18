package guests.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ServiceProviderIdentifierRef implements ServiceProviderIdentifier {

    private final String serviceProviderId;

    @Override
    public void setServiceProviderId(String serviceProviderId) {

    }
}
