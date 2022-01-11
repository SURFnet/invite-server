package guests.domain;

import java.util.Set;

public enum Authority {

    SUPER_ADMIN(3), INSTITUTION_ADMINISTRATOR(2), INVITER(1), GUEST(0);

    private final int rights;

    Authority(int rights) {
        this.rights = rights;
    }

    public boolean isAllowed(Authority requiredAuthority) {
        return rights >= requiredAuthority.rights;
    }

    public boolean isAllowedForAll(Set<Authority> requiredAuthorities) {
        return requiredAuthorities.stream().allMatch(requiredAuthority -> rights >= requiredAuthority.rights);
    }

    public String friendlyName() {
        return this.name().replaceAll("_", " ").toLowerCase();
    }

}
