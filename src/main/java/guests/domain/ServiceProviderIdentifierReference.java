package guests.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class ServiceProviderIdentifierReference implements ServiceProviderIdentifier {

    private final String serviceProviderId;

    @Override
    public void setServiceProviderId(String serviceProviderId) {

    }
}