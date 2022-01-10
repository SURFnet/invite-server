CREATE TABLE `scim_failures`
(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `message`        text          DEFAULT NULL,
    `http_method`    varchar(255)  DEFAULT NULL,
    `uri`            varchar(1024) DEFAULT NULL,
    `created_at`     datetime      DEFAULT CURRENT_TIMESTAMP,
    `application_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_scim_failures_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
