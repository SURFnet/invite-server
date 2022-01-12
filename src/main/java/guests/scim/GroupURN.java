package guests.scim;

import guests.domain.Application;
import guests.domain.Role;

public class GroupURN {

    public static String urnFromRole(String groupUrnPrefix, Role role) {
        Application application = role.getApplication();
        return String.format("%s:%s:%s:%s",
                groupUrnPrefix,
                application.getInstitution().getHomeInstitution().toLowerCase(),
                application.getName().toLowerCase(),
                role.getName()).toLowerCase();
    }

    public static ExternalID parseUrnRole(String urnRole) {
        String[] parts = urnRole.split(":");
        return new ExternalID(parts[1], parts[2], parts[3]);
    }

}
