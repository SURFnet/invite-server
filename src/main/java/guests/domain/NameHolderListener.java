package guests.domain;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.NotNull;

public class NameHolderListener {

    @PrePersist
    @PreUpdate
    public void beforeSave(@NotNull NameHolder nameHolder) {
        nameHolder.nameUrnCompatibilityCheck();
    }
}
