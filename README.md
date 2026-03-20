# 📚 Course App — Backend API

Backend REST API untuk aplikasi mobile **Course App** yang dibangun menggunakan **Kotlin + Ktor 3**.

---

## 🏗️ Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin 2.1 |
| Framework | Ktor 3.1 (Netty engine) |
| ORM | Jetbrains Exposed 0.57 |
| Database | PostgreSQL 16 |
| DI | Koin 3.5 |
| Auth | JWT (HMAC256) + Refresh Token |
| Password | BCrypt (cost 12) |
| Serialization | Kotlinx Serialization |
| Deploy | Docker + Docker Compose |

---

## 📁 Struktur Proyek

```
src/main/kotlin/org/course/
├── Application.kt          # Entry point + plugin setup
├── Routing.kt              # Semua route (35+ endpoint)
├── tables/                 # Exposed table definitions (7 tabel)
│   ├── UserTable.kt
│   ├── RefreshTokenTable.kt
│   ├── CategoryTable.kt
│   ├── CourseTable.kt
│   ├── LessonTable.kt
│   ├── EnrollmentTable.kt
│   ├── LessonProgressTable.kt
│   └── ReviewTable.kt
├── entities/               # Domain entities (data class)
├── data/
│   ├── BaseResponse.kt     # DataResponse, ErrorResponse, PaginatedResponse
│   ├── request/            # Request DTOs
│   └── response/           # Response DTOs
├── repositories/           # Data access layer (interface + impl)
├── services/               # Business logic
│   ├── AuthService.kt
│   ├── UserService.kt
│   ├── CategoryService.kt
│   ├── CourseService.kt
│   ├── LessonService.kt
│   ├── EnrollmentService.kt
│   └── ReviewService.kt
├── helpers/                # Utilities
│   ├── AppHelpers.kt       # Slug, file upload, validator, pagination
│   ├── DatabaseHelper.kt   # DB connection + auto-migrate
│   ├── JWTHelper.kt
│   ├── MappingHelper.kt    # Entity → Response mapping
│   └── PasswordHelper.kt
└── module/
    └── AppModule.kt        # Koin DI bindings
```

---

## 🗄️ Database Schema

```
users ──────────────────────────────────────────┐
  id, name, username, email, password            │
  role (student|instructor), bio, photo          │
  is_active, created_at, updated_at              │
                                                 │
refresh_tokens ──────────────────────────────── ─┤
  id, user_id → users, auth_token, refresh_token │
                                                 │
categories                                       │
  id, name, description, icon                    │
                                                 │
courses ─────────────────────────────────────────┤
  id, instructor_id → users                      │
  category_id → categories                       │
  title, slug, description, thumbnail            │
  level, language, price, discount_price, status │
  total_duration, total_lessons, total_students  │
  avg_rating, total_reviews                      │
                                                 │
lessons ─────────────────────────────────────────┤
  id, course_id → courses                        │
  title, description, content_type               │
  content_url, content, duration                 │
  order_index, is_free, is_published             │
                                                 │
enrollments ─────────────────────────────────────┤
  id, user_id → users, course_id → courses       │
  status, progress (0-100), paid_amount          │
  enrolled_at, completed_at                      │
                                                 │
lesson_progress ─────────────────────────────────┤
  id, user_id, lesson_id, course_id              │
  is_completed, watched_seconds, completed_at    │
                                                 │
reviews                                          │
  id, user_id → users, course_id → courses       │
  rating (1-5), comment                          │
```

---

## 🚀 Setup & Menjalankan

### Prasyarat
- JDK 17+
- PostgreSQL 14+  
- (Opsional) Docker & Docker Compose

### 1. Clone dan konfigurasi environment

```bash
git clone <repo-url>
cd course-app-be

# Salin dan sesuaikan .env
cp .env .env.local
# Edit .env sesuai konfigurasi database Anda
```

### 2. Setup database

