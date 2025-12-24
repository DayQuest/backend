-- Create databases for each microservice
CREATE DATABASE dayquest_users;
CREATE DATABASE dayquest_auth;
CREATE DATABASE dayquest_videos;
CREATE DATABASE dayquest_quests;
CREATE DATABASE dayquest_social;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE dayquest_users TO postgres;
GRANT ALL PRIVILEGES ON DATABASE dayquest_auth TO postgres;
GRANT ALL PRIVILEGES ON DATABASE dayquest_videos TO postgres;
GRANT ALL PRIVILEGES ON DATABASE dayquest_quests TO postgres;
GRANT ALL PRIVILEGES ON DATABASE dayquest_social TO postgres;
