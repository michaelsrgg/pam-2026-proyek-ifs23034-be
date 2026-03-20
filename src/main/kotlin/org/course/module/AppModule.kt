package org.course.module

import org.course.repositories.*
import org.course.services.*
import org.koin.dsl.module

fun appModule(jwtSecret: String) = module {

    // ── Repositories ─────────────────────────────────────────────────────────
    single<IUserRepository>           { UserRepository() }
    single<IRefreshTokenRepository>   { RefreshTokenRepository() }
    single<ICategoryRepository>       { CategoryRepository() }
    single<ICourseRepository>         { CourseRepository() }
    single<ILessonRepository>         { LessonRepository() }
    single<IEnrollmentRepository>     { EnrollmentRepository() }
    single<ILessonProgressRepository> { LessonProgressRepository() }
    single<IReviewRepository>         { ReviewRepository() }

    // ── Services ──────────────────────────────────────────────────────────────
    single { AuthService(jwtSecret, get(), get()) }
    single { UserService(get()) }
    single { CategoryService(get(), get()) }
    single { CourseService(get(), get(), get(), get()) }
    single { LessonService(get(), get(), get(), get(), get()) }
    single { EnrollmentService(get(), get(), get(), get(), get(), get()) }
    single { ReviewService(get(), get(), get(), get()) }
}