```bash
# Buat database di PostgreSQL
psql -U postgres -c "CREATE DATABASE db_course_app;"

# Jalankan schema (opsional, Exposed akan auto-create tables)
psql -U postgres -d db_course_app -f data.sql
```

### 3. Jalankan development

```bash
./gradlew run
# API tersedia di http://localhost:8080
```

### 4. Build production JAR

```bash
./gradlew buildFatJar
java -jar build/libs/course-app-be.jar
```

### 5. Jalankan dengan Docker Compose

```bash
# Production
docker compose up -d

# Development (dengan pgAdmin di port 5050)
docker compose --profile dev up -d

# Lihat log
docker compose logs -f api
```

---

## 📡 API Endpoints (35+ endpoint)

### Base URL: `http://localhost:8080`

### Response Format

**Sukses:**
```json
{
  "status": "success",
  "message": "Pesan sukses",
  "data": { ... }
}
```

**Sukses dengan Pagination:**
```json
{
  "status": "success",
  "message": "...",
  "data": { ... },
  "meta": {
    "currentPage": 1,
    "perPage": 10,
    "total": 100,
    "totalPages": 10,
    "hasNextPage": true,
    "hasPrevPage": false
  }
}
```

**Error:**
```json
{
  "status": "fail",
  "message": "Pesan error"
}
```

---

### 🔑 Auth `/auth`

| Method | Endpoint | Body | Deskripsi |
|---|---|---|---|
| POST | `/auth/register` | `{name, username, email, password, role}` | Registrasi akun baru |
| POST | `/auth/login` | `{usernameOrEmail, password}` | Login |
| POST | `/auth/refresh-token` | `{authToken, refreshToken}` | Perbarui access token |
| POST | `/auth/logout` | `{authToken}` | Logout & invalidate token |

**Login Response:**
```json
{
  "status": "success",
  "message": "Login berhasil.",
  "data": {
    "authToken": "eyJ...",
    "refreshToken": "uuid-...",
    "userId": "uuid",
    "role": "student"
  }
}
```

---

### 👤 User Profile `/users/me` *(🔒 JWT required)*

| Method | Endpoint | Deskripsi |
|---|---|---|
| GET | `/users/me` | Ambil profil user yang sedang login |
| PUT | `/users/me` | Update nama dan bio |
| PUT | `/users/me/password` | Ganti password |
| PUT | `/users/me/photo` | Upload foto profil (multipart) |
| GET | `/images/users/{id}` | Serve file foto (public) |

---

### 📂 Categories `/categories`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| GET | `/categories` | Public | Semua kategori |
| GET | `/categories/{id}` | Public | Detail kategori |
| POST | `/categories` | 🔒 Instructor | Buat kategori baru |
| PUT | `/categories/{id}` | 🔒 Instructor | Update kategori |
| DELETE | `/categories/{id}` | 🔒 Instructor | Hapus kategori |

---

### 📖 Courses `/courses`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| GET | `/courses` | Public | Semua kursus published (filter + pagination) |
| GET | `/courses/{slug}` | Public/🔒 | Detail kursus by slug |
| GET | `/images/courses/{id}` | Public | Serve thumbnail |

**Query params `/courses`:**
- `search` — cari berdasarkan judul
- `categoryId` — filter kategori
- `level` — `beginner | intermediate | advanced`
- `sortBy` — `newest | popular | rating | price_asc | price_desc`
- `minPrice`, `maxPrice` — filter harga
- `page`, `perPage` — pagination

---

### 🎓 Instructor Courses `/instructor/courses` *(🔒 Instructor only)*

| Method | Endpoint | Deskripsi |
|---|---|---|
| GET | `/instructor/courses` | Semua kursus milik saya |
| POST | `/instructor/courses` | Buat kursus baru (draft) |
| GET | `/instructor/courses/{id}` | Detail kursus |
| PUT | `/instructor/courses/{id}` | Update kursus |
| DELETE | `/instructor/courses/{id}` | Hapus kursus |
| PUT | `/instructor/courses/{id}/thumbnail` | Upload thumbnail |
| PATCH | `/instructor/courses/{id}/status/{status}` | Ubah status kursus |
| GET | `/instructor/courses/dashboard/stats` | Dashboard statistik |
| GET | `/instructor/courses/{courseId}/students` | Daftar siswa |

