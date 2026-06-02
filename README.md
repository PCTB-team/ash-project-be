# AI Study Hub - Backend

## Screenshots

### Frontend
![AI Study Hub Frontend](docs/images/FE.png)

### Backend API
![AI Study Hub Backend Swagger](docs/images/BE.png)

---

## Overview
AI Study Hub Backend is a RESTful API system for managing learning documents, cloud storage integration, and AI-powered chatbot support.

The system allows users to upload, organize, search, and interact with study materials, while ensuring secure authentication and scalable architecture.

---

## Tech Stack
- Java 26
- Spring Boot 4.0.6
- Spring Security (JWT)
- Spring Data JPA / Hibernate
- MySQL / PostgreSQL
- Redis (OTP, token blacklist, caching)
- Cloud Storage (Cloudinary / AWS S3)
- Maven

---

## Core Features

### Authentication
- User registration with OTP verification
- Login (Email / Username)
- Google OAuth login
- JWT authentication
- Logout with Redis blacklist
- Forgot password (OTP-based reset)

### User Management
- Profile update (name, avatar, password)
- Role-based access control:
  - Guest
  - User
  - Admin

### Document Management
- Upload / download / delete documents
- File preview support
- Folder organization
- Search & filter documents
- Trash bin (soft delete & restore)
- Activity history tracking

### Cloud Storage
- File upload to cloud storage
- Metadata management in database
- Storage usage tracking

### AI Chatbot
- Chat with AI assistant
- Document-based Q&A
- Folder-based summarization
- Semantic search (future enhancement)

---

## System Roles
- **Guest**: View landing page only
- **User**: Full document & AI features based on permissions
- **Admin**: Manage users and system monitoring

---

## Security
- JWT-based authentication
- Password hashing (BCrypt)
- Redis token blacklist (logout handling)
- OTP expiration & rate limiting
- Role-based API authorization

---

## Architecture (High Level)
- Spring Boot REST API
- MySQL/PostgreSQL database
- Redis cache layer
- Cloud storage integration (file storage)
- AI service integration (chatbot)

---

## Key Non-Functional Requirements
- Secure file validation (mime-type + extension check)
- Scalable cloud storage design
- Stateless authentication (JWT)
- Audit logging for user actions
- High availability document access

---


## Future Improvements
- Real-time collaboration on documents
- Version control system
- Advanced AI semantic search (vector database)
- Payment system for storage upgrade
- Mobile application support
