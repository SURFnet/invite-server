package guests.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ApplicationExists extends ObjectExists implements Serializable {

    private Long institutionId;

    public ApplicationExists(boolean existingObject, String uniqueAttribute, Long institutionId) {
        super(existingObject, uniqueAttribute);
        this.institutionId = institutionId;
    }

}
