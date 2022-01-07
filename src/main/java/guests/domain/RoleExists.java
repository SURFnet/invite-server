package guests.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class RoleExists extends ObjectExists implements Serializable {

    private Long applicationId;

    public RoleExists(boolean existingObject, String uniqueAttribute, Long applicationId) {
        super(existingObject, uniqueAttribute);
        this.applicationId = applicationId;
    }

}
