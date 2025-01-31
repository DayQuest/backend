# DayQuest Backend

© 2025 DayQuest  
All rights reserved.

This software is **private**. You are **not** allowed to copy, modify, distribute, or use this code in any way except for the following cases:  

### ✅ Allowed:
- Private use (for personal, non-public and non-commercial purposes).
- Contributing to this project.  

### ❌ Not Allowed:
- Redistributing, sharing, or publishing this code in any form.
- Modifying and using this code in other projects without permission.
- Using this software for commercial purposes.  

```
DayQuest-backend/
├── docker-compose.dev.yml   # Docker Compose configuration for development with hot reload and volume mounts
├── Dockerfile.dev           # Dockerfile for development environment with dependencies and hot reload
├── docker-compose.yml       # Docker Compose configuration for production without mounts and volumes
└── Dockerfile               # Multi-stage Dockerfile for production deployment, optimized for smaller images
```

### Commands

1. To start the development environment:
   ```bash
   make dev
   ```

2. To start the production environment:
   ```bash
   make prod
   ```

3. To stop and remove containers:
   ```bash
   make down
   ```

4. To build the Docker images:
   ```bash
   make build
   ```

5. To view logs of running containers:
   ```bash
   make logs
   ```
6. Get a list of all available commands:
   ```bash
   make help
   ```
---

## Contribution Rules for Feature Development

### 1. **Production Branch Rules**

The `production` branch is untouchable. You **must never** directly commit or push any changes to this branch. All production changes will be automatically deployed to the server from this branch, so it is critical that this branch remains stable and only reflects production ready code.

### 2. **Development Branch Rules**

The `development` branch is semi touchable. You are allowed to create new feature branches from it, and you can merge them back into the `development` branch through a pull request. Follow these steps for creating a new feature:

1. **Create a New Feature Branch**  
   When starting a new feature, create a new branch from `development`. Naming conventions for branches should be clear and descriptive of the feature. Example:
   ```bash
   git checkout development
   git checkout -b leck-eier-feature
   ```

2. **Work on the Feature**  
   Implement your feature on the new branch. Make sure to frequently commit your changes with meaningful commit messages.

3. **Create a Pull Request**  
   Once your feature is complete, create a pull request to merge your feature branch back into the `development` branch. The PR should be reviewed by another staff for safety.

4. **Merge Back into Development**  
   Once the PR is approved, merge it into the `development` branch. Ensure no direct commits are made to `development` outside of PR merges.

5. **Deploy to Production**  
   Only after the feature is fully tested and verified in the development branch, will it be merged into the `production` branch by an authorized team member, preferably `AgentP`, if possible. The `production` branch will automatically deploy to the server.

### 3. **Do Not Push Directly to Production**

- All changes to the `production` branch must go through the `development` branch via a PR.
