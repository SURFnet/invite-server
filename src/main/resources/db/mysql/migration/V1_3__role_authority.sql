ALTER TABLE `roles` ADD authority VARCHAR(255);
UPDATE `roles` SET authority = 'INVITER';
ALTER TABLE `roles` MODIFY authority VARCHAR(255) NOT NULL;
