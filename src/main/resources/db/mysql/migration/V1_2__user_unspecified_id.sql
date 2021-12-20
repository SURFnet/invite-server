ALTER TABLE `users` ADD unspecified_id VARCHAR(255) NOT NULL;
ALTER TABLE `users` ADD UNIQUE INDEX unspecified_id_index (unspecified_id);