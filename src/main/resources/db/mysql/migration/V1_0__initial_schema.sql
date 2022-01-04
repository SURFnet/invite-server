CREATE TABLE `institutions`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT,
    `entity_id`        varchar(255) NOT NULL,
    `home_institution` varchar(255) NOT NULL,
    `display_name`     varchar(255) DEFAULT NULL,
    `aup_url`          varchar(255) DEFAULT NULL,
    `aup_version`      varchar(255) DEFAULT NULL,
    `created_by`       varchar(255) NOT NULL,
    `created_at`       datetime     DEFAULT CURRENT_TIMESTAMP,
    `updated_by`       varchar(255) DEFAULT NULL,
    `updated_at`       datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `applications_unique_entity_id` (`entity_id`),
    UNIQUE KEY `applications_unique_home_institution` (`home_institution`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `applications`
(
    `id`                         bigint       NOT NULL AUTO_INCREMENT,
    `entity_id`                  varchar(255) NOT NULL,
    `name`                       varchar(255) NOT NULL,
    `display_name`               text         DEFAULT NULL,
    `provisioning_hook_url`      varchar(255) DEFAULT NULL,
    `provisioning_hook_username` varchar(255) DEFAULT NULL,
    `provisioning_hook_password` varchar(255) DEFAULT NULL,
    `provisioning_hook_email`    varchar(255) DEFAULT NULL,
    `landing_page`               varchar(255) DEFAULT NULL,
    `institution_id`             bigint       NOT NULL,
    `created_by`                 varchar(255) NOT NULL,
    `created_at`                 datetime     DEFAULT CURRENT_TIMESTAMP,
    `updated_by`                 varchar(255) DEFAULT NULL,
    `updated_at`                 datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `applications_unique_entity_id` (`entity_id`),
    CONSTRAINT `fk_applications_institution` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `users`
(
    `id`                       bigint       NOT NULL AUTO_INCREMENT,
    `eduperson_principal_name` varchar(255) NOT NULL,
    `given_name`               varchar(255) DEFAULT NULL,
    `family_name`              varchar(255) DEFAULT NULL,
    `email`                    varchar(255) DEFAULT NULL,
    `created_at`               datetime     DEFAULT CURRENT_TIMESTAMP,
    `unspecified_id`           varchar(255) NOT NULL,
    `last_activity`            datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `users_unique_eppn` (`eduperson_principal_name`),
    UNIQUE INDEX `users_unique_unspecified_id` (unspecified_id)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `institution_memberships`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `authority`      varchar(255) NOT NULL,
    `created_at`     datetime DEFAULT CURRENT_TIMESTAMP,
    `user_id`        bigint       NOT NULL,
    `institution_id` bigint       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `institution_memberships_unique` (`user_id`, `institution_id`),
    CONSTRAINT `fk_institution_memberships_institution` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_institution_memberships_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `roles`
(
    `id`                  bigint       NOT NULL AUTO_INCREMENT,
    `name`                varchar(255) NOT NULL,
    `display_name`        text                  DEFAULT NULL,
    `landing_page`        varchar(255)          DEFAULT NULL,
    `created_by`          varchar(255) NOT NULL,
    `instant_available`   bool                  DEFAULT 1,
    `created_at`          datetime              DEFAULT CURRENT_TIMESTAMP,
    `updated_by`          varchar(255)          DEFAULT NULL,
    `updated_at`          datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `application_id`      bigint       NOT NULL,
    `authority`           varchar(255) NOT NULL DEFAULT 'INVITER',
    `service_provider_id` varchar(255)          DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `roles_unique_application_name` (`name`, `application_id`),
    CONSTRAINT `fk_roles_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `user_roles`
(
    `id`                  bigint NOT NULL AUTO_INCREMENT,
    `user_id`             bigint NOT NULL,
    `role_id`             bigint NOT NULL,
    `end_date`            datetime     DEFAULT NULL,
    `service_provider_id` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_roles_unique_user_role` (`user_id`, `role_id`),
    CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `invitations`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `intended_authority`     varchar(255) NOT NULL,
    `status`                 varchar(255) DEFAULT NULL,
    `hash`                   varchar(255) DEFAULT NULL,
    `email`                  varchar(255) NOT NULL,
    `message`                varchar(255) DEFAULT NULL,
    `created_at`             datetime     DEFAULT CURRENT_TIMESTAMP,
    `expiry_date`            datetime     NOT NULL,
    `enforce_email_equality` bool         DEFAULT 0,
    `inviter_id`             bigint       NOT NULL,
    `institution_id`         bigint       NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `index_invitation_hash` (`hash`),
    CONSTRAINT `fk_invitations_user` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_invitations_institution` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `invitation_roles`
(
    `id`            bigint NOT NULL AUTO_INCREMENT,
    `invitation_id` bigint NOT NULL,
    `role_id`       bigint NOT NULL,
    `end_date`      datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `invitation_roles_unique_user_role` (`invitation_id`, `role_id`),
    CONSTRAINT `fk_invitation_roles_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `invitations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_invitation_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE `aups`
(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `user_id`        bigint NOT NULL,
    `institution_id` bigint NOT NULL,
    `agreed_at`      datetime     DEFAULT CURRENT_TIMESTAMP,
    `version`        varchar(255) DEFAULT NULL,
    `url`            varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `aups_unique_institution_role_version` (`user_id`, `institution_id`, `version`),
    CONSTRAINT `fk_aups_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_aups_role` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
