package guests.scim;

import guests.domain.Application;
import guests.domain.Role;

public class GroupURN {

    private GroupURN() {
    }

    public static String urnFromRole(String groupUrnPrefix, Role role) {
        Application application = role.getApplication();
        return String.format("%s:%s:%s:%s",
                groupUrnPrefix,
                application.getInstitution().getHomeInstitution().toLowerCase(),
                application.getName().toLowerCase(),
                role.getName()).toLowerCase();
    }

    public static ExternalID parseUrnRole(String urnRole) {
        String[] parts = urnRole.toLowerCase().split(":");
        int length = parts.length;
        return new ExternalID(parts[length - 3], parts[length - 2], parts[length - 1]);
    }

}
