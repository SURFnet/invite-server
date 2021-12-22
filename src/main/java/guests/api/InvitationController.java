package guests.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.config.HashGenerator;
import guests.domain.*;
import guests.exception.InvitationNotOpenException;
import guests.exception.NotFoundException;
import guests.mail.MailBox;
import guests.repository.ApplicationRepository;
import guests.repository.InvitationRepository;
import guests.repository.RoleRepository;
import guests.repository.UserRepository;
import guests.validation.EmailFormatValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final EmailFormatValidator emailFormatValidator = new EmailFormatValidator();

    @Autowired
    public InvitationController(InvitationRepository invitationRepository,
                                UserRepository userRepository,
                                ApplicationRepository applicationRepository,
                                RoleRepository roleRepository,
                                MailBox mailBox) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.roleRepository = roleRepository;
        this.mailBox = mailBox;
    }

    @GetMapping("/{hash}")
    public ResponseEntity<Invitation> invitation(@PathVariable("hash") String hash) throws JsonProcessingException {
        Invitation invitation = invitationRepository.findByHash(hash).orElseThrow(NotFoundException::new);
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
        Invitation invitationFromDB = invitationRepository.findByHash(invitation.getHash()).orElseThrow(NotFoundException::new);
        if (!invitationFromDB.getStatus().equals(Status.OPEN)) {
            throw new InvitationNotOpenException();
        }
        invitationFromDB.setStatus(invitation.getStatus());
        if (invitation.getStatus().equals(Status.ACCEPTED)) {
            //create user
            Institution institution = invitationFromDB.getInstitution();
            User user = new User(institution, invitationFromDB.getIntendedRole(), authentication.getTokenAttributes());
            Set<UserRole> userRoles = invitationFromDB.getRoles().stream().map(invitationRole -> new UserRole(user, invitationRole.getRole(), invitationRole.getEndDate())).collect(Collectors.toSet());
            user.setRoles(userRoles);
            if (StringUtils.hasText(institution.getAupUrl())) {
                user.getAups().add(new Aup(user, institution));
            }
            User newUser = userRepository.save(user);
            //TODO send notifications to all applications connected to the roles
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        }
        invitationFromDB.setStatus(Status.DENIED);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Map<String, Integer>> invite(User user, @RequestBody InvitationRequest invitationRequest) throws IOException, MessagingException {
        Invitation invitationData = invitationRequest.getInvitation();
        List<String> invites = invitationRequest.getInvites();
        Set<String> emails = emailFormatValidator.validateEmails(invites);
        emails.forEach(email -> {
            Invitation invitation = new Invitation(invitationData.getIntendedRole(), Status.OPEN, HashGenerator.generateHash(), user, email);
            invitation.setMessage(invitationData.getMessage());
            invitation.setInstitution(invitationData.getInstitution());
            invitation.defaults();
            invitation.setEnforceEmailEquality(invitationData.isEnforceEmailEquality());
            invitation.setExpiryDate(invitationData.getExpiryDate());
            if (!CollectionUtils.isEmpty(invitation.getRoles())) {
                invitation.getRoles().forEach(role -> role.setInvitation(invitation));
            }
            Invitation saved = invitationRepository.save(invitation);
            //Ensure all applications are loaded for the roles
            if (!CollectionUtils.isEmpty(saved.getRoles())) {
                saved.getRoles().forEach(role -> {
                    Role transientRole = role.getRole();
                    transientRole.setApplication(roleRepository.findById(transientRole.getId()).get().getApplication());
                });
            }
            mailBox.sendInviteMail(saved);
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("status", 201));
    }

    private void restrictUser(User user, Invitation invitation) throws AuthenticationException {
        verifyUser(user, invitation.getInstitution().getId());
        verifyAuthority(user, invitation.getIntendedRole());
    }

}
