package com.example.eatopedia.data.repository

import android.util.Log // для логування
import androidx.core.text.isDigitsOnly
import com.example.eatopedia.data.remote.ProfileDto //для передачі даних профілю між локальною логікою і Supabase.
import com.example.eatopedia.data.remote.SupabaseClient //клас для доступу до Supabase, через нього робитимемо запити
import io.github.jan.supabase.auth.* //забезпечує авторизацію користувачів
import io.github.jan.supabase.auth.providers.builtin.Email //клас авторизації через email/password
import io.github.jan.supabase.auth.status.SessionSource //todo за оце треба буде почитати
import io.github.jan.supabase.postgrest.from //функція для роботи з таблицями Supabase
import kotlinx.coroutines.Dispatchers //вказує, на якому потоці виконувати корутини
import kotlinx.coroutines.withContext //функція корутин, яка дозволяє змінити контекст виконання (потік) на певний диспатчер
import java.lang.Exception

class AuthRepository {
    private val supabase = SupabaseClient.client
    private val TAG = "AuthRepository"//для логування


    //РЕЄСТРАЦІЯ
    //todo можливо з цим будуть якісь трабли, так шо треба буде затестити
    suspend fun signUp(
        email: String,
        password: String,
        username: String): Result<Unit> = withContext(Dispatchers.IO) //unit +-= void. Не повертає корисного значення, тіко результат операції
    {
        //перевірка
        try {
            if(email.isBlank()|| password.isBlank()||username.isBlank())//isBlank - перевіряємо чи рядок склад тіко із пробілів або ентерів
            {
                return@withContext Result.failure(Exception("Всі поля є обов'язкові"))
            }

            if(password.length<6) {
                return@withContext Result.failure(Exception("Пароль має бути мінімум 6 символів"))
            }

            //реєструємо
            val result = supabase.auth.signUpWith(Email){
                this.email = email.trim()
                this.password = password.trim()
            }

            val userId = result?.id ?: return@withContext Result.failure(Exception("Не вдалося отримати userId"))

            val profile = ProfileDto(id = userId,
                    username = username.trim(),
                    bio = null,
                    avatarUrl = null,
                    createdAt = null)
            supabase.from("profiles").insert(profile)
            Log.d(TAG, "User registered: $email")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Не вдалось зареєструватись", e)
            Result.failure(e)
        }
    }



    //ВХІД
    suspend fun signIn(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank() || password.isBlank()) {
                return@withContext Result.failure(Exception("Email і пароль обов'язкові"))
            }

            val result = supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()

            val userId = user?.id ?: return@withContext Result.failure(Exception("Невірний email або пароль"))

            Log.d(TAG, "User signed in: $email, ID: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(Exception("Невірний email або пароль"))
        }
    }

    //ВИХІД
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signOut()
            Log.d(TAG, "User signed out")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        }
    }

    //Профіль користувача
    suspend fun getCurrentUser(): Result<ProfileDto?> = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
            //якшо залогінений...
            if (userId != null) {
                val profile = supabase.from("profiles")
                    .select {
                        filter { eq("id", userId) }
                    }.decodeSingle<ProfileDto>()//перетворює JSON з бази у DTO

                Log.d(TAG, "Current user: ${profile.username}")
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current user", e)
            Result.failure(e)
        }
    }

    //Отримуємо профіль інших користувачів
    suspend fun getUserProfile(userId: String): Result<ProfileDto?> = withContext(Dispatchers.IO) {
        try {
            val profile = supabase.from("profiles").select {
                filter { eq("id", userId) }//SELECT * FROM profiles WHERE id = 'userId';
                }.decodeSingle<ProfileDto>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //Змінити дані профілю
    suspend fun updateProfile(
        username: String? = null,
        bio: String? = null,
        avatarUrl: String? = null
    ): Result<ProfileDto> = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Користувач не залогінений"))

            // Отримуємо поточний профіль
            val currentProfile = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }.decodeSingle<ProfileDto>()

            // Оновлюємо тільки ті поля, які передали
            val updatedProfile = ProfileDto(
                id = userId,
                username = username ?: currentProfile.username,
                bio = bio ?: currentProfile.bio,
                avatarUrl = avatarUrl ?: currentProfile.avatarUrl,
                createdAt = currentProfile.createdAt
            )

            supabase.from("profiles").update(updatedProfile) {
                    filter { eq("id", userId) }
                }

            Log.d(TAG, "Profile updated")
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile", e)
            Result.failure(e)
        }
    }


    //не робиться жодних мережевих запитів тому і suspend не треба
    fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}