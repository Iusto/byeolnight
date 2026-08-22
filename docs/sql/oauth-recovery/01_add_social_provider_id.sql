ALTER TABLE `user`
    ADD COLUMN social_provider_id VARCHAR(255) NULL AFTER social_provider,
    ADD CONSTRAINT uk_user_social_identity UNIQUE (social_provider, social_provider_id);
