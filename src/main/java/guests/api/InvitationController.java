package guests.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.config.HashGenerator;
import guests.domain.*;
import guests.exception.InvitationEmailMatchingException;
import guests.exception.NotFoundException;
import guests.exception.UserRestrictionException;
import guests.mail.MailBox;
import guests.repository.*;
import guests.scim.SCIMService;
import guests.validation.EmailFormatValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static guests.api.Shared.*;

@RestController
@RequestMapping(value = "/guests/api/invitations", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final RoleRepository roleRepository;
    private final InstitutionRepository institutionRepository;

    private final MailBox mailBox;
    private final SCIMService scimService;

    private final EmailFormatValidator emailFormatValidator = new EmailFormatValidator();

    @Autowired
    public InvitationController(InvitationRepository invitationRepository,
                                UserRepository userRepository,
                                ApplicationRepository applicationRepository,
                                RoleRepository roleRepository,
                                InstitutionRepository institutionRepository,
                                MailBox mailBox,
                                SCIMService scimService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.institutionRepository = institutionRepository;
        this.roleRepository = roleRepository;
        this.mailBox = mailBox;
        this.scimService = scimService;
    }

    @GetMapping("/{hash}")
    public ResponseEntity<Invitation> invitationByHash(BearerTokenAuthentication authentication, @PathVariable("hash") String hash) {
        Invitation invitation = invitationRepository.findByHashAndStatus(hash, Status.OPEN).orElseThrow(NotFoundException::new);
        Object details = authentication.getDetails();
        String email = details instanceof User ? ((User) details).getEmail() : (String) authentication.getTokenAttributes().get("email");
        invitation.setEmailEqualityConflict(invitation.isEnforceEmailEquality() && !invitation.getEmail().equalsIgnoreCase(email));
        return ResponseEntity.ok(invitation);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<Invitation> invitationById(User authenticatedUser, @PathVariable("id") Long id) {
        Invitation invitation = invitationRepository.findById(id).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, invitation.getInstitution().getId(), Authority.INVITER);
        return ResponseEntity.ok(invitation);
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<Invitation>> getByInstitution(@PathVariable("institutionId") Long institutionId, User authenticatedUser) {
        verifyAuthority(authenticatedUser, institutionId, Authority.INVITER);
        return ResponseEntity.ok(invitationRepository.findByInstitution_id(institutionId));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<Invitation>> getByApplication(@PathVariable("applicationId") Long applicationId, User authenticatedUser) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, application.getInstitution().getId(), Authority.INVITER);
        return ResponseEntity.ok(invitationRepository.findByRoles_role_application_id(applicationId));
    }

    @PostMapping
    public ResponseEntity<User> accept(BearerTokenAuthentication authentication,
                                       @RequestBody Invitation invitation) throws JsonProcessingException {
        Invitation invitationFromDB = invitationRepository.findByHashAndStatus(invitation.getHash(), Status.OPEN).orElseThrow(NotFoundException::new);
        invitationFromDB.setStatus(invitation.getStatus());
        Object details = authentication.getDetails();
        User newUser;
        User user;
        Institution institution = invitationFromDB.getInstitution();
        if (details instanceof User detailsFromUser) {
            user = userRepository.findById(detailsFromUser.getId()).orElseThrow(NotFoundException::new);
            Optional<InstitutionMembership> membershipOptional = user.getInstitutionMemberships().stream()
                    .filter(ms -> ms.getInstitution().getId().equals(institution.getId()))
                    .findFirst();
            if (membershipOptional.isEmpty()) {
                user.addMembership(new InstitutionMembership(invitationFromDB.getIntendedAuthority(), institution));
            }

        } else {
            user = new User(institution, invitationFromDB.getIntendedAuthority(), authentication.getTokenAttributes());
        }
        checkEmailEquality(user, invitationFromDB);
        if (!user.hasAgreedWithAup(institution)) {
            user.addAup(new Aup(institution));
        }
        /*
         * Chicken & egg problem. The user including his / hers roles must be first known in Scim, and then we
         * need to send the updateRoleRequests for each new Role of this user.
         */
        List<Role> newRoles = new ArrayList<>();
        invitationFromDB.getRoles()
                .forEach(invitationRole -> {
                    Role role = invitationRole.getRole();
                    if (user.getRoles().stream().noneMatch(userRole -> userRole.getRole().getId().equals(role.getId()))) {
                        user.addUserRole(new UserRole(role, invitationRole.getEndDate()));
                        newRoles.add(role);
                    }
                });
        // This will assign the external ID to the userRoles
        if (user.getId() == null) {
            scimService.newUserRequest(user);
        }
        newUser = userRepository.save(user);

        newRoles.forEach(role -> {
            List<User> users = userRepository.findByRoles_role_id(role.getId());
            scimService.updateRoleRequest(role, users);
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PutMapping
    public ResponseEntity<Map<String, Integer>> invite(User user, @RequestBody InvitationRequest invitationRequest) {
        Invitation invitationData = invitationRequest.getInvitation();
        Institution institution = institutionRepository.findById(invitationRequest.getInstitutionId()).orElseThrow(NotFoundException::new);

        verifyAuthority(user, institution.getId(), invitationData.getIntendedAuthority());
        Authority authority = user.authorityByInstitution(institution.getId()).orElseThrow(() -> userRestrictedException(user, institution.getId()));
        // Inviter can only invite GUESTS
        if (authority.equals(Authority.INVITER) && !invitationData.getIntendedAuthority().equals(Authority.GUEST)) {
            throw new UserRestrictionException("Authority mismatch");
        }

        List<String> invites = invitationRequest.getInvites();
        Set<String> emails = emailFormatValidator.validateEmails(invites);
        emails.forEach(email -> {
            Invitation invitation = new Invitation(
                    invitationData.getIntendedAuthority(),
                    Status.OPEN,
                    HashGenerator.generateHash(),
                    user,
                    institution,
                    email);
            invitation.setMessage(invitationData.getMessage());
            invitation.setInstitution(invitationData.getInstitution());
            invitation.defaults();
            invitation.setEnforceEmailEquality(invitationData.isEnforceEmailEquality());
            invitation.setExpiryDate(invitationData.getExpiryDate());
            invitationData.getRoles().forEach(invitation::addInvitationRole);
            Invitation saved = invitationRepository.save(invitation);
            //Ensure all the data is loaded for the roles to be rendered in the email
            saved.getRoles().forEach(invitationRole -> {
                Role transientRole = invitationRole.getRole();
                Role persistentRole = roleRepository.findById(transientRole.getId()).orElseThrow(NotFoundException::new);
                transientRole.setApplication(persistentRole.getApplication());
                transientRole.setName(persistentRole.getName());
            });
            mailBox.sendInviteMail(user, saved);
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("status", 201));
    }

    @PutMapping("/resend")
    public ResponseEntity<Map<String, Integer>> resend(User authenticatedUser, @RequestBody Map<String, Object> invitation) {
        Number number = (Number) invitation.get("id");
        Invitation invitationFromDB = invitationRepository.findById(number.longValue()).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, invitationFromDB.getInstitution().getId(), Authority.INVITER);

        invitationFromDB.setMessage((String) invitation.get("message"));
        invitationRepository.save(invitationFromDB);

        mailBox.sendInviteMail(authenticatedUser, invitationFromDB);
        return createdResponse();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Map<String, Integer>> deleteInvitation(User authenticatedUser, @PathVariable("id") Long id) {
        Invitation invitation = invitationRepository.findById(id).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, invitation.getInstitution().getId(), Authority.INVITER);
        invitationRepository.delete(invitation);
        return createdResponse();
    }

    private void checkEmailEquality(User user, Invitation invitation) {
        if (invitation.isEnforceEmailEquality() && !invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvitationEmailMatchingException(
                    String.format("Invitation email %s does not match user email %s", invitation.getEmail(), user.getEmail()));
        }
    }

}
