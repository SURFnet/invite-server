package guests.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.config.HashGenerator;
import guests.domain.*;
import guests.exception.InvitationEmailMatchingException;
import guests.exception.NotFoundException;
import guests.mail.MailBox;
import guests.repository.ApplicationRepository;
import guests.repository.InvitationRepository;
import guests.repository.RoleRepository;
import guests.repository.UserRepository;
import guests.scim.SCIMService;
import guests.validation.EmailFormatValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static guests.api.Shared.verifyAuthority;
import static guests.api.Shared.verifyUser;

@RestController
@RequestMapping(value = "/guests/api/invitations", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final RoleRepository roleRepository;
    private final MailBox mailBox;
    private final SCIMService scimService;

    private final EmailFormatValidator emailFormatValidator = new EmailFormatValidator();

    @Autowired
    public InvitationController(InvitationRepository invitationRepository,
                                UserRepository userRepository,
                                ApplicationRepository applicationRepository,
                                RoleRepository roleRepository,
                                MailBox mailBox,
                                SCIMService scimService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.roleRepository = roleRepository;
        this.mailBox = mailBox;
        this.scimService = scimService;
    }

    @GetMapping("/{hash}")
    public ResponseEntity<Invitation> invitationI(@PathVariable("hash") String hash) throws JsonProcessingException {
        Invitation invitation = invitationRepository.findByHashAndStatus(hash, Status.OPEN).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(invitation);
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<Invitation>> getByInstitution(@PathVariable("institutionId") Long institutionId, User authenticatedUser) {
        verifyUser(authenticatedUser, institutionId);
        return ResponseEntity.ok(invitationRepository.findByInstitution_id(institutionId));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<Invitation>> getByApplication(@PathVariable("applicationId") Long applicationId, User authenticatedUser) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(NotFoundException::new);
        verifyUser(authenticatedUser, application.getInstitution().getId());
        return ResponseEntity.ok(invitationRepository.findByRoles_role_application_id(applicationId));
    }

    @PostMapping
    public ResponseEntity accept(BearerTokenAuthentication authentication,
                                 @RequestBody Invitation invitation) throws JsonProcessingException {
        Invitation invitationFromDB = invitationRepository.findByHashAndStatus(invitation.getHash(), Status.OPEN).orElseThrow(NotFoundException::new);
        invitationFromDB.setStatus(invitation.getStatus());
        if (invitation.getStatus().equals(Status.ACCEPTED)) {
            Object details = authentication.getDetails();
            User newUser;
            User user;
            if (details instanceof User) {
                user = (User) details;
                checkEmailEquality(user, invitationFromDB);
            } else {
                Institution institution = invitationFromDB.getInstitution();
                user = new User(institution, invitationFromDB.getIntendedAuthority(), authentication.getTokenAttributes());
                checkEmailEquality(user, invitationFromDB);
                if (StringUtils.hasText(institution.getAupUrl())) {
                    user.getAups().add(new Aup(user, institution));
                }
                scimService.newUserRequest(user);
            }
            invitationFromDB.getRoles()
                    .forEach(invitationRole -> {
                        user.addUserRole(new UserRole(invitationRole.getRole(), invitationRole.getEndDate()));
                        List<User> users = userRepository.findByRoles_role_id(invitationRole.getRole().getId());
                        scimService.updateRoleRequest(invitationRole.getRole(), users);
                    });
            newUser = userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        }
        invitationFromDB.setStatus(Status.DENIED);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Map<String, Integer>> invite(User user, @RequestBody InvitationRequest invitationRequest) {
        Invitation invitationData = invitationRequest.getInvitation();
        restrictUser(user, invitationData);
        List<String> invites = invitationRequest.getInvites();
        Set<String> emails = emailFormatValidator.validateEmails(invites);
        emails.forEach(email -> {
            Invitation invitation = new Invitation(invitationData.getIntendedAuthority(), Status.OPEN, HashGenerator.generateHash(), user, email);
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
                Role persistentRole = roleRepository.findById(transientRole.getId()).get();
                transientRole.setApplication(persistentRole.getApplication());
                transientRole.setName(persistentRole.getName());
            });
            mailBox.sendInviteMail(user, saved);
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("status", 201));
    }

    private void restrictUser(User user, Invitation invitation) throws AuthenticationException {
        verifyUser(user, invitation.getInstitution().getId());
        verifyAuthority(user, invitation.getIntendedAuthority());
    }

    private void checkEmailEquality(User user, Invitation invitation) {
        if (invitation.isEnforceEmailEquality() && !invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvitationEmailMatchingException(
                    String.format("Invitation email %s does not match user email", invitation.getEmail(), user.getEmail()));
        }
    }

}