**Status values:** `published | draft | archived`

---

### 📝 Lessons `/courses/{courseId}/lessons`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| GET | `/courses/{courseId}/lessons` | 🔒 | Daftar lesson (konten tersembunyi jika belum enroll) |
| GET | `/courses/{courseId}/lessons/{id}` | 🔒 | Detail lesson |
| POST | `/courses/{courseId}/lessons` | 🔒 Instructor | Buat lesson baru |
| PUT | `/courses/{courseId}/lessons/{id}` | 🔒 Instructor | Update lesson |
| DELETE | `/courses/{courseId}/lessons/{id}` | 🔒 Instructor | Hapus lesson |
| PUT | `/courses/{courseId}/lessons/reorder` | 🔒 Instructor | Ubah urutan lesson |
| POST | `/courses/{courseId}/lessons/{id}/progress` | 🔒 Student | Update progress lesson |

**Content Types:** `video | text | quiz`

---

### 🎟️ Enrollments `/enrollments`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| GET | `/enrollments` | 🔒 | Semua kursus yang diikuti |
| POST | `/enrollments/{courseId}` | 🔒 | Daftar ke kursus |
| GET | `/enrollments/{courseId}` | 🔒 | Detail enrollment + progress |
| DELETE | `/enrollments/{courseId}/cancel` | 🔒 | Batalkan enrollment |

---

### ⭐ Reviews `/courses/{courseId}/reviews`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| GET | `/courses/{courseId}/reviews` | Public | Daftar review |
| POST | `/courses/{courseId}/reviews` | 🔒 Student | Tulis review |
| PUT | `/courses/{courseId}/reviews` | 🔒 Student | Update review |
| DELETE | `/courses/{courseId}/reviews` | 🔒 Student | Hapus review |
| GET | `/courses/{courseId}/reviews/me` | 🔒 | Review saya |

---

## 🔐 Role & Permission

| Fitur | Student | Instructor |
|---|---|---|
| Register / Login | ✅ | ✅ |
| Lihat kursus published | ✅ | ✅ |
| Enroll kursus | ✅ | ❌ |
| Akses lesson | ✅ (enrolled) | ✅ (owner) |
| Update lesson progress | ✅ | ❌ |
| Tulis review | ✅ (enrolled) | ❌ (own course) |
| Buat / kelola kursus | ❌ | ✅ |
| Buat / kelola lesson | ❌ | ✅ (own course) |
| Buat kategori | ❌ | ✅ |
| Lihat dashboard | ❌ | ✅ |
| Lihat daftar siswa | ❌ | ✅ (own course) |

---

## 🌱 Seed Accounts

| Role | Username | Email | Password |
|---|---|---|---|
| Instructor | `budi_instructor` | `budi@example.com` | `Password123` |
| Student | `siti_student` | `siti@example.com` | `Password123` |

---

## 🧪 Testing API

Gunakan file `App.http` dengan IntelliJ IDEA HTTP Client atau VS Code REST Client extension.

```bash
# Atau dengan curl:
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"budi_instructor","password":"Password123"}'
```

---

## 📌 Catatan Deploy

1. Pastikan `JWT_SECRET` diganti dengan nilai yang kuat di production
2. Gunakan environment variables, bukan `.env` file, di production
3. Set `APP_HOST=0.0.0.0` agar container bisa menerima koneksi dari luar
4. Volume `uploads_data` dan `postgres_data` harus di-backup secara rutin
5. Gunakan reverse proxy (Nginx/Caddy) di depan API untuk HTTPS

---

*Dibuat untuk tugas Pemrograman Aplikasi Mobile (PAM) 2026*
