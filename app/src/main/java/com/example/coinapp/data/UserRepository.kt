package com.example.coinapp.data

import android.content.Context
import com.example.coinapp.db.CoinDatabase
import com.example.coinapp.ui.login.WrongCredentialsException
import com.example.coinapp.ui.register.UserExistsException
import com.example.coinapp.utils.UserUtils

class UserRepository(context: Context) {
    companion object {
        private var instance: UserRepository? = null

        fun getInstance(context: Context): UserRepository {
            if (instance == null) {
                instance = UserRepository(context)
            }
            return instance!!
        }
    }

    private val userDao = CoinDatabase.getInstance(context).userDao()

    suspend fun registerUser(user: User) {
        if (userDao.loadByEmail(user.email) != null) {
            throw UserExistsException("This user already exists")
        }
        userDao.insertAll(user)
    }

    suspend fun retrieveUser(email: String, password: String): User {
        val user = userDao.loadByEmail(email)
            ?: throw WrongCredentialsException("This user doesn't exist")
        val hashedPassword = UserUtils.hashPassword(password)

        if (user.password != hashedPassword) {
            throw WrongCredentialsException("Wrong password")
        }

        return user
    }
}