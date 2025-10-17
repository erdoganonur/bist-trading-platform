-- Grant All Authorities to 'onur' User
-- This script grants all available authorities and roles to the user 'onur'
-- User ID: 30d094a1-96f6-4327-ab74-c185da8a007a

-- Update user authorities with all available permissions
UPDATE user_entities
SET authorities = 'ROLE_ADMIN,ROLE_TRADER,ROLE_USER,market:read,trading:read,trading:place,trading:modify,trading:cancel,portfolio:read,orders:read'
WHERE username = 'onur';

-- Verify the update
SELECT id, username, email, authorities, status
FROM user_entities
WHERE username = 'onur';
